package me.anno.io.zip

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.instances.PDFCache
import me.anno.config.DefaultConfig
import me.anno.image.ImageReader
import me.anno.image.gimp.GimpImage
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.unity.UnityReader
import me.anno.io.zip.InnerFile7z.Companion.createZipRegistry7z
import me.anno.io.zip.InnerFile7z.Companion.fileFromStream7z
import me.anno.io.zip.InnerRarFile.Companion.createZipRegistryRar
import me.anno.io.zip.InnerRarFile.Companion.fileFromStreamRar
import me.anno.io.zip.InnerTarFile.Companion.readAsGZip
import me.anno.io.zip.InnerZipFile.Companion.createZipRegistryV2
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.obj.MTLReader2
import me.anno.mesh.obj.OBJReader2
import me.anno.mesh.vox.VOXReader
import java.io.IOException

object ZipCache : CacheSection("ZipCache") {

    // cache all content? if less than a certain file size
    // cache the whole hierarchy [? only less than a certain depth level - not done]

    // todo read compressed exe files?

    // done display unity packages differently: display them as their usual file structure
    // it kind of is a new format, that is based on another decompression

    private val readerBySignature = HashMap<String, (FileReference) -> InnerFile>(64)
    private val readerByFileExtension = HashMap<String, (FileReference) -> InnerFile>(64)

    @Suppress("unused")
    fun registerFileExtension(signature: String, reader: (FileReference) -> InnerFolder) {
        readerBySignature[signature] = reader
    }

    fun registerFileExtension(signatures: List<String>, reader: (FileReference) -> InnerFolder) {
        for (signature in signatures) {
            readerBySignature[signature] = reader
        }
    }

    fun register(signature: String, reader: (FileReference) -> InnerFolder) {
        readerBySignature[signature] = reader
    }

    fun register(signatures: List<String>, reader: (FileReference) -> InnerFolder) {
        for (signature in signatures) {
            readerBySignature[signature] = reader
        }
    }

    fun hasReaderForSignature(signature: String?): Boolean {
        return signature != null && signature in readerBySignature
    }

    fun hasReaderForFileExtension(lcExtension: String?): Boolean {
        return lcExtension != null && (lcExtension in readerByFileExtension || lcExtension in readerBySignature)
    }

    init {
        // compressed folders
        register(listOf("bz2", "lz4", "xar", "oar")) { createZipRegistryV2(it) }
        register("7z") { createZipRegistry7z(it) { fileFromStream7z(it) } }
        register("rar") { createZipRegistryRar(it) { fileFromStreamRar(it) } }
        register("gzip", ::readAsGZip)
        register("tar", ::readAsGZip)
        // pdf documents
        register("pdf", PDFCache::readAsFolder)
        // meshes
        // to do all mesh extensions
        register(listOf("fbx", "gltf", "dae", "draco", "md2", "md5mesh"), AnimatedMeshesLoader::readAsFolder)
        register("blend", BlenderReader::readAsFolder)
        register("obj", OBJReader2::readAsFolder)
        register("mtl", MTLReader2::readAsFolder)
        register("vox", VOXReader::readAsFolder)
        register("zip", ::createZipRegistryV2)
        // cannot be read by assimp anyway
        // registerFileExtension("max", AnimatedMeshesLoader::readAsFolder) // 3ds max file, idk about its file signature
        // images
        // to do all image formats
        val imageFormats = listOf("png", "jpg", "bmp", "pds", "hdr", "webp", "tga", "ico", "dds", "gif", "exr", "qoi")
        register(imageFormats, ImageReader::readAsFolder)
        register("gimp", GimpImage::readAsFolder)
        register("media", ImageReader::readAsFolder) // correct for webp, not for videos
        // register yaml generally for unity files?
        registerFileExtension(UnityReader.unityExtensions) {
            UnityReader.readAsFolder(it) as? InnerFolder
                ?: throw IOException("$it cannot be read as Unity project")
        }
    }

    fun unzipMaybe(file: FileReference): InnerFolder? {
        val data = getEntryWithoutGenerator(file) as? CacheData<*>
        return data?.value as? InnerFolder
    }

    fun unzip(file: FileReference, async: Boolean): InnerFile? {
        if (file is InnerFile && file.folder != null) return file.folder
        return getFileEntry(file, false, timeout, async) { file1, _ ->
            val signature = Signature.findName(file1)
            val ext = file1.lcExtension
            if (signature == "json" && ext == "json") null
            else {
                try {
                    val reader = readerBySignature[signature] ?: readerBySignature[ext] ?: readerByFileExtension[ext]
                    val folder = if (reader != null) reader(file1) else createZipRegistryV2(file1)
                    if (file1 is InnerFile) file1.folder = folder
                    folder
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } as? InnerFile
    }

    fun splitParent(name: String): Pair<String, String> {
        var path = name.replace('\\', '/')
        while (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val parent = if (nameIndex < 0) "" else path.substring(0, nameIndex)
        return parent to path
    }

    val timeout = DefaultConfig["zipCache.timeoutMillis", 60_000L]

    // opening a packed stream again would be really expensive for large packages
    // is there a better strategy than this?? maybe index a few on every go to load something
    val sizeLimit = DefaultConfig["zipCache.autoLoadLimit", 20_000_000L]

    // private val LOGGER = LogManager.getLogger(ZipCache::class)

}