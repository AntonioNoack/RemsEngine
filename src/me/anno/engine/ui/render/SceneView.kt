package me.anno.engine.ui.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.Events.addEvent
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.control.DraggingControlSettings
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.control.PlayControls
import me.anno.engine.ui.scenetabs.ECSSceneTab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.engine.ui.vr.VRRenderingRoutine
import me.anno.gpu.GFX
import me.anno.graph.visual.Graph
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.graph.GraphEditor

@Suppress("MemberVisibilityCanBePrivate")
class SceneView(val renderView: RenderView, style: Style) : PanelStack(style) {

    constructor(playMode: PlayMode, style: Style) : this(RenderView0(playMode, style), style)

    var editControls: ControlScheme = DraggingControls(renderView)
        set(value) {
            if (field !== value) {
                remove(field)
                field = value
                add(value)
            }
        }

    var playControls: ControlScheme = PlayControls(renderView)
        set(value) {
            if (field !== value) {
                remove(field)
                field = value
                add(value)
            }
        }

    var renderMode: RenderMode
        get() = renderView.renderMode
        set(value) {
            val settings = editControls.settings
            if (settings is DraggingControlSettings) settings.renderMode = value
            else renderView.renderMode = value
        }

    // todo show the background renderer, when edited graph is RenderGraph
    var graphEditor = GraphEditor(null, style)

    init {
        add(renderView)
        add(editControls)
        add(playControls)
        add(graphEditor)
    }

    override fun onUpdate() {
        super.onUpdate()
        val world = renderView.getWorld()
        // todo get the node library from the graph somehow?
        graphEditor.graph = world as? Graph
        val worldIsGraph = world is Graph
        val editing = renderView.playMode == PlayMode.EDITING
        editControls.isVisible = !worldIsGraph && editing
        playControls.isVisible = !worldIsGraph && !editing
        renderView.controlScheme = if (editing) editControls else playControls
        renderView.controlScheme?.uiParent = this
        graphEditor.isVisible = worldIsGraph
        graphEditor.makeBackgroundTransparent()
        // graphEditor.backgroundColor = graphEditor.backgroundColor.withAlpha(127)
    }

    companion object {

        fun testSceneWithUI(title: String, source: FileReference, init: ((SceneView) -> Unit)? = null) {
            testUI3(title) { createSceneUI(source, init) }
        }

        fun testSceneWithUI(title: String, prefab: Prefab, init: ((SceneView) -> Unit)? = null) {
            testSceneWithUI(title, prefab.newInstance(), init)
        }

        fun testSceneWithUI(title: String, source: FileReference, renderMode: RenderMode) {
            testSceneWithUI(title, source) {
                (it.editControls as DraggingControls).settings.renderMode = renderMode
            }
        }

        fun testSceneWithUI(
            title: String, prefab: PrefabSaveable,
            renderMode: RenderMode, init: ((SceneView) -> Unit)? = null
        ) {
            testSceneWithUI(title, prefab) { sceneView ->
                (sceneView.editControls as DraggingControls).settings.renderMode = renderMode
                init?.invoke(sceneView)
            }
        }

        fun testSceneWithUI(title: String, scene: PrefabSaveable, init: ((SceneView) -> Unit)? = null) {
            testUI3(title) {
                GFX.someWindow.windowStack.firstOrNull()?.drawDirectly = false
                createSceneUI(scene, init)
            }
        }

        @Suppress("unused")
        fun createSceneUI(scene: PrefabSaveable, init: ((SceneView) -> Unit)? = null): Panel {
            scene.prefabPath = Path.ROOT_PATH
            return createSceneUI(scene.ref, init)
        }

        fun createSceneUI(scene: FileReference, init: ((SceneView) -> Unit)? = null): Panel {
            val listY = PanelListY(style)
            listY.add(ECSSceneTabs)
            ECSSceneTabs.open(ECSSceneTab(scene, PlayMode.EDITING), true)
            val sceneView = SceneView(PlayMode.EDITING, style)
            PrefabInspector.currentInspector = PrefabInspector(scene)
            val list = CustomList(false, style)
            list.add(ECSTreeView(style), 1f)
            list.add(sceneView, 3f)
            list.add(PropertyInspector({ EditorState.selection }, style), 1f)
            if (init != null) init(sceneView)
            tryStartVR(sceneView)
            listY.add(list)
            list.weight = 1f
            listY.weight = 1f
            return listY
        }

        fun testScene2(scene: PrefabSaveable, init: ((SceneView) -> Unit)? = null): Panel {
            scene.prefabPath = Path.ROOT_PATH
            EditorState.prefabSource = scene.ref
            val sceneView = SceneView(PlayMode.EDITING, style)
            PrefabInspector.currentInspector = PrefabInspector(scene.ref)
            if (init != null) init(sceneView)
            tryStartVR(sceneView)
            return sceneView
        }

        private fun tryStartVR(sceneView: SceneView) {
            addEvent {
                val osWindow = GFX.someWindow
                VRRenderingRoutine.tryStartVR(osWindow, sceneView.renderView)
            }
        }
    }
}