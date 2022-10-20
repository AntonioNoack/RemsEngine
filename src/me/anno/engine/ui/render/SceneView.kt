package me.anno.engine.ui.render

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.control.PlayControls
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.style.Style

@Suppress("MemberVisibilityCanBePrivate")
class SceneView(
    val library: EditorState, playMode: PlayMode, style: Style,
    val renderer: RenderView = RenderView(library, playMode, style)
) : PanelStack(style) {

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

    init {
        add(renderer)
        add(editControls)
        add(playControls)
    }

    override fun onUpdate() {
        super.onUpdate()
        val editing = renderer.playMode == PlayMode.EDITING
        editControls.isVisible = editing
        playControls.isVisible = !editing
        renderer.controlScheme = if (editing) editControls else playControls
    }

    companion object {

        fun testSceneWithUI(prefab: Prefab, init: (SceneView) -> Unit = {}) {
            testSceneWithUI(prefab.createInstance(), init)
        }

        fun testSceneWithUI(scene: PrefabSaveable, init: (SceneView) -> Unit = {}) {
            testUI {
                testScene(scene, init)
            }
        }

        @Suppress("unused")
        fun testScene(scene: PrefabSaveable, init: (SceneView) -> Unit = {}): Panel {
            scene.prefabPath = Path.ROOT_PATH
            EditorState.prefabSource = scene.ref
            val sceneView = SceneView(EditorState, PlayMode.EDITING, DefaultConfig.style)
            PrefabInspector.currentInspector = PrefabInspector(scene.ref)
            val list = CustomList(false, DefaultConfig.style)
            list.add(ECSTreeView(EditorState, DefaultConfig.style), 1f)
            list.add(sceneView, 3f)
            list.add(PropertyInspector({ EditorState.selection }, DefaultConfig.style), 1f)
            init(sceneView)
            return list
        }
    }

}