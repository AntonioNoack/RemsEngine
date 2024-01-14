package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.Panel
import me.anno.ui.debug.PureTestEngine.Companion.testPureUI

fun main() {
    disableRenderDoc()
    testPureUI("FPSPanelSpeed", Panel(style))
}