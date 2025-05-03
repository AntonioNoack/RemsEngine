package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.input.ActionManager
import me.anno.ui.Panel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.black
import me.anno.utils.assertions.assertTrue

class TestPressingPanel : Panel(style) {

    var pressed = false

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        background.color = if (pressed) black else background.originalColor
        super.draw(x0, y0, x1, y1)
        pressed = false
    }

    override fun onGotAction(
        x: Float, y: Float, dx: Float, dy: Float,
        action: String, isContinuous: Boolean
    ): Boolean {
        // this was broken
        pressed = true
        return true
    }
}

/**
 * Key-press events were broken when the mouse isn't moving.
 * */
fun main() {
    val panel = TestPressingPanel()
    assertTrue(ActionManager.register("${panel.className}.w.p", "Pressing"))
    testUI3("Test Pressing", panel)
}