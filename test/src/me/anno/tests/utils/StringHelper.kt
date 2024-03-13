package me.anno.tests.utils

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.smallCaps

fun main() {
    val test = "polyGeneLubricants"
    println(test.camelCaseToTitle())
    println(test.smallCaps())
    disableRenderDoc()
    testUI(
        "SmallCaps Rendering",
        listOf(
            TextPanel(test, style),
            TextPanel(test.smallCaps(), style)
        )
    )
}
