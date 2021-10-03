package me.anno.io.zip

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.ImageReader
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.unity.UnityReader
import me.anno.io.zip.InnerFile7z.Companion.createZipRegistry7z
import me.anno.io.zip.InnerFile7z.Companion.fileFromStream7z
import me.anno.io.zip.InnerRarFile.Companion.createZipRegistryRar
import me.anno.io.zip.InnerRarFile.Companion.fileFromStreamRar
import me.anno.io.zip.InnerTarFile.Companion.readAsGZip
import me.anno.io.zip.InnerZipFile.Companion.createZipRegistryV2
import me.anno.io.zip.InnerZipFile.Companion.fileFromStreamV2
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.obj.MTLReader2
import me.anno.mesh.obj.OBJReader2
import me.anno.mesh.vox.VOXReader
import me.anno.objects.documents.pdf.PDFCache
import org.apache.logging.log4j.LogManager

object ZipCache : CacheSection("ZipCache") {

    // private val LOGGER = LogManager.getLogger(ZipCache::class)

    // done cache the whole content? if less than a certain file size
    // done cache the whole hierarchy [? only less than a certain depth level - not done]

    // todo read compressed exe files?


    // done display unity packages differently: display them as their usual file structure
    // it kind of is a new format, that is based on another decompression

    fun unzipMaybe(file: FileReference): InnerFolder? {
        val data = getEntryWithoutGenerator(file) as? CacheData<*>
        return data?.value as? InnerFolder
    }

    fun unzip(file: FileReference, async: Boolean): InnerFolder? {
        val data = getFileEntry(file, false, timeout, async) { file1, _ ->
            CacheData(try {
                when (Signature.findName(file1)) {
                    "7z" -> createZipRegistry7z(file1) { fileFromStream7z(file1) }
                    "rar" -> createZipRegistryRar(file1) { fileFromStreamRar(file1) }
                    "gzip", "tar" -> readAsGZip(file1)
                    // todo all mesh extensions
                    "fbx", "gltf", "dae",
                    "draco", "md2", "md5mesh" ->
                        AnimatedMeshesLoader.readAsFolder(file1)
                    "blend" -> BlenderReader.readAsFolder(file1)
                    "obj" -> OBJReader2.readAsFolder(file1)
                    "mtl" -> MTLReader2.readAsFolder(file1)
                    "pdf" -> PDFCache.readAsFolder(file1)
                    "vox" -> VOXReader.readAsFolder(file1)
                    "zip" -> createZipRegistryV2(file1) { fileFromStreamV2(file1) }
                    // todo all image formats
                    "png", "jpg", "bmp", "pds", "hdr", "webp", "tga", "ico", "dds" ->
                        ImageReader.readAsFolder(file1)
                    null, "xml", "yaml", "json" -> {
                        when (file1.lcExtension) {
                            // todo all mesh extensions
                            "fbx", "gltf", "glb", "dae", "draco",
                            "md2", "md5mesh" ->
                                AnimatedMeshesLoader.readAsFolder(file1)
                            "obj" -> OBJReader2.readAsFolder(file1)
                            "blend" -> BlenderReader.readAsFolder(file1)
                            "mtl" -> MTLReader2.readAsFolder(file1)
                            "vox" -> VOXReader.readAsFolder(file1)
                            "mat", "prefab", "unity", "asset", "controller" -> UnityReader.readAsFolder(file1)
                            // todo all image formats
                            "png", "jpg", "bmp", "pds", "hdr", "webp", "tga", "dds" -> ImageReader.readAsFolder(
                                file1
                            )
                            "json" -> null
                            else -> createZipRegistryV2(file1) { fileFromStreamV2(file1) }
                        }
                    }
                    "media" -> ImageReader.readAsFolder(file1) // correct for webp, not for videos
                    else -> createZipRegistryV2(file1) { fileFromStreamV2(file1) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            })
        } as? CacheData<*>
        return data?.value as? InnerFolder
    }

    fun splitParent(name: String): Pair<String, String> {
        var path = name.replace('\\', '/')
        while (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val parent = if (nameIndex < 0) "" else path.substring(0, nameIndex)
        return parent to path
    }

    val timeout = 60_000L

    // opening a packed stream again would be really expensive for large packages
    // todo is there a better strategy than this?? maybe index a few on every go to load something
    val sizeLimit = 20_000_000L

}