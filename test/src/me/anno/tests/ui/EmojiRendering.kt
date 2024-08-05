package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.PureTestEngine.Companion.testPureUI

fun main() {
    disableRenderDoc()
    testPureUI("Emojis", TextPanel("😊", style))
}