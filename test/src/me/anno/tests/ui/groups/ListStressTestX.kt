package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.engine.EngineBase
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.Color.toHexString

fun main() {
    disableRenderDoc()
    testUI("ListStressTest-X") {
        EngineBase.enableVSync = false
        val n = 100_000
        val list = PanelListX(style)
        list.allChildrenHaveSameSize = true
        for (i in 0 until n) list.add(TextPanel("Test-${i.toHexString()}", style))
        ScrollPanelX(list, style)
    }
}