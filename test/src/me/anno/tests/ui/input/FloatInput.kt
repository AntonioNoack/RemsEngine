package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.input.FloatInput

fun main() {
    // enter was broken, and no longer calculating value
    disableRenderDoc()
    val pl = PanelList2D(style)
    repeat(50) {
        pl.add(FloatInput(style))
    }
    // fi.isInputAllowed = false
    // fi.isEnabled = false
    testUI("Float Input", pl)
}