package me.anno.io.unity

import me.anno.image.thumbs.AssetThumbHelper
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerLinkFile
import me.anno.io.unity.UnityReader.assetExtension
import me.anno.io.unity.UnityReader.readUnityObjects
import me.anno.io.yaml.generic.YAMLNode
import me.anno.io.yaml.generic.YAMLReader
import me.anno.utils.Clock
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.LogManager

/**
 * in a Unity file,
 * - there are fileIDs, which are local ids to objects of that same file
 * - there is a guid. This is the identifier for the file
 * */
class UnityProject(val root: FileReference) : InnerFolder(root) {

    init {
        assertTrue(root != InvalidRef)
    }

    val clock = Clock(LOGGER)

    private val registry = HashMap<String, FileReference>()
    private val yamlCache = HashMap<FileReference, YAMLNode>()

    // guid -> folder file for the decoded instances
    val files = HashMap<String, InnerFolder>()

    private fun getCachedYAML(file: FileReference): YAMLNode {
        return synchronized(this) {
            yamlCache.getOrPut(file) {
                if (file.lcExtension == "meta") file.hide()
                try {
                    file.inputStreamSync().bufferedReader().use { reader ->
                        YAMLReader.parseYAML(reader, true)
                    }
                } catch (e: Exception) {
                    LOGGER.warn("$e by $file")
                    throw e
                }
            }
        }
    }

    fun parse(node: YAMLNode, guid: String, file: InnerFolder) {
        readUnityObjects(node, guid, this, file)
    }

    private fun getGuid(metaFile: FileReference): String? {
        return getMeta(metaFile)["Guid"]?.value
    }

    fun getGuidFolder(metaFile: FileReference): FileReference {
        val guid = getGuid(metaFile) ?: return InvalidRef
        return getGuidFolder(guid)
    }

    fun getGuidFolder(guid: String): FileReference {
        synchronized(this) {
            var folder = files[guid]
            if (folder == null && isValidUUID(guid)) {
                val guidObject = registry[guid]
                if (guidObject == null) {
                    LOGGER.warn("GUID '$guid' was not found in registry@$root!")
                    return InvalidRef
                }
                // this looks much nicer, because then we have the file name in the name, not just IDs
                // folder = InnerFolder(content)
                // but it would also override the original resources...
                folder = InnerFolder("${root.absolutePath}/$guid", guid, root)
                folder.hide()
                files[guid] = folder
                when (guidObject.lcExtension) {
                    in AssetThumbHelper.unityExtensions1 -> {
                        val node = getCachedYAML(guidObject)
                        parse(node, guid, folder)
                    }
                    else -> {
                        // probably a binary file
                        // create a fake link file
                        // find file id
                        // to do there may be actual useful data in the meta file,
                        // to do e.g. import settings
                        // to do use this data to create a prefab, which then links to the original file
                        val meta = getMeta(guidObject)
                        val fileId = getMainId(meta)
                        // LOGGER.info("[89] fileId from $meta: $fileId, created link")
                        InnerLinkFile(folder, fileId ?: guidObject.name, guidObject)
                    }
                }
            }
            if (folder == null) {
                LOGGER.warn("Could not find folder for GUID $guid")
            }
            return folder ?: InvalidRef
        }
    }

    fun getMainId(node: YAMLNode): String? {
        // NativeFormatImporter:
        //  mainObjectFileID
        val value = node["NativeFormatImporter"]?.get("MainObjectFileID")?.value
        return if (value == null) null else value + assetExtension
    }

    override fun getChildImpl(name: String): FileReference {
        val superChild = super.getChildImpl(name)
        return if (!superChild.exists && isValidUUID(name)) {
            getGuidFolder(name)
        } else superChild
    }

    fun getMeta(metaFile: FileReference): YAMLNode {
        var file = metaFile
        if (metaFile.lcExtension != "meta") {
            val quickAnswer = yamlCache[metaFile]
            if (quickAnswer != null) return quickAnswer
            file = metaFile.getParent().getChild(metaFile.name + ".meta")
        }
        file.hide()
        return getCachedYAML(file)
    }

    fun register(guid: String, assetFile: FileReference) {
        synchronized(this) {
            registry[guid] = assetFile
        }
    }

    fun register(file: FileReference, maxDepth: Int = 10): Boolean {
        if (!file.exists) return false
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
                if (maxDepth <= 0) return false
                for (child in file.listChildren()) {
                    register(child, maxDepth - 1)
                }
            }
            else -> {
                when (file.lcExtension) {
                    "meta"/*, "mat", "prefab", "unity", "asset"*/ -> {
                        // this metadata isn't necessarily the same as file.getSibling!
                        val yaml = getCachedYAML(file)
                        val guid = yaml["Guid"]?.value
                        if (guid != null) {
                            val sibling = file.getSibling(file.nameWithoutExtension)
                            register(guid, sibling) // metadata != yaml
                        } else LOGGER.warn("Didn't find guid in $file")
                    }
                }
            }
        }
        return true
    }

    override fun toString(): String {
        return "UnityProject[$root, ${files.size} files indexed]"
    }

    companion object {

        private val LOGGER = LogManager.getLogger(UnityProject::class)

        fun isValidUUID(name: String): Boolean {
            for (char in name) {
                if (char !in 'A'..'Z' &&
                    char !in 'a'..'z' &&
                    char !in '0'..'9'
                ) return false
            }
            return name.isNotEmpty()
        }
    }
}