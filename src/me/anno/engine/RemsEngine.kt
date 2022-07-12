package me.anno.engine

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache.loadScenePrefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.DefaultLayout
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.gpu.WindowX
import me.anno.gpu.shader.ShaderLib
import me.anno.input.ActionManager
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.studio.Inspectable
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.debug.FrameTimes
import me.anno.ui.editor.OptionBar
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.WelcomeUI
import me.anno.ui.editor.config.ConfigPanel
import me.anno.ui.utils.WindowStack.Companion.createReloadWindow
import me.anno.utils.OS
import me.anno.utils.files.Files.findNextFileName
import org.apache.logging.log4j.LogManager

// todo loading is slow: all tabs are loaded, even if only a single one is actually used

// todo forward-plus rendering
// which platforms support Compute Shaders? we need them for forward+
// Windows, Android with OpenGL 3.0

// todo foliage rendering... how ever we can do that at all scales...

// todo runtime-only-PrefabSaveables must show warning in UI, that they are temporary

// todo to reduce the size of the engine, physics engines could be turned into mods
// todo libraries like jpeg2000, pdf and such should become mods as well
// todo spellchecking could then become a mod :)


// todo right click on a path:
// todo - mutate -> create an asset based on that and replace the path, then inspect
// todo - inspect -> show it in the editor

// todo reduce skeletal animations to a single translation plus rotations only?
// todo animation matrices then can be reduced to rotation + translation

// games, which we want to develop:
//  - city builder
//          on a globe would be cool
//          with districts, giant world, connecting multiple cities together
//  - underground survival after apocalypse/crash: many dungeons, water break-ins, food resources, building electricity system,
//          and resource management
//  - minecraft like game with easy modding and plugin support, without version barriers, with Minecraft-Mod support
//  - gta / grand theft waifu,
//          money/car/heist stealing game with 3rd person shooting and car driving as main game loop
//          low poly style, because that's the only possible way for a hobby programmer
//          should be playable on keyboard/mouse + controller at the same time <3
//  - GTA x Minecraft? it would be nice to build in such a world... low poly could allow that by placing assets, and then
//          creating personal levels or bases with that
//  - Sims like game, just low-poly style
//          simlish should be easy ^^

open class RemsEngine : StudioBase(true, "Rem's Engine", "RemsEngine", 1) {

    lateinit var currentProject: GameEngineProject

    override fun loadConfig() {
        DefaultConfig.defineDefaultFileAssociations()
    }

    override fun onGameInit() {

        // CommandLineReader.start()
        ECSRegistry.init()
        startClock.stop("ECS Registry")

        Dict.loadDefault()
        startClock.stop("Dictionary")

        // pdf stuff
        LogManager.disableLogger("PDICCBased")
        LogManager.disableLogger("PostScriptTable")
        LogManager.disableLogger("GlyphSubstitutionTable")
        LogManager.disableLogger("GouraudShadingContext")
        LogManager.disableLogger("FontMapperImpl")
        LogManager.disableLogger("FileSystemFontProvider")
        LogManager.disableLogger("ScratchFileBuffer")
        LogManager.disableLogger("FontFileFinder")
        LogManager.disableLogger("PDFObjectStreamParser")
        LogManager.disableLogger("TriangleBasedShadingContext")
        LogManager.disableLogger("Type4ShadingContext")
        startClock.stop("Disable some loggers")

        // to avoid race conditions
        ScenePrefab.prefab.getSampleInstance()

    }

    override fun onGameLoopStart() {
        super.onGameLoopStart()
        GameEngine.scaledDeltaTime = Engine.gameTimeF * GameEngine.timeFactor
        GameEngine.scaledNanos += (GameEngine.scaledDeltaTime * 1e9).toLong()
        GameEngine.scaledTime = GameEngine.scaledNanos * 1e-9
    }

    override fun isSelected(obj: Any?) =
        EditorState.selection.contains(obj)

    override fun onGameLoop(window: WindowX, w: Int, h: Int) {
        DefaultConfig.saveMaybe("main.config")
        super.onGameLoop(window, w, h)
    }

    override fun save() {
        ECSSceneTabs.currentTab?.save()
    }

    fun loadSafely(file: FileReference): Prefab {
        if (file.exists && !file.isDirectory) {
            return try {
                loadScenePrefab(file)
            } catch (e: Exception) {
                val nextName = findNextFileName(file, 1, '-')
                LOGGER.warn("Could not open $file", e)
                val newFile = file.getSibling(nextName)
                return loadSafely(newFile)
            }
        } else {
            val prefab = Prefab("Entity")
            prefab.source = file
            return prefab
        }
    }

    override fun clearAll() {
        val selected = collectSelected()
        super.clearAll()
        restoreSelected(selected)
    }

    override fun createUI() {

        workspace = OS.documents.getChild("RemsEngine")
        workspace.tryMkdirs()

        object : WelcomeUI() {
            override fun createProjectUI() {

                val windowStack = GFX.windows.first().windowStack
                val style = style
                val list = PanelListY(style)
                val options = OptionBar(style)
                val configTitle = Dict["Config", "ui.top.config"]
                options.addAction(configTitle, Dict["Settings", "ui.top.config.settings"]) {
                    val panel = ConfigPanel(DefaultConfig, false, style)
                    val window = createReloadWindow(panel, true) { createUI() }
                    panel.create()
                    windowStack.push(window)
                }

                options.addAction(configTitle, Dict["Style", "ui.top.config.style"]) {
                    val panel = ConfigPanel(DefaultConfig.style.values, true, style)
                    val window = createReloadWindow(panel, true) { createUI() }
                    panel.create()
                    windowStack.push(window)
                }

                list.add(options)

                list.add(ECSSceneTabs)

                val editUI = DefaultLayout.createDefaultMainUI(currentProject.location, style)
                list.add(editUI)

                list.add(ConsoleOutputPanel.createConsoleWithStats(true, style))
                windowStack.push(list)
            }

            override fun loadProject(name: String, folder: FileReference): Pair<String, FileReference> {
                currentProject = GameEngineProject.readOrCreate(folder)!!
                currentProject.init()
                EditorState.projectFile = currentProject.location
                return name to folder
            }
        }.create(this)

        ShaderLib.init()
        ECSShaderLib.init()

        // todo different editing modes like Blender?, e.g. animating stuff, scripting, ...

        val windowStack = GFX.windows.first().windowStack
        ECSSceneTabs.window = windowStack.firstElement()
        EngineActions.register()
        ActionManager.init()

    }

    class RuntimeInfoPlaceholder : Panel(style) {
        override fun calculateSize(w: Int, h: Int) {
            minW = if (instance?.showFPS == true) FrameTimes.width else 0
            minH = 1
        }
    }

    override fun run() {
        instance2 = this
        super.run()
    }

    override fun openHistory() {
        PrefabInspector.currentInspector?.history?.display()
    }

    companion object {

        fun collectSelected(): Any {
            val selection = EditorState.selection.map { (it as? PrefabSaveable)?.prefabPath ?: it }
            val lastSelection = EditorState.lastSelection.run { (this as? PrefabSaveable)?.prefabPath ?: this }
            return Pair(selection, lastSelection)
        }

        fun restoreSelected(collected: Any) {
            val (selection, lastSelection) = @Suppress("unchecked_cast")
            (collected as Pair<List<Any>, Any?>)
            // restore the current selection
            // reloaded prefab; must not be accessed before clearAll
            val prefab = EditorState.prefab
            val sample = prefab?.getSampleInstance()
            if (prefab != null && sample != null) {
                EditorState.selection = selection
                    .mapNotNull { if (it is Path) Hierarchy.getInstanceAt(sample, it) else it }
                    .filterIsInstance<Inspectable>()
                EditorState.lastSelection = lastSelection.run {
                    if (this is Path) Hierarchy.getInstanceAt(sample, this) else (this as? Inspectable)
                }
            }
            PropertyInspector.invalidateUI(true)
        }

        private val LOGGER = LogManager.getLogger(RemsEngine::class)
        var instance2: RemsEngine? = null

        @JvmStatic
        fun main(args: Array<String>) {
            RemsEngine().run()
        }

    }

}