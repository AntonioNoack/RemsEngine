package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.ColorInput

fun main() {
    GFXBase.disableRenderDoc()
    testUI("Color Input", ColorInput(style))
}