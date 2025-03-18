package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.engine.WindowRenderFlags
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI

fun main() {
    testUI("ListStressTest-XY") {
        WindowRenderFlags.enableVSync = false
        val listY = PanelListY(style)
        for (j in 0 until 100) {
            val list = PanelListX(style)
            list.allChildrenHaveSameSize = true
            for (i in 0 until 100) {
                list.add(TextPanel("Test-$i/$j", style))
            }
            listY.add(list)
        }
        ScrollPanelXY(listY, style)
    }
}