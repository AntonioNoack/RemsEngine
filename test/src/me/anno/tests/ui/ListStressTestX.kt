package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.studio.StudioBase
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    testUI("List Stresstest X") {
        StudioBase.instance?.enableVSync = false
        val n = 100_000
        val list = PanelListX(style)
        list.allChildrenHaveSameSize = true
        for (i in 0 until n) list.add(TextPanel("Test-$i", style))
        ScrollPanelX(list, style)
    }
}