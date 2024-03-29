package me.anno.io.files.inner

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.ImageReader
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.Signature
import me.anno.mesh.vox.VOXReader

object InnerFolderCache : CacheSection("InnerFolderCache") {

    // cache all content? if less than a certain file size
    // cache the whole hierarchy [? only less than a certain depth level - not done]

    // todo read compressed exe files?

    // done display unity packages differently: display them as their usual file structure
    // it kind of is a new format, that is based on another decompression

    private val readerBySignature = HashMap<String, InnerFolderReader>(64)
    private val readerByFileExtension = HashMap<String, InnerFolderReader>(64)

    @Suppress("unused")
    fun registerFileExtension(fileExtension: String, reader: InnerFolderReader) {
        readerBySignature[fileExtension] = reader
    }

    fun registerFileExtension(fileExtensions: List<String>, reader: InnerFolderReader) {
        for (signature in fileExtensions) {
            readerBySignature[signature] = reader
        }
    }

    fun register(signature: String, reader: InnerFolderReader) {
        readerBySignature[signature] = reader
    }

    fun register(signatures: List<String>, reader: InnerFolderReader) {
        for (signature in signatures) {
            register(signature, reader)
        }
    }

    fun unregisterSignatures(vararg signatures: String) {
        for (signature in signatures) {
            readerBySignature.remove(signature)
        }
    }

    fun unregisterFileExtension(fileExtension: String) {
        readerByFileExtension.remove(fileExtension)
    }

    fun hasReaderForSignature(signature: String?): Boolean {
        return signature != null && signature in readerBySignature
    }

    fun hasReaderForFileExtension(lcExtension: String?): Boolean {
        return lcExtension != null && (lcExtension in readerByFileExtension || lcExtension in readerBySignature)
    }

    val imageFormats = listOf("png", "jpg", "bmp", "pds", "hdr", "webp", "tga", "ico", "dds", "gif", "exr", "qoi")

    init {
        // meshes
        // to do all mesh extensions
        register("vox", VOXReader.Companion::readAsFolder)

        // cannot be read by assimp anyway
        // registerFileExtension("max", AnimatedMeshesLoader::readAsFolder) // 3ds max file, idk about its file signature
        // images
        // to do all image formats
        register(imageFormats, ImageReader::readAsFolder)
        register("media", ImageReader::readAsFolder) // correct for webp, not for videos
    }

    fun wasReadAsFolder(file: FileReference): InnerFolder? {
        val data = getEntryWithoutGenerator(file) as? CacheData<*>
        return data?.value as? InnerFolder
    }

    fun readAsFolder(file: FileReference, timeoutMillis: Long, async: Boolean): InnerFile? {
        if (file is InnerFile && file.folder != null) return file.folder
        val data = getFileEntry(file, false, timeoutMillis, async) { file1, _ ->
            val signature = Signature.findNameSync(file1)
            val ext = file1.lcExtension
            if (signature == "json" && ext == "json") null
            else {
                val data = AsyncCacheData<InnerFolder?>()
                val reader = readerBySignature[signature] ?: readerBySignature[ext] ?: readerByFileExtension[ext]
                val callback = { folder: InnerFolder?, ec: Exception? ->
                    if (file1 is InnerFile) file1.folder = folder
                    data.value = folder
                    ec?.printStackTrace()
                    if (folder != null) { // todo remove watch dog when unloading it?
                        FileWatch.addWatchDog(file1)
                    }
                }
                if (reader != null) {
                    reader(file1, callback)
                } else data.value = null
                data
            }
        } as? AsyncCacheData<*>
        return data?.value as? InnerFile
    }

    fun readAsFolder(file: FileReference, async: Boolean): InnerFile? {
        return readAsFolder(file, timeoutMillis, async)
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
    var sizeLimit = 20_000_000L
}