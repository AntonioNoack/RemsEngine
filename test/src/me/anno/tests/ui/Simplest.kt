package me.anno.tests.ui

import me.anno.ui.debug.PureTestEngine.Companion.testPureUI
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha

fun main() {
    testPureUI("Simplest") {
        TestDrawPanel { p, canvas ->
            p.background.radius = 50f
            p.background.outlineColor = white.withAlpha(0.7f)
            p.background.outlineThickness = 3f
            p.clear(canvas)
        }
    }
}