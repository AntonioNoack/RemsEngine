package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.Panel
import me.anno.ui.debug.PureTestStudio.Companion.testPureUI

fun main() {
    GFXBase.disableRenderDoc()
    testPureUI(Panel(style))
}