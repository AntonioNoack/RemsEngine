package me.anno.tests

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.strings.StringHelper.distance
import me.anno.utils.strings.StringHelper.smallCaps

fun main() {
    println("abc".distance("abcdef"))
    println("abcdef".distance("abc"))
    println("bcd".distance("abc"))
    println("helloworldbcd".distance("halloweltabc"))
    println("polyGeneLubricants".camelCaseToTitle())
    GFXBase.disableRenderDoc()
    println("polyGeneLubricants".smallCaps())
    testUI(listOf(
        TextPanel("polyGeneLubricants", style),
        TextPanel("polyGeneLubricants".smallCaps(), style)
    ))
}
