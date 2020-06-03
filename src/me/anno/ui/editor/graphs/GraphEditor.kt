package me.anno.ui.editor.graphs

import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

class GraphEditor(style: Style): PanelListY(style) {

    val title = TextPanel("Timeline", style)
    val body = GraphEditorBody(style)

    init {
        this += title
        this += body.setWeight(1f)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
    }

}