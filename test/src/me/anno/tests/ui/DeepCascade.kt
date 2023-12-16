package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI3

// create a deep cascade of panels to find eventual slow downs
fun main() {
    testUI3("Deep Cascade", createX(0, 10))
}

fun createX(id: Long, depth: Int): Panel {
    return finish(
        if (depth > 0) {
            val list = PanelListX(style)
            for (i in 0 until 2) {
                list.add(createY(id * 2 + i, depth - 1))
            }
            list
        } else create(id)
    )
}

fun finish(panel: Panel): Panel {
    panel.alignmentX = AxisAlignment.FILL
    panel.alignmentY = AxisAlignment.FILL
    panel.weight = 1f
    return panel
}

fun createY(id: Long, depth: Int): Panel {
    return finish(
        if (depth > 0) {
            val list = PanelListY(style)
            for (i in 0 until 2) {
                list.add(createX(id * 2 + i, depth - 1))
            }
            list
        } else create(id)
    )
}

fun create(id: Long): Panel {
    val panel = TextPanel(id.toString(16), style)
    panel.makeBackgroundTransparent()
    return panel
}