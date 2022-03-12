package me.anno.engine.ui.render

import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.control.PlayControls
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.style.Style

@Suppress("MemberVisibilityCanBePrivate")
class SceneView(val library: EditorState, playMode: PlayMode, style: Style) : PanelStack(style) {

    // todo how do we define the camera? RenderView
    // todo function to switch it :)

    val renderer = RenderView(library, playMode, style)

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

    override fun tickUpdate() {
        super.tickUpdate()
        val editing = renderer.playMode == PlayMode.EDITING
        editControls.visibility = Visibility[editing]
        playControls.visibility = Visibility[!editing]
        renderer.controlScheme = if (editing) editControls else playControls
    }

}