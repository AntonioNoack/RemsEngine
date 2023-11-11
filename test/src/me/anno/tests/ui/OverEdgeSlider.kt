package me.anno.tests.ui

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.FloatInput

fun main() {
    disableRenderDoc()
    testUI("Number") {
        FloatInput("Number", 0f, Type.FLOAT, style)
    }
}