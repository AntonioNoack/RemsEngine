package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.engine.WindowRenderFlags
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.debug.TestEngine.Companion.testUI2
import me.anno.ui.input.FloatInput
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.IntVectorInput
import me.anno.ui.input.NumberType
import me.anno.ui.input.NumberType.Companion.SCALE
import me.anno.ui.input.NumberType.Companion.VEC3D
import me.anno.ui.input.TextInput

fun main() {
    // arrow keys were broken because of class names / action manager
    disableRenderDoc()
    val ti = TextInput(style).setValue("103212", false) // works
    val fi = FloatInput(NameDesc("Float"), "", 103212f, NumberType.DOUBLE, style) // broken
    val ii = IntInput(NameDesc("Int"), "", 103212, style) // broken
    val fvi = FloatVectorInput(NameDesc("Float Vector"), "", SCALE, style)
    val ivi = IntVectorInput(NameDesc("Int Vector"), "", VEC3D, style)
    testUI2("Text Input") {
        WindowRenderFlags.enableVSync = true
        listOf(ti, fi, ii, fvi, ivi)
    }
}