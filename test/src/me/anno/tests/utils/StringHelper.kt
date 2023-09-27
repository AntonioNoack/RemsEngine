package me.anno.tests.utils

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.strings.StringHelper.smallCaps

fun main() {
    println("polyGeneLubricants".camelCaseToTitle())
    println("polyGeneLubricants".smallCaps())
    GFXBase.disableRenderDoc()
    testUI(
        "SmallCaps Rendering",
        listOf(
            TextPanel("polyGeneLubricants", style),
            TextPanel("polyGeneLubricants".smallCaps(), style)
        )
    )
}
