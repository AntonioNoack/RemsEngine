package me.anno.io.files.inner

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.extensions.FileReaderRegistryImpl
import me.anno.extensions.FileReaderRegistry
import me.anno.image.ImageAsFolder
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.Signature
import me.anno.mesh.vox.VOXReader
import kotlin.math.max

object InnerFolderCache : CacheSection("InnerFolderCache"),
    FileReaderRegistry<InnerFolderReader> by FileReaderRegistryImpl() {

    val imageFormats = "png,jpg,bmp,pds,hdr,webp,tga,ico,dds,gif,exr,qoi"
    val imageFormats1 = imageFormats.split(',')

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

    fun readAsFolder(file: FileReference, timeoutMillis: Long, async: Boolean): InnerFile? {
        if (file is InnerFile && file.folder != null) return file.folder
        val data = getFileEntry(file, false, timeoutMillis, async) { file1, _ ->
            generate(file1)
        }
        if (!async && data != null) data.waitFor()
        return data?.value
    }

    private fun generate(file1: FileReference): AsyncCacheData<InnerFolder?> {
        val signature = Signature.findSync(file1)
        val ext = file1.lcExtension
        val data = AsyncCacheData<InnerFolder?>()
        if (signature?.name == "json" && ext == "json") {
            data.value = null
        } else {
            val reader = getReader(signature, ext)
            if (reader != null) {
                reader(file1) { folder, err ->
                    if (file1 is InnerFile) file1.folder = folder
                    data.value = folder
                    err?.printStackTrace()
                    if (folder != null) { // todo remove watch dog when unloading it?
                        FileWatch.addWatchDog(file1)
                    }
                }
            } else data.value = null
        }
        return data
    }

    fun splitParent(name: String): Pair<String, String> {
        var path = name.replace('\\', '/')
        while (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val parent = path.substring(0, max(nameIndex,0))
        return parent to path
    }

    var timeoutMillis = 60_000L

    /**
     * opening a packed stream again would be really expensive for large packages;
     * is there a better strategy than this? -> HeavyIterator reduces the number of required passes
     * */
    var sizeLimit = 500_000L
}