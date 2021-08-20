package me.anno.io.zip

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
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
import me.anno.mesh.vox.VOXReader
import org.apache.logging.log4j.LogManager

object ZipCache : CacheSection("ZipCache") {

    private val LOGGER = LogManager.getLogger(ZipCache::class)

    // done cache the whole content? if less than a certain file size
    // done cache the whole hierarchy [? only less than a certain depth level - not done]

    // todo read compressed exe files?


    // todo display unity packages differently: display them as their usual file structure
    // it kind of is a new format, that is based on another decompression

    fun getMeta(file: FileReference, async: Boolean): InnerFolder? {
        val data = getEntry(file.absolutePath, timeout, async) {
            CacheData(try {
                val signature = Signature.find(file)
                when (signature?.name) {
                    "7z" -> createZipRegistry7z(file) { fileFromStream7z(file) }
                    "rar" -> createZipRegistryRar(file) { fileFromStreamRar(file) }
                    "gzip", "tar" -> readAsGZip(file)
                    // todo all mesh extensions
                    "fbx", "gltf", "obj", "dae", "blend", "draco" -> AnimatedMeshesLoader.readAsFolder(file)
                    "vox" -> VOXReader.readAsFolder(file)
                    "zip" -> createZipRegistryV2(file) { fileFromStreamV2(file) }
                    null, "xml", "yaml" -> {
                        when (file.extension) {
                            // todo all mesh extensions
                            "fbx", "gltf", "glb", "obj", "dae", "blend", "draco" ->
                                AnimatedMeshesLoader.readAsFolder(file)
                            "vox" -> VOXReader.readAsFolder(file)
                            "mat", "prefab", "unity", "asset", "controller" -> UnityReader.readAsFolder(file)
                            else -> createZipRegistryV2(file) { fileFromStreamV2(file) }
                        }
                    }
                    else -> createZipRegistryV2(file) { fileFromStreamV2(file) }
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

    val timeout = 10000L

    // opening a packed stream again would be really expensive for large packages
    // todo is there a better strategy than this?? maybe index a few on every go to load something
    val sizeLimit = 100_000_000L

}