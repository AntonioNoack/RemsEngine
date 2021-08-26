package me.anno.io.unity

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.unity.UnityReader.readUnityObjects
import me.anno.io.yaml.YAMLNode
import me.anno.io.yaml.YAMLReader.parseYAML
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerLinkFile
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager
import java.io.IOException

/**
 * in a Unity file,
 * - there are fileIDs, which are local ids to objects of that same file
 * - there is a guid. This is the identifier for the file
 * */
class UnityProject(val root: FileReference) : InnerFolder(root) {

    val clock = Clock()

    private val registry = HashMap<String, GuidObject>()
    private val yamlCache = HashMap<FileReference, YAMLNode>()

    // guid -> folder file for the decoded instances
    val files = HashMap<String, InnerFolder>()

    fun getYAML(file: FileReference): YAMLNode {
        return synchronized(this) {
            yamlCache.getOrPut(file) {
                try {
                    parseYAML(file.readText(), true)
                } catch (e: Exception) {
                    LOGGER.warn("$e by $file")
                    throw e
                }
            }
        }
    }

    fun request(guid: String, fileId: String): FileReference {
        return getGuidFolder(guid).getChild(fileId)
    }

    fun parse(node: YAMLNode, guid: String, file: InnerFolder) {
        readUnityObjects(node, guid, this, file)
    }

    private fun getGuid(metaFile: FileReference): String {
        return getMeta(metaFile)["Guid"]!!.value!!
    }

    fun getGuidFolder(metaFile: FileReference): FileReference {
        return getGuidFolder(getGuid(metaFile))
    }

    fun getGuidFolder(guid: String): FileReference {
        synchronized(this) {
            var folder = files[guid]
            if (folder == null && isValidUUID(guid)) {
                val guidObject = registry[guid] ?: return InvalidRef
                folder = InnerFolder("${root.absolutePath}/$guid", guid, root)
                files[guid] = folder
                val content = guidObject.contentFile
                when (content.lcExtension) {
                    "asset", "unity", "mat", "prefab" -> {
                        val node = getYAML(guidObject.contentFile)
                        parse(node, guid, folder)
                    }
                    else -> {
                        // probably a binary file
                        // create a fake link file
                        // find file id
                        // todo there may be actual useful data in the meta file,
                        // todo e.g. import settings
                        // todo use this data to create a prefab, which then links to the original file
                        val meta = getMeta(content)
                        val fileId = getMainId(meta)
                        // LOGGER.info("fileId from $meta: $fileId")
                        InnerLinkFile(folder, fileId ?: content.name, content)
                    }
                }
            }
            return folder ?: InvalidRef
        }
    }

    fun getMainId(node: YAMLNode): String? {
        // NativeFormatImporter:
        //  mainObjectFileID
        return node["NativeFormatImporter"]?.get("MainObjectFileID")?.value
    }

    override fun getChild(name: String): FileReference {
        val superChild = super.getChild(name)
        return if (!superChild.exists && isValidUUID(name)) {
            getGuidFolder(name)
        } else superChild
    }

    fun getMeta(metaFile: FileReference): YAMLNode {
        return if (metaFile.extension != "meta") {
            getMeta(metaFile.getSibling(metaFile.name + ".meta"))
        } else getYAML(metaFile)
    }

    fun register(guid: String, metaFile: FileReference, assetFile: FileReference) {
        try {
            synchronized(this) {
                registry[guid] = GuidObject(parseYAML(metaFile.readText(), true), metaFile, assetFile)
            }
        } catch (e: Exception) {
            LOGGER.warn("$e in $metaFile")
            e.printStackTrace()
        }
    }

    fun register(file: FileReference, maxDepth: Int = 10) {
        if (!file.exists) return
        clock.update(
            {
                "Loading project '${root.name}': ${yamlCache.size}, ${
                    file.absolutePath
                        .substring(root.absolutePath.length + 1)
                }"
            }, 0.5
        )
        when {
            file.isDirectory -> {
                if (maxDepth <= 0) return
                for (child in file.listChildren() ?: return) {
                    register(child, maxDepth - 1)
                }
            }
            else -> {
                when (file.lcExtension) {
                    "meta"/*, "mat", "prefab", "unity", "asset"*/ -> {
                        try {
                            val yaml = parseYAML(file.readText(), true)
                            val guid = yaml["guid"]?.value
                            if (guid != null) {
                                val content = if (file.extension == "meta") {
                                    file.getSibling(file.nameWithoutExtension)
                                } else null
                                registry[guid] = GuidObject(yaml, file, content ?: InvalidRef)
                            }
                            yamlCache[file] = yaml
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    companion object {

        val invalidProject = UnityProject(InvalidRef)

        fun isValidUUID(name: String): Boolean {
            for (char in name) {
                if (char !in 'A'..'Z' && char !in 'a'..'z' && char !in '0'..'9') return false
            }
            return name.isNotEmpty()
        }

        private val LOGGER = LogManager.getLogger(UnityProject::class)
    }
}