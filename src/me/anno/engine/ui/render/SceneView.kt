package me.anno.engine.ui.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.control.PlayControls
import me.anno.engine.ui.scenetabs.ECSSceneTab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.graph.Graph
import me.anno.graph.ui.GraphEditor
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.PropertyInspector

@Suppress("MemberVisibilityCanBePrivate")
class SceneView(val renderer: RenderView, style: Style) : PanelStack(style) {

    constructor(playMode: PlayMode, style: Style) : this(RenderView0(playMode, style), style)

    var editControls: ControlScheme = DraggingControls(renderer)
        set(value) {
            if (field !== value) {
                remove(field)
                field = value
                add(value)
            }
        }

    var playControls: ControlScheme = PlayControls(renderer)
        set(value) {
            if (field !== value) {
                remove(field)
                field = value
                add(value)
            }
        }

    var graphEditor: GraphEditor = GraphEditor(null, style)

    init {
        fill(renderer)
        fill(editControls)
        fill(playControls)
        fill(graphEditor)
    }

    fun fill(ui: Panel) {
        add(ui)
        ui.alignmentX = AxisAlignment.FILL
        ui.alignmentY = AxisAlignment.FILL
    }

    override fun onUpdate() {
        super.onUpdate()
        val world = renderer.getWorld()
        // todo get the node library from the graph somehow?
        graphEditor.graph = world as? Graph
        val worldIsGraph = world is Graph
        val editing = renderer.playMode == PlayMode.EDITING
        editControls.isVisible = !worldIsGraph && editing
        playControls.isVisible = !worldIsGraph && !editing
        renderer.controlScheme = if (editing) editControls else playControls
        renderer.isVisible = !worldIsGraph
        graphEditor.isVisible = worldIsGraph
    }

    override val className: String
        get() = "SceneView"

    companion object {

        fun testSceneWithUI(title: String, source: FileReference, init: ((SceneView) -> Unit)? = null) {
            testUI3(title) { testScene(source, init) }
        }

        fun testSceneWithUI(title: String, prefab: Prefab, init: ((SceneView) -> Unit)? = null) {
            testSceneWithUI(title, prefab.createInstance(), init)
        }

        fun testSceneWithUI(title: String, scene: PrefabSaveable, init: ((SceneView) -> Unit)? = null) {
            testUI3(title) {
                GFX.someWindow?.windowStack?.firstOrNull()?.drawDirectly = false
                testScene(scene, init)
            }
        }

        @Suppress("unused")
        fun testScene(scene: PrefabSaveable, init: ((SceneView) -> Unit)? = null): Panel {
            scene.prefabPath = Path.ROOT_PATH
            return testScene(scene.ref, init)
        }

        fun testScene(scene: FileReference, init: ((SceneView) -> Unit)? = null): Panel {
            val listY = PanelListY(style)
            listY.add(ECSSceneTabs)
            ECSSceneTabs.open(ECSSceneTab(scene, PlayMode.EDITING), true)
            val sceneView = SceneView(PlayMode.EDITING, style)
            PrefabInspector.currentInspector = PrefabInspector(scene)
            val list = CustomList(false, style)
            list.add(ECSTreeView(EditorState, style), 1f)
            list.add(sceneView, 3f)
            list.add(PropertyInspector({ EditorState.selection }, style), 1f)
            if (init != null) init(sceneView)
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
            return sceneView
        }
    }
}