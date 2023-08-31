package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.FloatInput

fun main() {
    // enter was broken, and no longer calculating value
    GFXBase.disableRenderDoc()
    val fi = FloatInput(style)
    // fi.isInputAllowed = false
    fi.isEnabled = false
    testUI("Float Input", fi)
}