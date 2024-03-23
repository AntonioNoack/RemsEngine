package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineBase
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    testUI3("List Stresstest 2d") {
        EngineBase.enableVSync = false
        val n = 100_000
        val list = PanelList2D(style)
        list.childWidth = 120
        list.childHeight = 24
        for (i in 0 until n) {
            val p = TextPanel("Test-$i", style)
            p.textAlignmentX = AxisAlignment.CENTER
            p.textAlignmentY = AxisAlignment.CENTER
            p.alignmentX = AxisAlignment.CENTER
            p.alignmentY = AxisAlignment.CENTER
            list.add(p)
        }
        ScrollPanelY(list, style)
    }
}