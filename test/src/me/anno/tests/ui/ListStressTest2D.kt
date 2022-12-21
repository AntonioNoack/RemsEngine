package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.studio.StudioBase
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI3

fun main() {
    testUI3 {
        StudioBase.instance?.enableVSync = false
        val n = 100_000
        val list = PanelList2D(style)
        // to do scrollbar jumps up and down... and it covers the top bar
        list.childWidth = 120
        list.childHeight = 24
        for (i in 0 until n) {
            val p = TextPanel("Test-$i", style)
            p.textAlignment = AxisAlignment.CENTER
            p.alignmentX = AxisAlignment.CENTER
            p.alignmentY = AxisAlignment.CENTER
            list.add(p)
        }
        list
    }
}