package me.anno.engine

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.hpc.ProcessingQueue
import org.apache.logging.log4j.LogManager
import java.io.IOException

class GameEngineProject() : NamedSaveable() {

    companion object {
        private val assetIndexQueue = ProcessingQueue("AssetIndex")
        private val LOGGER = LogManager.getLogger(GameEngineProject::class)
        fun readOrCreate(location: FileReference?): GameEngineProject? {
            location ?: return null
            if (location == InvalidRef) return null
            return if (location.exists) {
                if (location.isDirectory) {
                    val configFile = location.getChild("config.json")
                    if (configFile.exists) {
                        val instance = TextReader.readFirstOrNull<GameEngineProject>(configFile, location)
                        instance?.location = location
                        instance
                    } else {
                        val project = GameEngineProject(location)
                        configFile.writeText(TextWriter.toText(project, location))
                        project
                    }
                } else {
                    // probably the config file
                    readOrCreate(location.getParent())
                }
            } else GameEngineProject(location)
        }
    }

    constructor(location: FileReference) : this() {
        this.location = location
        location.tryMkdirs()
    }

    var location: FileReference = InvalidRef // a folder
    var lastScene: String? = null
    val openTabs = HashSet<String>()

    val configFile get() = location.getChild("config.json")

    val assetIndex = HashSet<FileReference>()

    private var isValid = true
    fun invalidate() {
        // save project config after a small delay
        if (isValid) {
            isValid = false
            addEvent {
                if (!isValid) {
                    isValid = true
                    configFile.writeText(TextWriter.toText(this, location))
                }
            }
        }
    }

    fun forAllPrefabs(run: (FileReference, Prefab) -> Unit) {
        forAllFiles { file ->
            val prefab = PrefabCache[file]
            if (prefab != null) run(file, prefab)
        }
    }

    fun forAllFiles(run: (FileReference) -> Unit) {
        forAllFiles(location, 10, run)
    }

    fun forAllFiles(folder: FileReference, maxDepth: Int, run: (FileReference) -> Unit) {
        try {
            // to do go into things as well???
            for (child in folder.listChildren() ?: return) {
                if (child.isDirectory) {
                    if (maxDepth > 0) {
                        forAllFiles(child, maxDepth - 1, run)
                    }
                } else {
                    run(child)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun init() {
        StudioBase.workspace = location

        // if last scene is invalid, create a valid scene
        if (lastScene == null) {
            lastScene = location.getChild("Scene.json").absolutePath
            LOGGER.debug("Set scene to $lastScene")
        }

        val lastSceneRef = getReference(lastScene)
        if (!lastSceneRef.exists) {
            val prefab = Prefab("Entity", ScenePrefab)
            lastSceneRef.writeText(TextWriter.toText(prefab, InvalidRef))
            LOGGER.debug("Wrote new scene to $lastScene")
        }

        assetIndex.clear()
        indexResources(location)

        // may be changed by ECSSceneTabs otherwise
        val lastScene = lastScene
        // open all tabs
        for (tab in openTabs) {
            try {
                ECSSceneTabs.open(getReference(tab), PlayMode.EDITING, false)
            } catch (e: Exception) {
                LOGGER.warn("Could not open $tab", e)
            }
        }
        // make last scene current
        try {
            ECSSceneTabs.open(lastSceneRef, PlayMode.EDITING, true)
        } catch (e: Exception) {
            LOGGER.warn("Could not open $lastScene", e)
        }
    }

    fun indexResources(file: FileReference, depth: Int = 2) {
        if (file.isDirectory) {
            for (child in file.listChildren()?.toList() ?: return) {
                try {
                    indexResources(child, depth)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            Signature.findName(file) { sign ->
                when (sign) {
                    "png", "jpg", "gimp", "blend", "gltf", "dae", "md2", "exr", "qoi",
                    "media", "vox", "fbx", "obj", "webp", "dds", "hdr", "ico", "pdf",
                    "ttf", "woff1", "woff2", "gif", "bmp" -> assetIndex.add(file)
                    else -> {
                        if (depth >= 0 && file.isSomeKindOfDirectory) {
                            val children = file.listChildren()
                            if (children != null) for (child in children.toList()) { // .toList() is to prevent concurrency issues
                                try {
                                    indexResources(child, depth - 1)
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            // todo make this async
                            val prefab = PrefabCache[file, false]
                            if (prefab != null) {
                                assetIndex.add(file)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("lastScene", lastScene)
        writer.writeStringArray("openTabs", openTabs.toTypedArray())
        // location doesn't really need to be saved
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "lastScene" -> lastScene = value?.toGlobalFile()?.absolutePath ?: value
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "lastScene" -> lastScene = value.absolutePath
            else -> super.readFile(name, value)
        }
    }

    override fun readStringArray(name: String, values: Array<String>) {
        when (name) {
            "openTabs" -> {
                openTabs.clear()
                openTabs.addAll(values)
            }
            else -> super.readStringArray(name, values)
        }
    }

    override fun readFileArray(name: String, values: Array<FileReference>) {
        when (name) {
            "openTabs" -> {
                openTabs.clear()
                openTabs.addAll(values.filter { it.exists }.map { it.absolutePath })
            }
            else -> super.readFileArray(name, values)
        }
    }

    override val className get() = "GameEngineProject"
    override val approxSize get() = 1_000_000

}