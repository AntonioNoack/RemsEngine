package me.anno.engine

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache.loadScenePrefab
import me.anno.engine.ui.DefaultLayout
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib
import me.anno.input.ActionManager
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.debug.FrameTimes
import me.anno.ui.editor.OptionBar
import me.anno.ui.editor.WelcomeUI
import me.anno.ui.editor.config.ConfigPanel
import me.anno.ui.utils.WindowStack.Companion.createReloadWindow
import me.anno.utils.OS
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.hpc.SyncMaster
import org.apache.logging.log4j.LogManager

// todo file explorer sometimes switches from files to folders ... why?

// todo bug: tooltip texts of properties are not being displayed

// todo runtime-components must have warning
// todo must be editable -> no CSet/CAdd, just instance changes


// todo color input sometimes janky... why?

// todo panel: console output of multiple lines, with filters

// todo also the main object randomly just shrinks down (pool & truck)


// todo to reduce the size of the engine, physics engines could be turned into mods
// todo libraries like jpeg2000, pdf and such should become mods as well
// todo spellchecking could then become a mod :)


// todo right click on a path:
// todo - mutate -> create an asset based on that and replace the path, then inspect
// todo - inspect -> show it in the editor

// todo drop in meshes
// todo drop in ui maybe...

// todo reduce skeletal animations to a single translation plus rotations only?
// todo animation matrices then can be reduced to rotation + translation

// could not reproduce it lately -> was it fixed?
// to do bug: long text field is broken...


// todo games, which we want to develop:
//  - city builder
//          on a globe would be cool
//          with districts, giant world, connecting multiple cities together
//  - underground survival after apocalypse/crash: many dungeons, water break-ins, food resources, building electricity system,
//          and resource management
//  - minecraft like game with easy modding and plugin support, without version barriers, with Minecraft-Mod support
//  - gta / grand theft waifu,
//          money/car/heist stealing game with 3rd person shooting and car driving as main game loop
//          low poly style, because that's the only possible way for a hobby programmer
//  - GTA x Minecraft? it would be nice to build in such a world... low poly could allow that by placing assets, and then
//          creating personal levels or bases with that
//  - Sims like game, just low-poly style
//          simlish should be easy ^^

// stuff, which originated from Rem's Studio:
// todo split the rendering in two parts:
// todo - without blending (no alpha, video or polygons)
// todo - with blending
// todo enqueue all objects for rendering
// todo sort blended objects by depth, if rendering with depth

class RemsEngine : StudioBase(true, "Rem's Engine", "RemsEngine", 1) {

    lateinit var currentProject: GameEngineProject

    val syncMaster = SyncMaster()

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
        startClock.stop("Disable some loggers")

    }

    override fun onGameLoopStart() {
        super.onGameLoopStart()
        GameEngine.scaledDeltaTime = Engine.gameTimeF * GameEngine.timeFactor
        GameEngine.scaledNanos += (GameEngine.scaledDeltaTime * 1e9).toLong()
        GameEngine.scaledTime = GameEngine.scaledNanos * 1e-9
    }

    override fun isSelected(obj: Any?): Boolean {
        return EditorState.selection.contains(obj) ||
                EditorState.fineSelection.contains(obj)
    }

    override fun onGameLoop(w: Int, h: Int) {
        DefaultConfig.saveMaybe("main.config")
        super.onGameLoop(w, h)
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

    override fun createUI() {

        workspace = OS.documents.getChild("RemsEngine")
        workspace.tryMkdirs()

        object : WelcomeUI() {
            override fun createProjectUI() {

                val editScene = loadSafely(currentProject.lastScene)

                val style = style

                val list = PanelListY(style)

                val isGaming = false
                EditorState.syncMaster = syncMaster
                EditorState.projectFile = editScene.source

                ECSSceneTabs.open(syncMaster, editScene, PlayMode.EDITING)

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

                val editUI = DefaultLayout.createDefaultMainUI(currentProject.location, syncMaster, isGaming, style)
                list.add(editUI)

                list.add(ConsoleOutputPanel.createConsoleWithStats(true, style))
                windowStack.push(list)
            }

            override fun loadProject(name: String, folder: FileReference): Pair<String, FileReference> {
                currentProject = GameEngineProject.readOrCreate(folder)!!
                currentProject.init()
                return name to folder
            }
        }.create(this)

        ShaderLib.init()
        ECSShaderLib.init()

        // todo different editing modes like Blender?, e.g. animating stuff, scripting, ...
        // todo and always be capable to change stuff

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

    companion object {

        private val LOGGER = LogManager.getLogger(RemsEngine::class)
        var instance2: RemsEngine? = null

        @JvmStatic
        fun main(args: Array<String>) {
            RemsEngine().run()
        }

    }

}