package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.FloatInput

fun main() {
    // enter was broken, and no longer calculating value
    disableRenderDoc()
    val pl = PanelList2D(style)
    for (i in 0 until 50) {
        pl.add(FloatInput(style))
    }
    // fi.isInputAllowed = false
    // fi.isEnabled = false
    testUI("Float Input", pl)
}