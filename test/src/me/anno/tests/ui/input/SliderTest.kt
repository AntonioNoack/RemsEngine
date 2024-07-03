package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.SliderInput

fun main() {
    disableRenderDoc()
    testUI3("Slide", SliderInput(0.0, 10.0, 1.0, 3.0, "Test", "x", style)
        .setChangeListener { println("New Value: $it") })
}