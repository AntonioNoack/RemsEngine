package me.anno.tests.ui

import me.anno.ui.debug.PureTestEngine.Companion.testPureUI
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha

fun main() {
    testPureUI("Simplest") {
        TestDrawPanel {
            it.background.radius = 50f
            it.background.outlineColor = white.withAlpha(0.7f)
            it.background.outlineThickness = 3f
            it.clear()
        }
    }
}