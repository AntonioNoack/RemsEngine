package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.studio.StudioBase
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    testUI("ListStressTest-Y") {
        StudioBase.instance?.enableVSync = false
        val n = 100_000
        val list = PanelListY(style)
        list.allChildrenHaveSameSize = true
        for (i in 0 until n) list.add(TextPanel("Test-$i", style))
        ScrollPanelY(list, style)
    }
}