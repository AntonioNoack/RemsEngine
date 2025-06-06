package me.anno.io.files.inner

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.extensions.FileReaderRegistry
import me.anno.extensions.FileReaderRegistryImpl
import me.anno.gpu.GFX
import me.anno.image.ImageAsFolder
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.Signature
import me.anno.io.files.SignatureCache
import me.anno.mesh.vox.VOXReader
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.waitFor
import kotlin.math.max

object InnerFolderCache : CacheSection("InnerFolderCache"),
    FileReaderRegistry<InnerFolderReader> by FileReaderRegistryImpl() {

    val imageFormats = "png,jpg,bmp,pds,hdr,webp,tga,ico,dds,gif,exr,qoi"
    val imageFormats1 = imageFormats.split(',')
    private val generator = { key: FileKey -> generate(key.file) }

    init {
        // meshes
        registerSignatures("vox", VOXReader::readAsFolder)
        // images
        registerSignatures(imageFormats, ImageAsFolder::readAsFolder)
        registerSignatures("media", ImageAsFolder::readAsFolder) // correct for webp, not for videos
    }

    fun wasReadAsFolder(file: FileReference): InnerFolder? {
        val data = getEntryWithoutGenerator(file) as? CacheData<*>
        return data?.value as? InnerFolder
    }

    fun readAsFolder(file: FileReference, async: Boolean): InnerFile? {
        return readAsFolder(file, timeoutMillis, async)
    }

    fun readAsFolder(file: FileReference, async: Boolean, callback: Callback<InnerFolder?>) {
        return getFileEntryAsync(file, false, timeoutMillis, async, generator, callback.waitFor())
    }

    fun readAsFolder(file: FileReference, timeoutMillis: Long, async: Boolean): InnerFile? {
        if (file is InnerFile && file.folder != null) return file.folder
        val data = getFileEntry(file, false, timeoutMillis, async, generator)
        if (!async) data?.waitFor()
        return data?.value
    }

    private fun generate(file1: FileReference): AsyncCacheData<InnerFolder?> {
        val result = AsyncCacheData<InnerFolder?>()
        if (GFX.glThread != null) {
            // todo can we get this working without introducing a dead-lock for tests?
            SignatureCache.getAsync(file1) { signature ->
                generate1(file1, signature, result)
            }
        } else {
            val signature = SignatureCache[file1, false]
            generate1(file1, signature, result)
        }
        return result
    }

    private fun generate1(file1: FileReference, signature: Signature?, result: AsyncCacheData<InnerFolder?>) {
        val ext = file1.lcExtension
        if (signature?.name == "json" && ext == "json") {
            result.value = null
        } else {
            val readers = getReaders(signature, ext)
            generate(file1, result, readers, 0)
        }
    }

    private fun generate(
        file1: FileReference, data: AsyncCacheData<InnerFolder?>,
        generators: List<InnerFolderReader>, gi: Int
    ) {
        if (gi < generators.size) {
            val reader = generators[gi]
            reader(file1) { folder, err ->
                err?.printStackTrace()
                if (folder != null) {
                    if (file1 is InnerFile) {
                        file1.folder = folder
                    }
                    data.value = folder
                    // todo remove watch dog when unloading it?
                    FileWatch.addWatchDog(file1)
                } else generate(file1, data, generators, gi + 1)
            }
        } else data.value = null
    }

    fun splitParent(name: String): Pair<String, String> {
        var path = name.replace('\\', '/')
        while (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val parent = path.substring(0, max(nameIndex, 0))
        return parent to path
    }

    var timeoutMillis = 60_000L

    /**
     * opening a packed stream again would be really expensive for large packages;
     * is there a better strategy than this? -> HeavyIterator reduces the number of required passes
     * */
    var sizeLimit = 500_000L
}