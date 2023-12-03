package me.anno.io.files.inner

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.ImageReader
import me.anno.image.gimp.GimpImage
import me.anno.image.svg.SVGMesh
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.unity.UnityReader
import me.anno.io.zip.*
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.mitsuba.MitsubaReader
import me.anno.mesh.obj.MTLReader
import me.anno.mesh.obj.OBJReader
import me.anno.mesh.vox.VOXReader
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.zip.ZipException

object InnerFolderCache : CacheSection("InnerFolderCache") {

    private val LOGGER = LogManager.getLogger(InnerFolderCache::class)

    // cache all content? if less than a certain file size
    // cache the whole hierarchy [? only less than a certain depth level - not done]

    // todo read compressed exe files?

    // done display unity packages differently: display them as their usual file structure
    // it kind of is a new format, that is based on another decompression

    private val readerBySignature = HashMap<String, InnerFolderReader>(64)
    private val readerByFileExtension = HashMap<String, InnerFolderReader>(64)

    @Suppress("unused")
    fun registerFileExtension(signature: String, reader: InnerFolderReader) {
        readerBySignature[signature] = reader
    }

    fun registerFileExtension(signatures: List<String>, reader: InnerFolderReader) {
        for (signature in signatures) {
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

    fun unregister(signature: String) {
        readerBySignature.remove(signature)
    }

    fun hasReaderForSignature(signature: String?): Boolean {
        return signature != null && signature in readerBySignature
    }

    fun hasReaderForFileExtension(lcExtension: String?): Boolean {
        return lcExtension != null && (lcExtension in readerByFileExtension || lcExtension in readerBySignature)
    }

    val imageFormats = listOf("png", "jpg", "bmp", "pds", "hdr", "webp", "tga", "ico", "dds", "gif", "exr", "qoi")

    init {

        // compressed folders
        register(listOf("zip", "bz2", "lz4", "xar", "oar"), InnerZipFile.Companion::createZipRegistryV2)
        register("7z") { it, c -> c(Inner7zFile.createZipRegistry7z(it) { Inner7zFile.fileFromStream7z(it) }, null) }
        register("rar") { it, c ->
            c(
                InnerRarFile.createZipRegistryRar(it) { InnerRarFile.fileFromStreamRar(it) },
                null
            )
        }
        register("gzip", InnerTarFile.Companion::readAsGZip)
        register("tar", InnerTarFile.Companion::readAsGZip)

        // meshes
        // to do all mesh extensions
        register(listOf("fbx", "gltf", "dae", "draco", "md2", "md5mesh")) { it, c ->
            c(AnimatedMeshesLoader.readAsFolder(it), null)
        }
        register("blend", BlenderReader::readAsFolder)
        register("obj", OBJReader.Companion::readAsFolder)
        register("mtl", MTLReader.Companion::readAsFolder)
        register("vox", VOXReader.Companion::readAsFolder)

        register("mitsuba-meshes", MitsubaReader::readMeshesAsFolder)
        register("mitsuba-scene", MitsubaReader::readSceneAsFolder)

        // cannot be read by assimp anyway
        // registerFileExtension("max", AnimatedMeshesLoader::readAsFolder) // 3ds max file, idk about its file signature
        // images
        // to do all image formats
        register(imageFormats, ImageReader::readAsFolder)
        register("gimp", GimpImage.Companion::readAsFolder)
        register("media", ImageReader::readAsFolder) // correct for webp, not for videos

        // "xml" can be "dae" as well
        register("svg", SVGMesh.Companion::readAsFolder)

        // register yaml generally for unity files?
        registerFileExtension(UnityReader.unityExtensions) { it, c ->
            val f = UnityReader.readAsFolder(it) as? InnerFolder
            c(f, if (f == null) IOException("$it cannot be read as Unity project") else null)
        }

        // todo register windows lnk
        //  then remove all windows-lnk specific code
        //  then check whether the stuff still works

        // register windows url
        register("url", ::readURLAsFolder)
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
                    if (ec is ZipException && ec.message == "Archive is not a ZIP archive") {
                        LOGGER.warn("{} '{}'", ec.message, file)
                    } else ec?.printStackTrace()
                    Unit
                }
                if (reader != null) {
                    reader(file1, callback)
                } else {
                    InnerZipFile.createZipRegistryV2(file1, callback)
                }
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

    // opening a packed stream again would be really expensive for large packages
    // is there a better strategy than this?? maybe index a few on every go to load something
    var sizeLimit = 20_000_000L

    // private val LOGGER = LogManager.getLogger(ZipCache::class)

}