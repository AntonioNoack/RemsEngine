package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.OptionBar

fun main() {
    // todo find out why menu is invisible in engine and studio...
    testUI("Menu") {
        val bar = OptionBar(style)
        bar.addMajor("Test") {}
        bar
    }
}