package me.anno.ui.debug

import me.anno.config.DefaultConfig.style
import me.anno.input.Input
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI
import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.GLFW_KEY_V

/**
 * panel to test drawing functions
 * */
open class TestDrawPanel(val draw: (p: TestDrawPanel) -> Unit) : Panel(style) {

    override fun onUpdate() {
        super.onUpdate()
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

    override val canDrawOverBorders = true

    var mx = 0f
    var my = 0f
    var mz = 0f

    var allowLeft = false
    var allowRight = true

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if ((allowLeft && Input.isLeftDown) || (allowRight && Input.isRightDown)) {
            mx += dx
            my += dy
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        mz += dy
    }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        if (key == GLFW_KEY_V && Input.isControlDown) {
            StudioBase.instance?.toggleVsync()
        } else super.onKeyDown(x, y, key)
    }

    companion object {
        fun testDrawing(draw: (p: TestDrawPanel) -> Unit) {
            testUI { TestDrawPanel(draw) }
        }
    }
}