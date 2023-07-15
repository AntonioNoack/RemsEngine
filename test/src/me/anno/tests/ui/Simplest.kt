package me.anno.tests.ui

import me.anno.ui.debug.PureTestStudio.Companion.testPureUI
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.Color.withAlpha

fun main() {
    testPureUI {
        TestDrawPanel {
            it.backgroundRadius = 50f
            it.backgroundOutlineColor = (-1).withAlpha(0.7f)
            it.backgroundOutlineThickness = 3f
            it.clear()
        }
    }
}