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
import me.anno.ui.input.TextInputML

fun main() {
    // arrow keys were broken because of class names / action manager
    disableRenderDoc()
    val textInput = TextInput(style).setValue("103212", false) // works
    val floatInput = FloatInput(NameDesc("Float"), "", 103212f, NumberType.DOUBLE, style) // broken
    val intInput = IntInput(NameDesc("Int"), "", 103212, style) // broken
    val floatVectorInput = FloatVectorInput(NameDesc("Float Vector"), "", SCALE, style)
    val intVectorInput = IntVectorInput(NameDesc("Int Vector"), "", VEC3D, style)

    // todo bug: text selection looks weird over multiple lines
    // todo bug: up/down arrow keys don't work
    val textInputML = TextInputML(style).setValue("a\nb", false)

    testUI2("Text Input") {
        WindowRenderFlags.enableVSync = true
        listOf(textInput, floatInput, intInput, floatVectorInput, intVectorInput, textInputML)
    }
}