package me.anno.ui.debug

import me.anno.config.DefaultConfig.style
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI

class TestDrawPanel(val draw: (p: Panel) -> Unit) : Panel(style) {
    override fun tickUpdate() {
        invalidateDrawing()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        draw(this)
    }

    init {
        setWeight(1f)
    }

    companion object {
        fun testDrawing(draw: (p: Panel) -> Unit) {
            testUI { TestDrawPanel(draw) }
        }
    }
}