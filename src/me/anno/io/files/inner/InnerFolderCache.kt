package me.anno.io.files.inner

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.extensions.FileRegistry
import me.anno.extensions.IFileRegistry
import me.anno.image.ImageReader
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.Signature
import me.anno.mesh.vox.VOXReader

object InnerFolderCache : CacheSection("InnerFolderCache"),
    IFileRegistry<InnerFolderReader> by FileRegistry() {

    // cache all content? if less than a certain file size
    // cache the whole hierarchy [? only less than a certain depth level - not done]

    // done display unity packages differently: display them as their usual file structure
    // it kind of is a new format, that is based on another decompression

    val imageFormats = "png,jpg,bmp,pds,hdr,webp,tga,ico,dds,gif,exr,qoi"
    val imageFormats1 = imageFormats.split(',')

    init {
        // meshes
        // to do all mesh extensions
        registerSignatures("vox", VOXReader.Companion::readAsFolder)

        // cannot be read by assimp anyway
        // registerFileExtension("max", AnimatedMeshesLoader::readAsFolder) // 3ds max file, idk about its file signature
        // images
        // to do all image formats
        registerSignatures(imageFormats, ImageReader::readAsFolder)
        registerSignatures("media", ImageReader::readAsFolder) // correct for webp, not for videos
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
        } as? AsyncCacheData<*>
        if (!async && data != null) data.waitFor()
        return data?.value as? InnerFile
    }

    private fun generate(file1: FileReference): AsyncCacheData<InnerFolder?> {
        val signature = Signature.findNameSync(file1)
        val ext = file1.lcExtension
        val data = AsyncCacheData<InnerFolder?>()
        if (signature == "json" && ext == "json") {
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
        val parent = if (nameIndex < 0) "" else path.substring(0, nameIndex)
        return parent to path
    }

    var timeoutMillis = 60_000L

    /**
     * opening a packed stream again would be really expensive for large packages;
     * is there a better strategy than this?? maybe index a few on every go to load something
     * */
    var sizeLimit = 500_000L
}