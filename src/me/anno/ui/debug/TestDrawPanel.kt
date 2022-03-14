package me.anno.ui.debug

import me.anno.config.DefaultConfig.style
import me.anno.input.Input
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI

class TestDrawPanel(val draw: (p: TestDrawPanel) -> Unit) : Panel(style) {
    override fun tickUpdate() {
        invalidateDrawing()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        draw(this)
    }

    fun clear() {
        drawBackground(x, y, x + w, y + h)
    }

    init {
        setWeight(1f)
    }

    var mx = 0f
    var my = 0f
    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isRightDown) {
            mx += dx / h
            my += dy / h
        }
    }

    companion object {
        fun testDrawing(draw: (p: TestDrawPanel) -> Unit) {
            testUI { TestDrawPanel(draw) }
        }
    }
}