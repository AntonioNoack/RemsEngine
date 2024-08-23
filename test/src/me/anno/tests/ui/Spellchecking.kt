package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.input.TextInput

fun main() {
    disableRenderDoc()
    testUI("Spellchecking") {
        // the rest after "gonna" was cut off -> is fixed now :)
        TextInput(NameDesc("Test"), "", "This is gonna be an upgrade", style)
    }
}