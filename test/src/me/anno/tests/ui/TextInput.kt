package me.anno.tests.ui

import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.gpu.GFXBase
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestStudio.Companion.testUI2
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.components.PureTextInput

fun main() {
    // arrow keys were broken because of class names / action manager
    GFXBase.disableRenderDoc()
    val ti = PureTextInput(DefaultConfig.style).setValue("103212", false) // works
    val fi = FloatInput("", "", 103212f, Type.DOUBLE, DefaultConfig.style) // broken
    val ii = IntInput("", "", 103212, DefaultConfig.style) // broken
    testUI2("Text Input") {
        StudioBase.instance!!.enableVSync = true
        listOf(ti, fi, ii)
    }
}