package me.anno.engine.ui.render

import me.anno.engine.ui.ECSTypeLibrary
import me.anno.engine.ui.control.BlenderControls
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.style.Style

class SceneView(val library: ECSTypeLibrary, style: Style) : PanelStack(style) {

    val renderer = RenderView(library, { library.world }, RenderView.Mode.EDITING, style)

    val controls = BlenderControls(renderer)

    init {
        add(renderer)
        add(controls)
    }

}