package me.anno.ui.debug

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.clamp
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f

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
        drawBackground(x, y, x + width, y + height)
    }

    override val canDrawOverBorders get() = true

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

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.KEY_V && Input.isControlDown) {
            StudioBase.instance?.toggleVsync()
        } else super.onKeyDown(x, y, key)
    }

    companion object {

        @JvmStatic
        fun testDrawing(title: String, draw: (p: TestDrawPanel) -> Unit) {
            testUI3(title) { TestDrawPanel(draw) }
        }

        @JvmStatic
        fun testDrawing(title: String, init: (p: TestDrawPanel) -> Unit, draw: (p: TestDrawPanel) -> Unit) {
            testUI3(title) {
                val panel = TestDrawPanel(draw)
                init(panel)
                panel
            }
        }

        @JvmStatic
        fun testDrawingWithControls(title: String, draw: (p: TestDrawPanel, pos: Vector3f, rot: Quaternionf) -> Unit) {
            val cameraPosition = Vector3f(0f, 2f, -3f)
            val cameraRotation = Quaternionf()
            val accumulatedRotation = Vector2f()
            val velocity = Vector3f()
            testDrawing(title) {
                val scale = 5f / it.height
                accumulatedRotation.add(it.mx * scale, it.my * scale)
                accumulatedRotation.y = clamp(accumulatedRotation.y, -1.57f, +1.57f)
                it.mx = 0f
                it.my = 0f
                cameraRotation.identity()
                    .rotateY(accumulatedRotation.x)
                    .rotateX(accumulatedRotation.y)
                velocity.set(0f)
                if (Input.isKeyDown(Key.KEY_W)) velocity.z += 1f
                if (Input.isKeyDown(Key.KEY_S)) velocity.z -= 1f
                if (Input.isKeyDown(Key.KEY_A)) velocity.x -= 1f
                if (Input.isKeyDown(Key.KEY_D)) velocity.x += 1f
                if (Input.isKeyDown(Key.KEY_Q)) velocity.y -= 1f
                if (Input.isKeyDown(Key.KEY_E)) velocity.y += 1f
                cameraPosition.add(velocity.mul(3f * Time.deltaTime.toFloat()).rotate(cameraRotation))
                draw(it, cameraPosition, cameraRotation)
            }
        }
    }
}