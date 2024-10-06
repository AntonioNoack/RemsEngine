package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.missingColors
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.raw.AlphaMaskImage
import me.anno.image.raw.BGRAImage
import me.anno.image.raw.ComponentImage
import me.anno.image.raw.GPUImage
import me.anno.image.raw.GrayscaleImage
import me.anno.image.raw.IntImage
import me.anno.image.raw.OpaqueImage
import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.SignatureFile
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.OS
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.io.InputStream

/**
 * Defines reading an image as a folder:
 * different combinations of sub-images created from its color channels.
 *
 * ICO- and GIMP-files contain multiple layers, and they are exposed here, too.
 * */
object ImageAsFolder {

    private val LOGGER = LogManager.getLogger(ImageAsFolder::class)
    private val missingImage = IntImage(2, 2, missingColors, false)

    var tryFFMPEG: ((file: FileReference, signature: String?, forGPU: Boolean, callback: Callback<Image>) -> Unit)? =
        null

    /** returns List<Image> or exception */
    var readIcoLayers: ((InputStream) -> Any)? = null
    var readJPGThumbnail: ((FileReference, Callback<Image?>) -> Unit)? = null

    @JvmStatic
    fun readAsFolder(file: FileReference, callback: Callback<InnerFolder>) {

        val folder = InnerFolder(file)

        // add the most common swizzles: r,g,b,a
        createSwizzle(file, folder, "r.png", 'r', false)
        createSwizzle(file, folder, "g.png", 'g', false)
        createSwizzle(file, folder, "b.png", 'b', false)
        createSwizzle(file, folder, "a.png", 'a', false)

        // bgra
        createSwizzle(file, folder, "bgra.png") {
            if (it is BGRAImage) it.base // bgra.bgra = rgba
            else BGRAImage(it)
        }

        // inverted components
        createSwizzle(file, folder, "1-r.png", 'r', true)
        createSwizzle(file, folder, "1-g.png", 'g', true)
        createSwizzle(file, folder, "1-b.png", 'b', true)
        createSwizzle(file, folder, "1-a.png", 'a', true)

        // white with transparency, black with transparency (overriding color)
        createAlphaMask(file, folder, "111a.png", false)
        createAlphaMask(file, folder, "000a.png", true)

        // grayscale, if not only a single channel
        createSwizzle(file, folder, "grayscale.png") {
            if (it.numChannels > 1) GrayscaleImage(it)
            else it
        }

        // rgb without alpha, if alpha exists
        createSwizzle(file, folder, "rgb.png") {
            if (it.hasAlphaChannel) OpaqueImage(it)
            else it
        }

        val ric = readIcoLayers
        if (file.lcExtension == "ico" && ric != null) {
            Signature.findName(file) { sig, _ ->
                if (sig == null || sig == "ico") {
                    file.inputStream { it, exc ->
                        if (it != null) {
                            val layers = ric(it)
                            if (layers is List<*>) {
                                for (index in layers.indices) {
                                    val layer = layers[index] as? Image ?: break
                                    folder.createImageChild("layer$index", layer)
                                }
                            } else if (layers is Exception) {
                                layers.printStackTrace()
                            }
                            it.close()
                            callback.ok(folder)
                        } else {
                            exc?.printStackTrace()
                            callback.ok(folder)
                        }
                    }
                } else callback.ok(folder)
            }
            return // we're done, don't call callback twice
        }

        val rjt = readJPGThumbnail
        if ((file.lcExtension == "jpg" || file.lcExtension == "jpeg") && rjt != null) {
            rjt.invoke(file) { thumb, _ ->
                if (thumb != null) folder.createImageChild("thumbnail.jpg", thumb)
                callback.ok(folder)
            }
            return
        }

        callback.ok(folder)
    }

    @JvmStatic
    private fun createSwizzle(file: FileReference, folder: InnerFolder, name: String, createImage: (Image) -> Image) {
        folder.createLazyImageChild(name, lazy {
            // CPU: calculated lazily
            val srcImage = warnIfMissing(ImageCache[file, false], missingImage, file)
            createImage(srcImage)
        }, {
            // GPU: calculated whenever needed
            val srcTexture = warnIfMissing(TextureCache[file, false], missingTexture, file)
            createImage(GPUImage(srcTexture))
        })
    }

    @JvmStatic
    private fun <V> warnIfMissing(nullable: V?, ifNull: V, file: FileReference): V {
        return if (nullable != null) nullable else {
            LOGGER.warn("Missing texture for {}", file)
            ifNull
        }
    }

    @JvmStatic
    private fun createSwizzle(
        file: FileReference, folder: InnerFolder, name: String,
        swizzle: Char, inverse: Boolean
    ) {
        createSwizzle(file, folder, name) { srcImage ->
            createSwizzleImage(srcImage, swizzle, inverse)
        }
    }

    private fun createSwizzleImage(srcImage: Image, swizzle: Char, inverse: Boolean): Image {
        val isBlack = getIsBlack(swizzle, inverse, srcImage)
        return if (isBlack != null) {
            GPUImage(if (isBlack) blackTexture else whiteTexture, 1, false)
        } else ComponentImage(srcImage, inverse, swizzle)
    }

    @JvmStatic
    private fun getIsBlack(swizzle: Char, inverse: Boolean, srcImage: Image): Boolean? {
        return when {
            (swizzle == 'a' && !srcImage.hasAlphaChannel) -> inverse
            (swizzle == 'b' && srcImage.numChannels < 3) || (swizzle == 'g' && srcImage.numChannels < 2) -> !inverse
            else -> null
        }
    }

    @JvmStatic
    private fun createAlphaMask(file: FileReference, folder: InnerFolder, name: String, isBlack: Boolean) {
        createSwizzle(file, folder, name) { srcImage ->
            if (srcImage.hasAlphaChannel) {
                val color1 = if (isBlack) black else white
                AlphaMaskImage(srcImage, false, 'a', color1)
            } else {
                GPUImage(if (isBlack) blackTexture else whiteTexture, 1, false)
            }
        }
    }

    private fun shouldUseFFMPEG(signature: String?, file: FileReference): Boolean {
        if (OS.isWeb) return false // uncomment, when we support FFMPEG in the browser XD
        return signature == "dds" || signature == "media" || file.lcExtension == "webp"
    }

    private val shouldIgnoreExt = ("rar,bz2,zip,tar,gzip,xz,lz4,7z,xar,oar,java,text,wasm,ttf,woff1,woff2,shell," +
            "xml,svg,exe,vox,fbx,gltf,obj,blend,mesh-draco,ply,md2,md5mesh,dae,yaml").split(',').toHashSet()

    private fun shouldIgnore(signature: String?): Boolean {
        return signature in shouldIgnoreExt
    }

    fun readImage(file: FileReference, forGPU: Boolean): AsyncCacheData<Image> {
        val data = AsyncCacheData<Image>()
        if (file is ImageReadable) {
            data.value = if (forGPU) file.readGPUImage() else file.readCPUImage()
        } else if (file is BundledRef || (file !is SignatureFile && file.length() < 10_000_000L)) { // < 10MB -> read directly
            file.readBytes { bytes, exc ->
                exc?.printStackTrace()
                if (bytes != null) {
                    readImage(file, data, bytes, forGPU)
                } else {
                    data.value = null
                    data.hasValue = true
                }
            }
        } else Signature.findName(file) { signature, _ ->
            readImage(file, data, signature, forGPU)
        }
        return data
    }

    private fun readImage(file: FileReference, data: AsyncCacheData<Image>, bytes: ByteArray, forGPU: Boolean) {
        val signature = Signature.findName(bytes)
        val tryFFMPEG = tryFFMPEG
        if (shouldIgnore(signature)) {
            data.value = null
        } else if (tryFFMPEG != null && shouldUseFFMPEG(signature, file)) {
            tryFFMPEG(file, signature, forGPU, data)
        } else {
            val reader = ImageCache.byteReaders[signature] ?: ImageCache.byteReaders[file.lcExtension]
            if (reader != null) reader.read(bytes, data)
            else data.value = null
        }
    }

    private fun readImage(file: FileReference, data: AsyncCacheData<Image>, signature: String?, forGPU: Boolean) {
        val tryFFMPEG = tryFFMPEG
        if (shouldIgnore(signature)) {
            data.value = null
        } else if (tryFFMPEG != null && shouldUseFFMPEG(signature, file)) {
            tryFFMPEG(file, signature, forGPU, data)
        } else {
            val reader = ImageCache.fileReaders[signature] ?: ImageCache.fileReaders[file.lcExtension]
            if (reader != null) reader.read(file, data)
            else data.value = null
        }
    }
}