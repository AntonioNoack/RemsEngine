package me.anno.ui.impl

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

}