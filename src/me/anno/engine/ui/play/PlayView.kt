package me.anno.engine.ui.play

import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.style.Style

class PlayView(val library: EditorState, val testing: Boolean, style: Style) : PanelStack(style) {

    // todo how do we define the camera? search scene, and assign?
    // todo function to switch it :)

    val renderer = RenderView(library, if (testing) PlayMode.PLAY_TESTING else PlayMode.PLAYING, style)

    var controls: ControlScheme = PlayControls(renderer)

    init {
        add(renderer)
        add(controls)
        renderer.controlScheme = controls
    }

}