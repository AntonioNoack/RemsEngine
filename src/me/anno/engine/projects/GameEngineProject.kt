package me.anno.engine.projects

import me.anno.Engine
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.EngineBase
import me.anno.engine.Events
import me.anno.engine.ScenePrefab
import me.anno.engine.inspector.Inspectable
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.extensions.events.Event
import me.anno.gpu.GFX
import me.anno.image.thumbs.Thumbs
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.SignatureCache
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.NamedSaveable
import me.anno.io.saveable.Saveable
import me.anno.ui.base.progress.ProgressBar
import me.anno.utils.OS
import me.anno.utils.algorithms.Recursion
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.promise
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.types.Floats.toLongOr
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class GameEngineProject() : NamedSaveable(), Inspectable {

    /**
     * is called when a project has been loaded
     * */
    class ProjectLoadedEvent(val project: GameEngineProject) : Event()

    companion object {

        var currentProject: GameEngineProject? = null

        val encoding get() = currentProject?.encoding ?: FileEncoding.PRETTY_JSON

        private val LOGGER = LogManager.getLogger(GameEngineProject::class)
        fun readOrCreate(location: FileReference?, callback: Callback<GameEngineProject>) {
            if (location == null || location == InvalidRef) return callback.err(null)
            val location2 = if (location.exists && !location.isDirectory) location.getParent() else location
            val configFile = location2.getChild("Project.json")

            promise { cb -> readProject(location2, configFile, cb) }
                .then(callback)
                .catch { callback.ok(createProject(location2, configFile)) }
        }

        private fun readProject(
            location: FileReference, configFile: FileReference,
            callback: Callback<GameEngineProject>
        ) {
            if (!configFile.exists) return callback.err(null)
            configFile.readText(callback.map { stream ->
                val instance = JsonStringReader
                    .readFirstOrNull(stream, location, GameEngineProject::class)
                    ?: createProject(location, configFile)
                instance.location = location
                instance
            })
        }

        private fun createProject(
            location: FileReference,
            configFile: FileReference,
        ): GameEngineProject {
            val project = GameEngineProject(location)
            val encoding = encoding.getForExtension(location)
            configFile.writeBytes(encoding.encode(project, location))
            return project
        }

        fun save(file: FileReference, value: Saveable) {
            return save(file, listOf(value))
        }

        fun save(file: FileReference, values: List<Saveable>) {
            val workspace0 = currentProject?.location ?: InvalidRef
            val workspace1 =
                if (workspace0 == EngineBase.workspace && file.isSubFolderOf(workspace0)) workspace0
                else InvalidRef
            // depending on extension, choose different encoding
            val encoding = encoding.getForExtension(file)
            file.writeBytes(encoding.encode(values, workspace1))
        }

        fun invalidateThumbnails(sourceFiles: Collection<FileReference>) {
            // invalidate all thumbnails:
            val project = currentProject
            val fileDependencies = if (project != null) {
                Recursion.collectRecursive2(sourceFiles) { file, remaining ->
                    remaining.addAll(project.findDependencies(file))
                }
            } else sourceFiles
            for (dependency in fileDependencies) {
                println("Invalidating dependency $dependency")
                Thumbs.invalidate(dependency)
            }
        }
    }

    constructor(location: FileReference) : this() {
        this.location = location
        location.tryMkdirs()
    }

    var location: FileReference = InvalidRef // a folder
    var lastScene: FileReference = InvalidRef
    val openTabs = ArrayList<FileReference>()

    val configFile get() = location.getChild("Project.json")
    var encoding = FileEncoding.PRETTY_JSON

    val assetIndex = HashMap<String, HashSet<FileReference>>()
    val dependencyToPrefab = HashMap<FileReference, HashSet<FileReference>>()
    var maxIndexDepth = 5

    fun addToAssetIndex(file: FileReference, type: String) {
        assetIndex.getOrPut(type) { HashSet() }.add(file)
    }

    fun setDependencies(prefabFile: FileReference, dependencies: HashSet<FileReference>) {
        dependencyToPrefab[prefabFile] = dependencies
    }

    fun addDependency(prefabFile: FileReference, dependency: FileReference) {
        dependencyToPrefab.getOrPut(prefabFile, ::HashSet).add(dependency)
    }

    fun findDependencies(file: FileReference): Set<FileReference> {
        return dependencyToPrefab.filterValues { dependencies ->
            dependencies.any { dependency -> dependency.isSameOrSubFolderOf(file) }
        }.keys
    }

    private var isValid = true

    /**
     * save project config after a small delay
     * */
    @DebugAction
    fun save() {
        if (isValid) {
            isValid = false
            Events.addEvent {
                if (!isValid) {
                    isValid = true
                    configFile.writeText(JsonStringWriter.toText(this, location))
                    LOGGER.info("Saved Project")
                }
            }
        }
    }

    fun init() {

        EngineBase.workspace = location

        // if last scene is invalid, create a valid scene
        if (lastScene == InvalidRef) {
            lastScene = location.getChild("Scene.${encoding.extension}")
            LOGGER.info("Set scene to {}", lastScene)
        }

        val lastSceneRef = lastScene
        if (!lastSceneRef.exists && lastSceneRef != InvalidRef) {
            val prefab = Prefab("Entity", ScenePrefab)
            lastSceneRef.getParent().tryMkdirs()
            val encoding = encoding.getForExtension(lastSceneRef)
            lastSceneRef.writeBytes(encoding.encode(prefab, InvalidRef))
            LOGGER.warn("Wrote new scene to {}", lastScene)
        }

        assetIndex.clear()

        // may be changed by ECSSceneTabs otherwise
        val lastScene = lastScene

        // open all tabs
        for (i in openTabs.indices) {
            val tab = openTabs[i]
            try {
                ECSSceneTabs.open(tab, PlayMode.EDITING, false)
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
                val progressBar = GFX.someWindow.addProgressBar(object : ProgressBar("Indexing Assets", "Files", 1.0) {
                    override fun formatProgress(): String {
                        return "$name: ${progress.toLongOr()} / ${total.toLongOr()} $unit"
                    }
                })
                val filesToIndex = ArrayList<FileReference>()
                indexFolder(progressBar, location, maxIndexDepth, filesToIndex)
                while (!Engine.shutdown && filesToIndex.isNotEmpty() && !progressBar.isCancelled) {
                    progressBar.progress += 1.0
                    indexResource(filesToIndex.removeFirst())
                }
                progressBar.finish(true)
            }
        }
    }

    private fun indexFolder(
        progressBar: ProgressBar,
        file: FileReference, depth: Int,
        resourcesToIndex: MutableCollection<FileReference>
    ) {
        val depthM1 = depth - 1
        if (!file.isDirectory || Engine.shutdown || progressBar.isCancelled) return
        val children = file.listChildren()
        progressBar.total += children.size
        progressBar.progress += 1.0
        for (child in children) {
            if (child.isDirectory) {
                if (depthM1 >= 0) {
                    indexFolder(progressBar, child, depthM1, resourcesToIndex)
                }
            } else resourcesToIndex.add(child)
        }
    }

    private fun indexResource(file: FileReference) {
        if (file.isDirectory) return
        SignatureCache.getAsync(file) { signature ->
            when (signature?.name) {
                "png", "jpg", "exr", "qoi", "webp", "dds", "hdr", "ico", "gimp", "bmp" -> {
                    addToAssetIndex(file, "Image") // cpu-side name
                    addToAssetIndex(file, "Texture") // gpu-side name
                }
                "blend", "gltf", "dae", "md2", "vox", "fbx", "obj", "ma", "ply" -> addToAssetIndex(file, "Mesh")
                "media", "gif" -> addToAssetIndex(file, "Media")
                "pdf" -> addToAssetIndex(file, "PDF")
                "ttf", "woff1", "woff2" -> addToAssetIndex(file, "Font")
                else -> {
                    val timeout = 0L // because we don't really need it
                    val prefab = PrefabCache[file, Prefab.maxPrefabDepth, timeout, false]
                    if (prefab != null) {
                        addToAssetIndex(file, prefab.clazzName)
                        setDependencies(file, prefab.dependencies)
                    }
                }
            }
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("lastScene", lastScene)
        writer.writeFileList("openTabs", openTabs)
        writer.writeInt("maxIndexDepth", maxIndexDepth)
        writer.writeInt("encoding", encoding.id)
        // location doesn't really need to be saved
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "lastScene" -> {
                lastScene = (value as? FileReference)
                    ?: ((value as? String)?.toGlobalFile(location))
                            ?: return
            }
            "maxIndexDepth" -> maxIndexDepth = value as? Int ?: return
            "openTabs" -> {
                val values = value as? List<*> ?: return
                openTabs.clear()
                openTabs.addAll(values.filterIsInstance2(String::class).map { it.toGlobalFile(location) })
                openTabs.addAll(values.filterIsInstance2(FileReference::class))
            }
            "encoding" -> encoding = FileEncoding.entries.getOrNull(value as? Int ?: return) ?: encoding
            else -> super.setProperty(name, value)
        }
    }

    override val approxSize get() = 1_000_000
}