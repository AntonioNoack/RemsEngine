package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.ColorInput

fun main() {
    disableRenderDoc()
    testUI("Color Input", ColorInput(style))
}