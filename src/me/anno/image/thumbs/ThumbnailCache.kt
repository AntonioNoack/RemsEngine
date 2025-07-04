package me.anno.image.thumbs

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.IgnoredException
import me.anno.ecs.prefab.PrefabReadable
import me.anno.extensions.FileReaderRegistry
import me.anno.extensions.FileReaderRegistryImpl
import me.anno.gpu.Blitting
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureReader
import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HDBKey
import me.anno.graph.hdb.HDBKey.Companion.InvalidKey
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.image.Image
import me.anno.image.ImageAsFolder
import me.anno.image.ImageReadable
import me.anno.image.ImageScale.scaleMax
import me.anno.io.Streams.readNBytes2
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.SignatureCache
import me.anno.io.files.inner.InnerByteSliceFile
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.utils.OS
import me.anno.utils.async.Callback
import me.anno.utils.async.firstPromise
import me.anno.utils.hpc.ProcessingQueue
import net.boeckling.crc.CRC64
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * creates and caches small versions of image, video and mesh resources
 *
 * todo we have a race-condition issue: sometimes, matrices are transformed incorrectly
 * todo also sometimes, images get rendered on top of each other, which is weird
 * */
object ThumbnailCache : FileReaderRegistry<ThumbGenerator> by FileReaderRegistryImpl() {

    private val LOGGER = LogManager.getLogger(ThumbnailCache::class)

    private val folder = ConfigBasics.cacheFolder.getChild("thumbs")
    val worker = ProcessingQueue("Thumbnails")

    private val hdb = HierarchicalDatabase(
        "Thumbs", folder, 5_000_000, 10_000L,
        2 * 7 * 24 * 64 * 64 * 1000L
    )

    private val sizes = intArrayOf(32, 64, 128, 256, 512)
    private val textures = CacheSection<ThumbnailKey, ITexture2D>("Thumbs")

    private const val timeout = 5000L

    var useCacheFolder = true

    @JvmStatic
    fun invalidate(file: FileReference, neededSize: Int) {
        if (file == InvalidRef) return
        val size = getSize(neededSize)
        textures.remove { key, _ -> key.file == file && key.size == size }
        // invalidate database, too
        file.getFileHash { hash ->
            hdb.remove(getCacheKey(file, hash, size))
        }
    }

    @JvmStatic
    fun invalidate(file: FileReference) {
        if (file == InvalidRef) return
        textures.remove { key, _ -> key.file == file }
        // invalidate database, too
        file.getFileHash { hash ->
            for (size in sizes) {
                hdb.remove(getCacheKey(file, hash, size))
            }
        }
    }

    @JvmStatic
    fun getEntry(file: FileReference, neededSize: Int): AsyncCacheData<ITexture2D> {
        return when {
            neededSize < 1 -> AsyncCacheData.empty()
            file == InvalidRef -> AsyncCacheData.empty()
            file is ImageReadable -> TextureCache[file, timeout]
            file.isDirectory || !file.exists -> AsyncCacheData.empty()
            else -> {
                val size = getSize(neededSize)
                val lastModified = file.lastModified
                val key = ThumbnailKey(file, lastModified, size)
                textures.getEntryLimitedWithRetry(key, timeout, 4, generator)
            }
        }
    }

    @JvmStatic
    operator fun get(file: FileReference, neededSize: Int): ITexture2D? {
        val value = getEntry(file, neededSize)
        return value.value?.createdOrNull() ?: run {
            val size = getSize(neededSize)
            val lastModified = file.lastModified
            findSmallerThumbnail(size, file, lastModified)
        }
    }

    private fun findSmallerThumbnail(size: Int, file: FileReference, lastModified: Long): ITexture2D? {
        // return lower resolutions, if they are available
        for (i in sizes.indexOf(size) - 1 downTo 0) {
            val size1 = sizes[i]
            val key1 = ThumbnailKey(file, lastModified, size1)
            val gen = textures.getEntryWithoutGenerator(key1, 50)
            val tex = gen?.value
            if (tex != null) return tex
        }
        return null
    }

    @JvmStatic
    private val generator = { key: ThumbnailKey, callback: AsyncCacheData<ITexture2D> ->
        val srcFile = key.file
        val size = key.size
        // if larger texture exists in cache, use it and scale it down
        val idx = sizes.indexOf(size) + 1
        var done = false
        for (i in idx until sizes.size) {
            val sizeI = sizes[i]
            val keyI = ThumbnailKey(key.file, key.lastModified, sizeI)
            val gen = textures.getEntryWithoutGenerator(keyI, 500)
            val tex = gen?.value
            if (tex != null && tex.isCreated()) {
                LOGGER.info("Copying texture for $key")
                copyTexIfPossible(srcFile, size, tex, callback)
                done = true
                break
            }
        }
        if (!done) {
            generate(srcFile, size, callback)
        }
    }

    private fun copyTexIfPossible(
        srcFile: FileReference, size: Int, tex: ITexture2D,
        callback: Callback<ITexture2D>
    ) {
        val (w, h) = scaleMax(tex.width, tex.height, size)
        if (w < 2 || h < 2) return // cannot generate texture anyway, no point in loading it
        if (isGFXThread()) {
            if (tex is Texture2D && tex.isDestroyed) {
                // fail, we were too slow waiting for a GFX queue call
                generate(srcFile, size, callback)
            } else {
                val newTex = Texture2D(srcFile.name, w, h, 1)
                newTex.create(TargetType.UInt8x4)
                useFrame(newTex) {
                    Blitting.copyColor(tex, true)
                }
                callback.ok(newTex)
            }
        } else addGPUTask("Copy", size, size) {
            copyTexIfPossible(srcFile, size, tex, callback)
        }
    }

    @JvmStatic
    private fun FileReference.getFileHash(callback: (Long) -> Unit) {
        val hashReadLimit = 4096
        val length = this.length()
        val baseHash = lastModified xor (454781903L * length)
        if (!isDirectory && length > 0) {
            inputStream(hashReadLimit.toLong(), true) { reader, _ ->
                if (reader != null) {
                    val sampleBytes = reader.readNBytes2(hashReadLimit, false)
                    callback(CRC64.update(sampleBytes, 0, sampleBytes.size, baseHash))
                } else callback(baseHash)
            }
        } else callback(baseHash)
    }

    @JvmStatic
    private fun getCacheKey(srcFile: FileReference, hash: Long, size: Int): HDBKey {
        if (srcFile is InnerTmpFile) return InvalidKey
        val split = srcFile.absolutePath.split('/')
        return HDBKey(split.subList(0, max(split.lastIndex, 0)), hash * 31 + size)
    }

    @JvmStatic
    private fun getSize(neededSize: Int): Int {
        for (i in 0 until sizes.lastIndex) {
            val size = sizes[i]
            if (size >= neededSize) {
                return size
            }
        }
        return sizes.last()
    }

    @JvmStatic
    private fun upload(
        srcFile: FileReference,
        checkRotation: Boolean, image: Image,
        callback: Callback<ITexture2D>
    ) {
        TextureReader.getRotation(if (checkRotation) srcFile else InvalidRef) { rot, _ ->
            val texture = Texture2D(srcFile.name, image.width, image.height, 1)
            image.createTexture(texture, checkRedundancy = true) { tex, exc ->
                if (tex is Texture2D) tex.rotation = rot
                callback.call(tex, exc)
            }
        }
    }

    @JvmStatic
    fun saveNUpload(
        srcFile: FileReference, checkRotation: Boolean, dstKey: HDBKey, dst: Image,
        callback: Callback<ITexture2D>, alreadyInWorker: Boolean = false
    ) {
        if (!alreadyInWorker && isGFXThread()) {
            return worker.plusAssign {
                saveNUpload(
                    srcFile, checkRotation, dstKey, dst,
                    callback, true
                )
            }
        }
        if (dstKey != InvalidKey) {
            // could we write webp? is most efficient
            //  -> only via ffmpeg, and that procedure is quite inefficient with creating a new process
            val bos = ByteArrayOutputStream()
            dst.write(bos, if (dst.hasAlphaChannel) "png" else "jpg")
            hdb.put(dstKey, bos.toByteArray())
        }
        upload(srcFile, checkRotation, dst, callback)
    }

    @JvmStatic
    fun transformNSaveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        src: Image,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>,
        alreadyInWorker: Boolean = false
    ) {
        val sw = src.width
        val sh = src.height
        if (min(sw, sh) < 1) return

        if (isGFXThread()) {
            return worker.plusAssign {
                transformNSaveNUpload(
                    srcFile, checkRotation, src, dstFile,
                    size, callback, true
                )
            }
        }

        // if it matches the size, just upload it
        // we have loaded it anyway already
        if (max(sw, sh) < size) {
            return saveNUpload(
                srcFile, checkRotation, dstFile, src,
                callback, alreadyInWorker
            )
        }

        val (w, h) = scaleMax(sw, sh, size)
        if (min(w, h) < 1) return
        val image = if (!(w == sw && h == sh)) {
            src.resized(w, h, false)
        } else src

        saveNUpload(
            srcFile, checkRotation, dstFile, image,
            callback, alreadyInWorker
        )
    }

    private fun readImage(bytes: ByteSlice): AsyncCacheData<Image> {
        val file = InnerByteSliceFile("", "", InvalidRef, bytes)
        return ImageAsFolder.readImage(file, true)
    }

    private fun shallReturnIfExists(
        srcFile: FileReference, dstFile: ByteSlice?,
        callback: Callback<ITexture2D>,
        callback1: (Boolean) -> Unit
    ) {
        if (dstFile == null) {
            callback1(false)
        } else {
            val promise = readImage(dstFile)
            promise.waitFor { image ->
                if (image != null) {
                    TextureReader.getRotation(srcFile) { rot, _ ->
                        addGPUTask("Thumbs.returnIfExists", image.width, image.height) {
                            val texture = Texture2D(srcFile.name, image, true)
                            texture.rotation = rot
                            callback.ok(texture)
                        }
                        callback1(true)
                    }
                } else callback1(false)
            }
        }
    }

    private fun shallReturnIfExists(
        srcFile: FileReference, dstFile: HDBKey,
        callback: Callback<ITexture2D>,
        callback1: (Boolean) -> Unit
    ) {
        hdb.get(dstFile) { it, err ->
            if (it != null) shallReturnIfExists(srcFile, it, callback, callback1)
            else callback.err(err)
        }
    }

    @JvmStatic
    fun findScale(
        src: Image,
        srcFile: FileReference,
        size0: Int,
        callback: Callback<ITexture2D>,
        callback1: (Image) -> Unit
    ) {
        var size = size0
        val sw = src.width
        val sh = src.height
        if (max(sw, sh) < size) {
            size /= 2
            if (size < 3) return
            srcFile.getFileHash { hash ->
                findScale(src, srcFile, size0, hash, callback, callback1)
            }
        } else {
            val (w, h) = scaleMax(sw, sh, size)
            if (w < 2 || h < 2) return
            callback1(src.resized(w, h, false))
        }
    }

    @JvmStatic
    private fun findScale(
        src: Image, srcFile: FileReference,
        size0: Int, hash: Long,
        callback: Callback<ITexture2D>,
        callback1: (Image) -> Unit
    ) {
        var size = size0
        val sw = src.width
        val sh = src.height
        if (max(sw, sh) < size) {
            size /= 2
            if (size < 3) return
            val key = getCacheKey(srcFile, hash, size)
            shallReturnIfExists(srcFile, key, callback) { shallReturn ->
                if (!shallReturn) {
                    findScale(src, srcFile, size, hash, callback, callback1)
                }
            }
        } else {
            val (w, h) = scaleMax(sw, sh, size)
            if (w < 2 || h < 2) return
            callback1(src.resized(w, h, false))
        }
    }

    @JvmStatic
    fun generate(srcFile: FileReference, size: Int, callback: Callback<ITexture2D>) {
        if (size < 3) return
        if (useCacheFolder) {
            srcFile.getFileHash { hash ->
                val key = getCacheKey(srcFile, hash, size)
                hdb.get(key) { byteSlice, _ ->
                    // check all higher LODs for data: if they exist, use them instead
                    checkHigherResolutions(srcFile, size, hash, callback) {
                        shallReturnIfExists(srcFile, byteSlice, callback) { foundExists ->
                            if (!foundExists) {
                                generate(srcFile, key, size, callback)
                            }
                        }
                    }
                }
            }
        } else {
            generate(srcFile, InvalidKey, size, callback)
        }
    }

    private fun checkHigherResolutions(
        srcFile: FileReference, size: Int, hash: Long,
        callback: Callback<ITexture2D>,
        ifFoundNothing: () -> Unit
    ) {
        val firstSizeIndexToCheck = sizes.indexOf(size) + 1
        checkResolutionI(srcFile, size, hash, callback, ifFoundNothing, firstSizeIndexToCheck)
    }

    private fun checkResolutionI(
        srcFile: FileReference, size: Int, hash: Long,
        callback: Callback<ITexture2D>,
        ifFoundNothing: () -> Unit, sizeIndex: Int,
    ) {
        if (sizeIndex < sizes.size) {
            val sizeI = sizes[sizeIndex]
            val keyI = getCacheKey(srcFile, hash, sizeI)
            hdb.get(keyI) { bytes, _ ->
                if (bytes != null) {
                    readImage(bytes).waitFor { image ->
                        if (image != null) {
                            scaleDownSolution(srcFile, size, image, callback)
                        } else checkResolutionI(srcFile, size, hash, callback, ifFoundNothing, sizeIndex + 1)
                    }
                } else checkResolutionI(srcFile, size, hash, callback, ifFoundNothing, sizeIndex + 1)
            }
        } else ifFoundNothing()
    }

    private fun scaleDownSolution(
        srcFile: FileReference, size: Int, image: Image,
        callback: Callback<ITexture2D>,
    ) {
        // scale down (and save?)
        TextureReader.getRotation(srcFile) { rot, _ ->
            val (w, h) = scaleMax(image.width, image.height, size)
            val newImage = image.resized(w, h, false)
            val texture = Texture2D("${srcFile.name}-$size", newImage.width, newImage.height, 1)
            newImage.createTexture(texture, checkRedundancy = false) { tex, exc ->
                if (tex is Texture2D) tex.rotation = rot
                callback.call(tex, exc)
            }
        }
    }

    init {

        TextThumbnails.register()
        LinkThumbnails.register()
        AssetThumbnails.register()
        ImageThumbnails.register()

        val ignored = "zip,bz2,tar,gzip,xz,lz4,7z,xar,sims,lua-bytecode"
        registerSignatures(ignored) { _, _, _, callback ->
            callback.err(IgnoredException())
        }
    }

    private fun shouldIgnoreFile(srcFile: FileReference): Boolean {
        if (OS.isWindows) { // these files should be ignored
            @Suppress("SpellCheckingInspection")
            when (srcFile.name) {
                "pagefile.sys", "hiberfil.sys",
                "DumpStack.log", "DumpStack.log.tmp",
                "swapfile.sys" -> return true
            }
        }
        return false
    }

    @JvmStatic
    fun generate(srcFile: FileReference, dstFile: HDBKey, size: Int, callback: Callback<ITexture2D>) {

        if (size < 3) return

        // for some stuff, the icons are really nice
        // for others, we need our previews
        // also some folder icons are really nice, while others are boring / default :/
        // generateSystemIcon(srcFile, dstFile, size, callback)
        // return

        if (srcFile.isDirectory) {
            // todo thumbnails for folders: what files are inside, including their preview images
            // generateSystemIcon(srcFile, dstFile, size, callback)
            return callback.err(IOException("Cannot generate thumbnail for folder"))
        }

        // generate the image,
        // upload the result to the gpu
        // save the file

        if (shouldIgnoreFile(srcFile)) {
            return callback.err(IgnoredException("$srcFile should be ignored"))
        }

        when (srcFile) {
            is ImageReadable -> {
                val image = if (useCacheFolder) srcFile.readCPUImage() else srcFile.readGPUImage()
                transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
            }
            is PrefabReadable -> AssetThumbnails.generateAssetFrame(srcFile, dstFile, size, callback)
            else -> SignatureCache[srcFile].waitFor { signature ->
                generate(srcFile, dstFile, size, signature, callback)
            }
        }
    }

    @JvmStatic
    private fun generate(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        signature: Signature?, callback: Callback<ITexture2D>
    ) {
        val readers = getReaders(signature, srcFile.lcExtension)
        firstPromise(readers) { reader, cb -> reader.generate(srcFile, dstFile, size, cb) }
            .then(callback)
            .catch { ImageThumbnails.generateImage(srcFile, dstFile, size, callback) }
    }
}