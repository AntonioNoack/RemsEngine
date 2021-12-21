package me.anno.engine

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabCache.loadScenePrefab
import me.anno.engine.ui.DefaultLayout
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.Window
import me.anno.input.ActionManager
import me.anno.language.translation.Dict
import me.anno.studio.StudioBase
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.StudioActions
import me.anno.ui.base.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.debug.FrameTimes
import me.anno.ui.debug.RuntimeInfoPanel
import me.anno.ui.editor.OptionBar
import me.anno.ui.editor.UILayouts.createReloadWindow
import me.anno.ui.editor.config.ConfigPanel
import me.anno.utils.OS
import me.anno.utils.hpc.SyncMaster
import org.apache.logging.log4j.LogManager

// todo fix: tree view, adding entities is no longer working
// todo also the main object randomly just shrinks down

// todo fix: tooltip texts of properties are not being displayed


// todo right click on a path:
// todo - mutate -> create an asset based on that and replace the path, then inspect
// todo - inspect -> show it in the editor

// todo drop in meshes
// todo drop in ui maybe...
// todo key listeners (?)...

// todo reduce animations to a single translation plus rotations only?
// todo animation matrices then can be reduced to rotation + translation

// could not reproduce it lately -> was it fixed?
// to do bug: long text field is broken...

class RemsEngine : StudioBase(true, "Rem's Engine", "RemsEngine", 1) {

    lateinit var currentProject: GameEngineProject

    val syncMaster = SyncMaster()

    override fun loadConfig() {
        DefaultConfig.defineDefaultFileAssociations()
        DefaultConfig.init()
    }

    override fun onGameInit() {

        // CommandLineReader.start()
        ECSRegistry.init()

        Dict.loadDefault()

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

    }

    override fun onGameLoopStart() {
        super.onGameLoopStart()
        GameEngine.scaledDeltaTime = GFX.deltaTime * GameEngine.timeFactor
        GameEngine.scaledNanos += (GameEngine.scaledDeltaTime * 1e9).toLong()
        GameEngine.scaledTime = GameEngine.scaledNanos * 1e-9
    }

    override fun onGameLoop(w: Int, h: Int): Boolean {
        DefaultConfig.saveMaybe("main.config")
        return super.onGameLoop(w, h)
    }

    override fun save() {
        ECSSceneTabs.currentTab?.save()
    }

    override fun createUI() {

        val projectFile = OS.documents.getChild("RemsEngine").getChild("SampleProject")
        currentProject = GameEngineProject.readOrCreate(projectFile)!!
        currentProject.init()

        ShaderLib.init()
        ECSShaderLib.init()

        // todo select project view, like Rem's Studio
        // todo select scene
        // todo show scene, and stuff, like Rem's Studio

        // todo different editing modes like Blender?, e.g. animating stuff, scripting, ...
        // todo and always be capable to change stuff

        // todo create our editor, where we can drag stuff into the scene, view it in 3D, move around, and such
        // todo play the scene

        // todo base shaders, which can be easily made touch-able

        // for testing directly jump in the editor

        val editScene = loadScenePrefab(projectFile.getChild("Scene.json"))

        val style = style

        val list = PanelListY(style)

        val isGaming = false
        EditorState.syncMaster = syncMaster
        EditorState.projectFile = editScene.source

        ECSSceneTabs.open(syncMaster, editScene)
        // ECSSceneTabs.add(syncMaster, projectFile.getChild("2ndScene.json"))


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

        val editUI = DefaultLayout.createDefaultMainUI(projectFile, syncMaster, isGaming, style)
        list.add(editUI)

        val bottom2 = PanelStack(style)
        bottom2 += RemsStudio.createConsole(style)
        val right = PanelListX(style)
        right.makeBackgroundTransparent()
        right.add(RuntimeInfoPanel(style).apply {
            alignmentX = AxisAlignment.MAX
            makeBackgroundOpaque()
            setWeight(1f)
        })
        right.add(RuntimeInfoPlaceholder())
        bottom2.add(right)
        list.add(bottom2)
        windowStack.push(list)

        ECSSceneTabs.window = windowStack.firstElement()
        StudioActions.register()
        ActionManager.init()

    }

    class RuntimeInfoPlaceholder : Panel(style) {
        override fun calculateSize(w: Int, h: Int) {
            minW = if (instance.showFPS) FrameTimes.width else 0
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