package me.anno.engine

import me.anno.Engine
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.ui.base.progress.ProgressBar
import me.anno.utils.OS
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class GameEngineProject() : NamedSaveable() {

    companion object {
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

    val assetIndex = HashSet<Pair<FileReference, String>>()
    var maxIndexDepth = 5

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

        workspace = location

        // if last scene is invalid, create a valid scene
        if (lastScene == null) {
            lastScene = location.getChild("Scene.json").absolutePath
            LOGGER.debug("Set scene to $lastScene")
        }

        val lastSceneRef = lastScene!!.toGlobalFile(location)
        if (!lastSceneRef.exists) {
            val prefab = Prefab("Entity", ScenePrefab)
            lastSceneRef.getParent()?.tryMkdirs()
            lastSceneRef.writeText(TextWriter.toText(prefab, InvalidRef))
            LOGGER.debug("Wrote new scene to $lastScene")
        }

        assetIndex.clear()

        // may be changed by ECSSceneTabs otherwise
        val lastScene = lastScene
        // open all tabs
        for (tab in openTabs.toList()) {
            try {
                ECSSceneTabs.open(tab.toGlobalFile(workspace), PlayMode.EDITING, false)
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

        // to do if is Web, provide mechanism to index files...
        // we can't really do that anyway...
        if (!OS.isWeb) {
            thread(name = "Indexing Resources") {
                val progress = GFX.someWindow?.addProgressBar(object : ProgressBar("Indexing Assets", "Files", 1.0) {
                    override fun formatProgress(): String {
                        return "$name: ${progress.toLong()} / ${total.toLong()} $unit"
                    }
                })
                val resourcesToIndex = ArrayList<Pair<FileReference, Int>>()
                resourcesToIndex.add(location to maxIndexDepth)
                var processedFiles = 0L
                while (!Engine.shutdown && resourcesToIndex.isNotEmpty() && progress?.isCancelled != true) {
                    if (Engine.shutdown) break
                    if (progress != null) {
                        progress.total = (resourcesToIndex.size + processedFiles).toDouble()
                        progress.progress = processedFiles.toDouble()
                    }
                    val (file, depth) = resourcesToIndex.removeLast()
                    indexResource(file, depth, resourcesToIndex)
                    processedFiles++
                }
                progress?.finish(true)
            }
        }
    }

    fun indexResource(file: FileReference, depth: Int, resourcesToIndex: MutableList<Pair<FileReference, Int>>) {
        if (file.isDirectory) {
            if (depth > 0) {
                val depthM1 = depth - 1
                for (child in file.listChildren() ?: return) {
                    resourcesToIndex.add(child to depthM1)
                }
            }
        } else {
            Signature.findName(file) { sign ->
                when (sign) {
                    "png", "jpg", "exr", "qoi", "webp", "dds", "hdr", "ico", "gimp", "bmp" -> assetIndex.add(file to "Image")
                    "blend", "gltf", "dae", "md2", "vox", "fbx", "obj" -> assetIndex.add(file to "Mesh")
                    "media", "gif" -> assetIndex.add(file to "Media")
                    "pdf" -> assetIndex.add(file to "PDF")
                    "ttf", "woff1", "woff2" -> assetIndex.add(file to "")
                    else -> {
                        // todo specify timeout as 0ms
                        val prefab = PrefabCache[file, false]
                        if (prefab != null) {
                            assetIndex.add(file to prefab.clazzName)
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
        writer.writeInt("maxIndexDepth", maxIndexDepth)
        // location doesn't really need to be saved
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "lastScene" -> lastScene = value?.toGlobalFile()?.absolutePath ?: value
            else -> super.readString(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "maxIndexDepth" -> maxIndexDepth = value
            else -> super.readInt(name, value)
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
                openTabs.addAll(values.filter { it.exists }.map { it.toLocalPath(location) })
            }
            else -> super.readFileArray(name, values)
        }
    }

    override val className: String get() = "GameEngineProject"
    override val approxSize get() = 1_000_000
}