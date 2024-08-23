package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.debug.TrackingPanel
import me.anno.ui.input.FloatInput
import me.anno.ui.input.NumberType
import me.anno.utils.Color.white

fun main() {
    disableRenderDoc()
    var value = 0.0
    testUI(
        "Number", listOf(
            FloatInput(NameDesc("Number"), 0f, NumberType.FLOAT, style).setChangeListener { value = it },
            TrackingPanel(listOf { value }, intArrayOf(white), style).fill(1f)
        )
    )
}