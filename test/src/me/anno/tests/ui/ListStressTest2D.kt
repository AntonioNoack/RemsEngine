package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.studio.StudioBase
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    testUI {
        StudioBase.instance?.enableVSync = false
        val n = 1_000_000
        val list = PanelList2D(style)
        // todo scrollbar jumps up and down... and it covers the top bar
        list.childWidth = 120
        list.childHeight = 24
        for (i in 0 until n) {
            val p = TextPanel("Test-$i", style)
            // todo alignment is not working
            p.alignmentX = AxisAlignment.CENTER
            p.alignmentY = AxisAlignment.CENTER
            list.add(p)
        }
        list.setWeight(1f)
    }
}