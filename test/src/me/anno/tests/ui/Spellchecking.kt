package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.TextInput

fun main() {
    disableRenderDoc()
    // the rest after "gonna" was cut off -> is fixed now :)
    val panel = TextInput("Test", "", "This is gonna be an upgrade", style)
    testUI("Spellchecking", panel)
}