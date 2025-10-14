package me.anno.engine.ui

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.ui.AssetImportType.getPureTypeOrNull
import me.anno.gpu.GFX
import me.anno.image.ImageReadable
import me.anno.image.thumbs.ThumbnailCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.LastModifiedCache
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.files.Files
import me.anno.utils.structures.maps.LazyMap
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream

// todo bug: shallow-copying animations doesn't work properly :/
// todo support recursive references, e.g., skeleton -> animations (old) -> skeleton

object AssetImport {

    private val LOGGER = LogManager.getLogger(AssetImport::class)

    fun shallowCopyImport(dstRootFolder: FileReference, files: List<FileReference>, fe: FileExplorer?) {
        executeImport(dstRootFolder, files, fe, false)
    }

    fun deepCopyImport(dstRootFolder: FileReference, files: List<FileReference>, fe: FileExplorer?) {
        executeImport(dstRootFolder, files, fe, true)
    }

    private fun executeImport(
        dstRootFolder: FileReference, srcFiles: List<FileReference>,
        fe: FileExplorer?, deepCopy: Boolean
    ) {

        val progress = GFX.someWindow.addProgressBar("Deep Copy", "", srcFiles.size.toDouble())

        val allSourceFiles = HashSet<FileReference>(srcFiles)
        val remaining = ArrayList<FileReference>(srcFiles)
        lateinit var fileMapping: LazyMap<FileReference, FileReference>

        val alreadyGenerating = HashSet<FileReference>()

        // todo bug: this breaks for recursive links,
        //  e.g. skeleton -> sample-animations -> skeleton

        fun copyPrefab(srcFile: FileReference): FileReference {

            if (srcFile == InvalidRef) { // easy :)
                return InvalidRef
            }

            val byMapping = fileMapping.getOrNull(srcFile)
            if (byMapping != null) return byMapping

            if (srcFile in alreadyGenerating) {
                // bad, this is a recursive trap
                val dstFolder = dstRootFolder
                val name = srcFile.nameWithoutExtension
                val ext = "json"
                val dstFile = Files.findNextFile(dstFolder, name, ext, 3, '-', 1)
                LOGGER.warn("There's a recursive dependency on '$srcFile', creating temporary prefab in '${dstFile.name}'")
                dstFile.writeText(JsonStringWriter.toText(Prefab("Entity"), InvalidRef)) // temporary
                fileMapping[srcFile] = dstFile
                return dstFile
            }

            if (!deepCopy && srcFile !in allSourceFiles) {
                LOGGER.info("Just linking $srcFile")
                fileMapping[srcFile] = srcFile
                return srcFile
            }

            val srcPrefab = loadPrefab(srcFile)
                ?: return InvalidRef

            val dstFolderName = when (srcPrefab.clazzName) {
                "Entity" -> ""
                "Material" -> "materials"
                "Mesh" -> "meshes"
                "BoneByBoneAnimation", "ImportedAnimation" -> "animations"
                "Skeleton" -> "skeletons"
                else -> srcPrefab.clazzName
            }

            val dstFolder =
                if (dstFolderName.isNotEmpty()) dstRootFolder.getChild(dstFolderName)
                else dstRootFolder

            LOGGER.info("Choose '$dstFolderName' for ${srcPrefab.clazzName} in '$srcFile'")

            alreadyGenerating.add(srcFile)

            val workspace = workspace
            val writer = JsonStringWriter(workspace)
            writer.resourceMap = fileMapping
            writer.add(srcPrefab)
            writer.writeAllInList()
            val asString = writer.toString()

            val newFile = fileMapping.getOrNull(srcFile)
            if (newFile != null) {
                newFile.writeText(asString)
                return newFile
            }

            val name = findName(srcFile, srcPrefab)
            val dstFile = saveContent(dstFolder, name, "json", asString.encodeToByteArray())
            fileMapping[srcFile] = dstFile
            return dstFile
        }

        fileMapping = LazyMap { srcFile ->

            val pureType = getPureTypeOrNull(srcFile)
            if (pureType != null) {
                val mappedName = when (pureType) {
                    "Image", "Video" -> "textures"
                    "Font" -> "fonts"
                    "Audio" -> "sounds"
                    "Link" -> "links"
                    else -> "raw"
                }
                LOGGER.info("$srcFile is raw file of type '$pureType' -> '$mappedName'")
                val dstFolder = dstRootFolder.getChild(mappedName)
                copyPureFile(srcFile, dstFolder)
            } else {
                val children = srcFile.listChildren()
                if (children.isNotEmpty()) {
                    LOGGER.info("$srcFile is a folder with children: ${children.map { it.name }}")
                    progress.total += children.size
                    allSourceFiles.addAll(children)
                    remaining.addAll(children)
                    InvalidRef
                } else if (!srcFile.isDirectory) {
                    val prefab = loadPrefab(srcFile)
                    if (prefab == null) {
                        val dstFolder = dstRootFolder.getChild("raw")
                        copyPureFile(srcFile, dstFolder)
                    } else {
                        LOGGER.info("$srcFile is a prefab, class ${prefab.clazzName}")
                        copyPrefab(srcFile)
                    }
                } else {
                    LOGGER.info("$srcFile is a folder without children")
                    InvalidRef
                }.apply {
                    progress.progress++
                }
            }
        }

        while (remaining.isNotEmpty()) {
            val file = remaining.first()
            fileMapping[file]
            remaining.remove(file)
        }

        onCopyFinished(dstRootFolder, fe)
        progress.finish(true)
    }

    private fun onCopyFinished(dst: FileReference, fe: FileExplorer?) {
        fe?.invalidate()
        LastModifiedCache.invalidate(dst)
        // update icon of current folder... hopefully this works
        ThumbnailCache.invalidate(dst)
        ThumbnailCache.invalidate(dst.getParent())
    }

    private fun getImageReadableExtension(srcFile: FileReference): String {
        return srcFile.getParent().lcExtension.ifEmpty { srcFile.lcExtension }
    }

    private fun getPureFileExtension(srcFile: FileReference): String {
        var ext = srcFile.lcExtension
        if (srcFile is ImageReadable) {
            // finding better extension: component images are always .png, but their source might be jpg,
            // so we can save lots of space by using the original format
            ext = getImageReadableExtension(srcFile)
        }
        return ext
    }

    private fun getPureFileBytes(srcFile: FileReference): ByteArray {
        // storage-space optimization: without this, we'd write a BMP file
        return if (srcFile is ImageReadable) {
            // finding better extension: component images are always .png, but their source might be jpg,
            // so we can save lots of space by using the original format
            val ext = getImageReadableExtension(srcFile)
            // creating a temporary buffer, where we can write into
            val tmp = ByteArrayOutputStream()
            srcFile.readCPUImage().write(tmp, ext)
            tmp.toByteArray()
        } else if (srcFile.exists) {
            srcFile.readBytesSync()
        } else {
            LOGGER.warn("Missing $srcFile")
            ByteArray(0)
        }
    }

    private fun copyPureFile(srcFile: FileReference, dstFolder: FileReference): FileReference {
        val ext = getPureFileExtension(srcFile)
        val bytes = getPureFileBytes(srcFile)
        val name = srcFile.nameWithoutExtension
        return saveContent(dstFolder, name, ext, bytes)
    }

    private fun loadPrefab(srcFile: FileReference): Prefab? {
        return try {
            PrefabCache[srcFile].waitFor()?.prefab
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findName(srcFile: FileReference, prefab: Prefab?): String {
        val prefabName = prefab?.instanceName?.toAllowedFilename()
        val fileName = srcFile.nameWithoutExtension.toAllowedFilename()
        var name = fileName ?: srcFile.getParent().nameWithoutExtension
        if (name == "BoneByBone" || name == "BoneByBone_InPlace" || name == "Imported" || name == "Imported_InPlace") {
            name = "${srcFile.getParent().name}/$name"
        }
        if (name.toIntOrNull() != null) {
            name = prefabName ?: "Scene"
        }
        if (name == "Scene") {
            // rename to file name
            name = srcFile.getParent().nameWithoutExtension
        }
        return name
    }

    private fun saveContent(dstFolder: FileReference, name: String, ext: String, data: ByteArray): FileReference {
        var dstFile = dstFolder.getChild("$name.$ext")
        if (dstFile.exists) {
            // compare the contents: if identical, we can use it
            val data0 = try {
                dstFile.readBytesSync()
            } catch (_: Exception) {
                null
            }
            // todo this comparison can only be done when everything has been renamed... [ShallowCopy]
            // todo we also have the issue, that we should compare with all siblings
            if (!data0.contentEquals(data)) {
                LOGGER.info("Files differ [$name]: ${data0?.hashCode()}[${data0?.size}] vs ${data.hashCode()}[${data.size}]")
                dstFile = Files.findNextFile(dstFolder, name, ext, 3, '-', 1)
            }
        }
        dstFile.getParent().tryMkdirs()
        dstFile.writeBytes(data)
        return dstFile
    }
}