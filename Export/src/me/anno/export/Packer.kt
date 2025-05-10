package me.anno.export

import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.projects.FileEncoding
import me.anno.image.ImageCache
import me.anno.image.raw.IFloatImage
import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference
import me.anno.io.files.ImportType.AUDIO
import me.anno.io.files.ImportType.CUBEMAP_EQU
import me.anno.io.files.ImportType.IMAGE
import me.anno.io.files.ImportType.MESH
import me.anno.io.files.ImportType.METADATA
import me.anno.io.files.ImportType.VIDEO
import me.anno.io.files.InvalidRef
import me.anno.io.files.SignatureCache
import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream

object Packer {

    fun interface ProgressCallback {
        fun callback(done: Long, total: Long)
    }

    // remove unnecessary classes, if possible
    //  e.g., we only need a few image formats,
    // we don't need pdf etc. (except we export an editor), ...

    //////////////////////////////////////////
    //           features by size           //
    //////////////////////////////////////////

    // opengl          1.0  MB
    // openal          0.6  MB/platform

    // tar,7z,zip      0.6  MB
    // rotated jpegs   0.86 MB (definitively can be reduced)
    // xz              0.15 MB
    // rar (+vfs)      0.66 MB
    // psd,tiff,...    0.73 MB (Commons Imaging)

    // pdf             5.59 MB
    // JNA (trash)     2.88 MB

    // box2d           0.35 MB
    // bullet          0.76 MB

    // assimp         ~3.0  MB/platform

    // ogg (stb)       1.2  MB | 0.1 MB + 0.2MB/platform
    // fft             1.5  MB | JTransforms.jar, currently only used in Rem's Studio + could be reduced

    // ffmpeg         63.1  MB
    // ffprobe        63.0  MB

    // (dynamically loaded)
    // spellcheck    169.7  MB
    // spellcheck-en  82.6  MB


    // the best formats (probably)
    // images  png, jpg, hdr
    // audio  mp3 -> ogg is better?
    // video  mp4
    // documents  md/svg/pdf?
    // mesh   Rem's Engine .json


    private val LOGGER = LogManager.getLogger(Packer::class)

    private fun isLossyImageFormat(resource: FileReference): Boolean {
        return resource.absolutePath.contains(".jpg/", true) ||
                resource.absolutePath.contains(".jpeg/", true) ||
                resource.lcExtension == "jpg" || resource.lcExtension == "jpeg"
    }

    private fun packImage(resource: FileReference): ByteArray? {
        // don't save it as bmp, use png instead
        // if the original was a jpg, we should use jpg
        val image = ImageCache[resource, false]
        return if (image != null) {
            val extension = when {
                image is IFloatImage -> "hdr"
                isLossyImageFormat(resource) -> "jpg"
                else -> "png"
            }
            val tmp = ByteArrayOutputStream(1024)
            image.write(tmp, extension, 0.9f)
            tmp.toByteArray()
        } else null
    }

    private fun packVideo(resource: FileReference): ByteArray? {
        // todo encode video to chosen video format, e.g. mp4
        return null
    }

    private fun packAudio(resource: FileReference): ByteArray? {
        // todo encode audio to chosen audio format, e.g. mp3
        return null
    }

    fun isMaybePrefab(resource: FileReference): Boolean {
        return when (SignatureCache[resource, false]?.importType) {
            MESH, METADATA -> true
            else -> false
        }
    }

    private fun packPrefab(resource: FileReference, resourceMap: Map<FileReference, FileReference>): ByteArray? {
        val prefab = PrefabCache[resource] ?: return null
        val encoding = FileEncoding.BINARY
        val history = prefab.history
        // to do clone the prefab before unlinking history and deleting collapsed-data??
        prefab.history = null // not needed
        prefab.sets.removeIf { _, k2, _ -> k2 == "isCollapsed" }
        val bytes = encoding.encode(listOf(prefab), InvalidRef, resourceMap)
        prefab.history = history // restore history
        return bytes
    }

    /**
     * packs all resources as raw files into a map
     * returns the map of files to new names, so they can be replaced
     *
     * this is meant for shipping the game
     * */
    fun pack(
        resources0: List<FileReference>,
        dst: MutableMap<String, ByteArray>,
        reportProgress: ProgressCallback?
    ): Map<FileReference, String> {

        val resourceMap = findResources(resources0)
        val resourceList = resourceMap.entries.toList()
        val resourceSizeApprox = LongArray(resourceList.size) { resourceList[it].key.length() }

        var doneSize = 0L
        var totalSize = resourceSizeApprox.sum()
        for (i in resourceList.indices) {
            val (srcFile, dstFile) = resourceList[i]
            try {
                val importType = SignatureCache[srcFile, false]?.importType
                val bytes = when (importType) {
                    IMAGE, CUBEMAP_EQU -> packImage(srcFile)
                    VIDEO -> packVideo(srcFile)
                    AUDIO -> packAudio(srcFile)
                    MESH, METADATA -> packPrefab(srcFile, resourceMap)
                    else -> null
                } ?: srcFile.readBytesSync()
                val fileName = dstFile.absolutePath
                    .substring(BundledRef.PREFIX.length)
                dst[fileName] = bytes
                doneSize += bytes.size
                totalSize += bytes.size - resourceSizeApprox[i]
                reportProgress?.callback(doneSize, totalSize)
            } catch (e: Exception) {
                LOGGER.warn("Issue when copying $srcFile: ${e.message}")
                e.printStackTrace()
            }
        }
        return resourceMap.mapValues { (_, dstFile) ->
            dstFile.absolutePath.substring(BundledRef.PREFIX.length)
        }
    }

    /**
     * class to list all resources for a prefab, so it can be packed;
     * replaces those references with new, bundled references
     * */
    private fun findResources(resources0: Collection<FileReference>): Map<FileReference, FileReference> {

        var nextResourceId = 0
        fun nextName(lcExtension: String): FileReference {
            return res.getChild("res/${(nextResourceId++).toString(36)}.$lcExtension")
        }

        fun nextName(src: FileReference): FileReference {
            return nextName(src.lcExtension.ifEmpty {
                SignatureCache[src, false]?.name // mmmh...
            } ?: "bin")
        }

        val resources = HashMap<FileReference, FileReference>()
        val addedToList = HashSet<FileReference>()
        val remaining = ArrayList<FileReference>()
        addedToList.addAll(resources0)
        remaining.addAll(addedToList)

        fun foundSource(source: FileReference) {
            if (source.exists) {
                if (addedToList.add(source)) {
                    remaining.add(source)
                }
            }
        }

        while (remaining.isNotEmpty()) {
            val source = remaining.removeLast()
            if (!isMaybePrefab(source)) continue
            val prefab = PrefabCache[source] ?: continue
            resources[source] = nextName("json")

            // find all references
            for ((_, adds) in prefab.adds) {
                for (i in adds.indices) {
                    val add = adds[i]
                    foundSource(add.prefab)
                }
            }
            prefab.sets.forEach { _, _, v ->
                fun foundSth(v: Any?, depth: Int) {
                    if (v is FileReference) {
                        foundSource(v)
                    } else if (v is List<*> && depth < 3) {
                        for (vi in v.indices) {
                            foundSth(v[vi], depth + 1)
                        }
                    }
                }
                foundSth(v, 0)
            }
        }

        // add remaining names
        for (res in addedToList) {
            resources.getOrPut(res) { nextName(res) }
        }
        return resources
    }
}
