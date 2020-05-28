package me.anno.ui.impl.timeline

import me.anno.config.DefaultStyle.black
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.impl.timeline.TimelineBody
import me.anno.ui.style.Style

class Timeline(style: Style): PanelListY(style) {

    val title = TextPanel("Timeline", style)
    val body = TimelineBody(style)

    init {
        this += title
        this += body
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
    }

}