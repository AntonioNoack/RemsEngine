package me.anno.engine

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.projects.GameEngineProject.Companion.currentProject
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.shader.ShaderLib
import me.anno.input.ActionManager
import me.anno.installer.Installer
import me.anno.io.files.FileReference
import me.anno.image.thumbs.ThumbsExt
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.engine.inspector.Inspectable
import me.anno.engine.projects.GameEngineProject
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
import me.anno.utils.OS
import org.joml.Matrix4f

// dithered shadows for transparent materials
// todo glass opaque-ness depends on color and alpha, not only on alpha
// glass is not opaque enough in current glass pass -> color-filtering must not depend on angle, only reflections shall depend on it
// -> todo glass generally looks bad in the engine, can we fix that somehow?

// todo implement exporting process:
//  - pack libraries and Universal into folder
//  - checkboxes for libraries/modules like Mesh to save space/complexity
//  - list of packed files/dynamically loaded files
//  - load main class?
//  - convert all file formats as needed

// to do Unity($)/RemsEngine(research) shader debugger:
//  - go up/down one instruction
//  - see local variables for all execution units
//  - printf() statements
//  - branching maps & branching results
//  - go into/out of/step

// pick random x: grep 'x' -r . | shuf -n 1 | head -n 1

// todo: translate everything (again xD)

// todo create first release :3 (to easier get started for others)

// todo billboards, which are conditionally rendered: when point is visible (for nice light camera-bug effects, e.g. stars with many blades)

// todo forward-plus rendering
// which platforms support Compute Shaders? we need them for forward+
// Windows, Android with OpenGL 3.0

// todo runtime-only-PrefabSaveables must show warning in UI, that they are temporary

// todo different editing modes like Blender?, e.g. animating stuff, scripting, ...

// todo spellchecking could then become a mod :)
//  - UI could become one, too,
//  - and ECS, but I'm not sure where to place their intersections
//  -> this is a build-stage optimization, so we could just annotate classes to be removed :)

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

open class RemsEngine : EngineBase("Rem's Engine", "RemsEngine", 1, true), WelcomeUI {

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
        ECSRegistry.initPrefabs()
        ECSRegistry.initMeshes()
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

        val animationWindow = CustomList(false, style)

        val libraryBase = EditorState
        val library = libraryBase.uiLibrary

        animationWindow.add(CustomContainer(ECSTreeView(style), library, style), 1f)
        animationWindow.add(CustomContainer(SceneView(PlayMode.EDITING, style), library, style), 3f)
        animationWindow.add(CustomContainer(PropertyInspector({ libraryBase.selection }, style), library, style), 1f)
        animationWindow.weight = 1f
        customUI.add(animationWindow, 2f)

        val explorers = CustomList(false, style).apply { weight = 0.3f }
        explorers.add(CustomContainer(ECSFileExplorer(projectFile, style), library, style))
        explorers.add(CustomContainer(ECSFileExplorer(OS.documents, style), library, style))

        customUI.add(explorers)
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
            private val modelMatrix = ThumbsExt.createModelMatrix().scale(0.62f)
            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                useFrame(previewRenderer) {
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
                    ThumbsExt.bindShader(shader, cameraMatrix, modelMatrix)
                    sky.material.bind(shader)
                    sky.getMesh().draw(shader, 0)
                }
            }
        }
    }

    override fun createProjectUI() {

        val windowStack = GFX.someWindow.windowStack
        val style = style
        val list = PanelListY(style)
        val options = OptionBar(style)
        val configTitle = Dict["Config", "ui.top.config"]
        options.addAction(configTitle, Dict["Settings", "ui.top.config.settings"]) {
            openConfigWindow(windowStack)
        }

        options.addAction(configTitle, Dict["Style", "ui.top.config.style"]) {
            openStylingWindow(windowStack)
        }

        list.add(options)

        list.add(ECSSceneTabs)

        val editUI = createDefaultMainUI(currentProject!!.location, style)
        list.add(editUI)

        list.add(ConsoleOutputPanel.createConsoleWithStats(true, style))
        windowStack.push(list)
        // could be drawDirectly, but the text quality of triangle- and draw count suffers from it
    }

    override fun loadProject(name: String, folder: FileReference): Pair<String, FileReference> {
        val project = GameEngineProject.readOrCreate(folder)!!
        currentProject = project
        project.init()
        val title = "$title - $name"
        for (window in GFX.windows) {
            window.title = title
        }
        EditorState.projectFile = project.location
        return name to folder
    }

    override fun createUI() {

        workspace = OS.documents.getChild("RemsEngine")
        workspace.tryMkdirs()

        getWelcomeUI().create(this)

        // do that now, because we now can support progress bars
        // todo collect progress bars from the start
        Installer.checkFFMPEGInstall()

        ShaderLib.init()

        val windowStack = GFX.windows.first().windowStack
        ECSSceneTabs.window = windowStack.firstElement()
        EngineActions.register()
        ActionManager.init()
    }

    override fun openHistory() {
        PrefabInspector.currentInspector?.history?.display()
    }

    companion object {

        fun openConfigWindow(windowStack: WindowStack) {
            val panel = ConfigPanel(DefaultConfig, false, style)
            val window = createReloadWindow(panel, transparent = false, fullscreen = true) { instance?.createUI() }
            panel.create()
            windowStack.push(window)
        }

        fun openStylingWindow(windowStack: WindowStack) {
            val panel = ConfigPanel(style.values, true, style)
            val window = createReloadWindow(panel, transparent = false, fullscreen = true) { instance?.createUI() }
            panel.create()
            windowStack.push(window)
        }

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
    }
}