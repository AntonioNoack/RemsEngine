package me.anno.engine

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.System
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.inspector.Inspectable
import me.anno.engine.projects.GameEngineProject
import me.anno.engine.projects.GameEngineProject.Companion.currentProject
import me.anno.engine.projects.ProjectHeader
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.engine.ui.vr.VRRenderingRoutine.Companion.tryStartVR
import me.anno.extensions.events.EventBroadcasting.callEvent
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.pipeline.Pipeline
import me.anno.image.thumbs.AssetThumbHelper
import me.anno.input.ActionManager
import me.anno.installer.Installer
import me.anno.io.files.FileReference
import me.anno.io.utils.StringMap
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.WindowStack
import me.anno.ui.WindowStack.Companion.createReloadWindow
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.editor.OptionBar
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.WelcomeUI
import me.anno.ui.editor.config.ConfigPanel
import me.anno.ui.editor.config.ConfigType
import me.anno.utils.OS
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.types.Strings.ifBlank2
import org.joml.Matrix4f

// to do Unity($)/RemsEngine(research) shader debugger:
//  - go up/down one instruction
//  - see local variables for all execution units
//  - printf() statements
//  - branching maps & branching results
//  - go into/out of/step

// todo: translate everything (again xD)

// todo billboards, which are conditionally rendered: when point is visible (for nice light camera-bug effects, e.g. stars with many blades)

// todo different editing modes like Blender?, e.g. animating stuff, scripting, ...

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

open class RemsEngine : EngineBase(NameDesc("Rem's Engine"), "RemsEngine", 1, true), WelcomeUI {

    override fun loadConfig() {
        DefaultConfig.defineDefaultFileAssociations()
    }

    override fun onGameInit() {

        // CommandLineReader.start()
        ECSRegistry.init()
        startClock.stop("ECS Registry")

        Dict.loadDefault()
        startClock.stop("Dictionary")

        // to avoid race conditions
        ScenePrefab.prefab.value.getSampleInstance()
        startClock.stop("Sample Scene")

        OfficialExtensions.register()
        startClock.stop("Loading Plugins")
    }

    override fun isSelected(obj: Any?) =
        EditorState.selection.contains(obj)

    override fun onGameLoop(window: OSWindow, w: Int, h: Int) {
        DefaultConfig.saveMaybe("main.config")
        super.onGameLoop(window, w, h)
    }

    override fun save() {
        try {
            // if we inspect/edit another prefab currently, we need to save that!
            PrefabInspector.currentInspector?.save()
            // save scene
            ECSSceneTabs.currentTab?.save()
        } catch (e: Exception) {
            e.printStackTrace()
            Menu.msg(NameDesc(e.toString()))
        }
    }

    override fun clearAll() {
        val selected = collectSelected()
        super.clearAll()
        restoreSelected(selected)
    }

    fun createDefaultMainUI(projectFile: FileReference, style: Style): Panel {

        val customUI = CustomList(true, style)
        customUI.weight = 10f

        val libraryBase = EditorState
        val library = libraryBase.uiLibrary

        val sceneView = SceneView(PlayMode.EDITING, style)
        val top = CustomList(false, style)
        top.add(CustomContainer(ECSTreeView(style), library, style), 1f)
        top.add(CustomContainer(sceneView, library, style), 3f)
        top.add(CustomContainer(PropertyInspector({ libraryBase.selection }, style), library, style), 1f)
        top.weight = 1f
        customUI.add(top, 2f)

        val bottom = CustomList(false, style).apply { weight = 0.3f }
        bottom.add(CustomContainer(ECSFileExplorer(projectFile, style), library, style))
        bottom.add(CustomContainer(ECSFileExplorer(OS.documents, style), library, style))
        customUI.add(bottom)

        ECSSceneTabs.currentTab?.applyRadius(sceneView.renderView)

        val osWindow = GFX.someWindow
        tryStartVR(osWindow, sceneView.renderView)

        return customUI
    }

    fun getWelcomeUI(): WelcomeUI {
        return this
    }

    override fun createBackground(style: Style): Panel {
        return object : Panel(style) {
            val sky = Skybox()
            val cameraMatrix = Matrix4f()
            override val canDrawOverBorders get() = true
            private val modelMatrix = AssetThumbHelper.createModelMatrix().scale(0.62f)
            private val pipeline = Pipeline(null)
            override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
                useFrame(previewRenderer) {
                    GFXState.drawingSky.use(true) {
                        sky.nadirSharpness = 10f
                        val shader = sky.shader!!.value
                        shader.use()
                        shader.v1f("meshScale", 1f)
                        shader.v1b("isPerspective", true)
                        shader.v1b("reverseDepth", false)
                        Perspective.setPerspective(
                            cameraMatrix,
                            0.7f,
                            (x1 - x0) * 1f / (y1 - y0),
                            0.001f, 10f, 0f, 0f
                        )
                        AssetThumbHelper.bindShader(shader, cameraMatrix, modelMatrix)
                        sky.material.bind(shader)
                        sky.getMesh().draw(pipeline, shader, 0)
                    }
                }
            }
        }
    }

    override fun createProjectUI() {

        val osWindow = GFX.someWindow
        val windowStack = osWindow.windowStack
        val style = style
        val list = PanelListY(style)
        val options = OptionBar(style)
        val configTitle = Dict["Config", "ui.top.config"]
        options.addAction(configTitle, Dict["Settings", "ui.top.config.settings"]) {
            openConfigWindow(windowStack)
        }
        options.addAction(configTitle, Dict["Project Settings", "ui.top.config.projectSettings"]) {
            val project = currentProject
            if (project != null) EditorState.select(project)
        }
        options.addAction(configTitle, Dict["Style", "ui.top.config.style"]) {
            openStylingWindow(windowStack)
        }
        options.addAction(configTitle, Dict["Keymap", "ui.top.config.keymap"]) {
            openKeymapWindow(windowStack)
        }

        list.add(options)
        list.add(ECSSceneTabs)

        val project = currentProject!!
        val editUI = createDefaultMainUI(project.location, style)
        list.add(editUI)

        list.add(ConsoleOutputPanel.createConsoleWithStats(true, style))
        windowStack.push(list)
        // could be drawDirectly, but the text quality of triangle- and draw count suffers from it

        callEvent(GameEngineProject.ProjectLoadedEvent(project))
    }

    override fun loadProject(name: String, folder: FileReference, callback: Callback<ProjectHeader>) {
        GameEngineProject.readOrCreate(folder, callback.map { project -> loadProjectImpl(project) })
    }

    override fun loadProjectHeader(folder: FileReference, callback: Callback<ProjectHeader>) {
        GameEngineProject.read(folder, callback.map {
            ProjectHeader(it.name.ifBlank2(it.location.name), it.location)
        })
    }

    fun loadProjectImpl(project: GameEngineProject): ProjectHeader {
        val name = project.name.ifBlank2(project.location.name)
        currentProject = project
        project.init()
        val title = "${nameDesc.name} - $name"
        for (window in GFX.windows) {
            window.title = title
        }
        EditorState.projectFile = project.location
        return ProjectHeader(name, project.location)
    }

    override fun createUI() {

        workspace = OS.documents.getChild("RemsEngine")
        workspace.tryMkdirs()

        getWelcomeUI().create(this)

        Installer.checkFFMPEGInstall()

        val windowStack = GFX.windows.first().windowStack
        ECSSceneTabs.window = windowStack.first()
        EngineActions.register()
        ActionManager.init()
    }

    override fun openHistory() {
        PrefabInspector.currentInspector?.history?.display()
    }

    companion object {

        fun openConfigWindow(windowStack: WindowStack, config: StringMap, type: ConfigType) {
            val panel = ConfigPanel(config, type, style)
            val window = createReloadWindow(panel, transparent = false, fullscreen = true) { instance?.createUI() }
            windowStack.push(window)
        }

        fun openConfigWindow(windowStack: WindowStack) {
            openConfigWindow(windowStack, DefaultConfig, ConfigType.SETTINGS)
        }

        fun openStylingWindow(windowStack: WindowStack) {
            openConfigWindow(windowStack, style.values, ConfigType.STYLE)
        }

        fun openKeymapWindow(windowStack: WindowStack) {
            openConfigWindow(windowStack, ActionManager, ConfigType.KEYMAP)
        }

        fun collectSelected(): Any {
            val selection = EditorState.selection.map { inspectableToCollectable(it) }
            val lastSelection = inspectableToCollectable(EditorState.lastSelection)
            return Pair(selection, lastSelection)
        }

        private fun inspectableToCollectable(instance: Inspectable?): Any? {
            return if (instance is PrefabSaveable && instance !is System) {
                // Systems are PrefabSaveable, but don't have a path (yet?)
                instance.prefabPath
            } else instance
        }

        fun restoreSelected(collected: Any) {
            if (collected !is Pair<*, *>) return
            val (selection, lastSelection) = collected
            if (selection !is List<Any?>) return
            // restore the current selection
            // reloaded prefab; must not be accessed before clearAll
            PrefabCache[EditorState.prefabSource].waitFor { prefab, err ->
                err?.printStackTrace()
                val sample = prefab?.sample
                if (sample is PrefabSaveable) {
                    EditorState.selection = selection
                        .mapNotNull { if (it is Path) Hierarchy.getInstanceAt(sample, it) else it }
                        .filterIsInstance<Inspectable>()
                    EditorState.lastSelection = lastSelection.run {
                        if (this is Path) Hierarchy.getInstanceAt(sample, this) else (this as? Inspectable)
                    }
                }
                PropertyInspector.invalidateUI(true)
            }
        }
    }
}