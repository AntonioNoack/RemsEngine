package me.anno.tests.gfx

import me.anno.Time
import me.anno.gpu.drawing.GFXx2D.drawCircle
import me.anno.engine.EngineBase
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.black
import org.joml.Vector2f
import kotlin.math.max

fun main() {

    // input lag (here): difference between Windows cursor and drawn geometry

    // results:
    // call glFinish() in Windows after glfwSwapBuffer() (1 frame less lag)
    // use full screen (1 frame less lag; seems to be the fault of the desktop window manager)

    val previous = ArrayList<Vector2f>()
    val colors = intArrayOf(
        0x00ff00, 0xffff00, 0xff0000,
        0x000000, 0xffffff,
        /*0x00ff00, 0xffff00, 0xff0000,
        0x000000, 0xffffff,
        0x00ff00, 0xffff00, 0xff0000,
        0x000000, 0xffffff,
        0x00ff00, 0xffff00, 0xff0000,
        0x000000, 0xffffff,
        0x00ff00, 0xffff00, 0xff0000,
        0x000000, 0xffffff,*/
    )
    for (i in colors.indices) {
        previous.add(Vector2f())
    }
    val measuredFps = 60f
    val vsync = true
    val delayNanos = (0.95 * 1e9 / measuredFps).toLong()
    var lastTime = Time.nanoTime
    testDrawing("InputLag") {
        // with Vsync: 3 frames input lag (red ring)
        // without vsync (at 2000 fps): still 33ms lag (yellow ring)
        // without vsync, with glFinish() after glfwSwapBuffers(): 16-25ms lag (between green and yellow)
        // with Vsync and glFinish(): 2 frames input lag (yellow ring)
        EngineBase.enableVSync = vsync
        it.clear()
        val window = it.window!!
        val mx = window.mouseX
        val my = window.mouseY
        for (i in previous.indices) {
            val v = previous[i]
            val r = max(v.distance(mx, my), 1f)
            drawCircle(
                mx.toInt(), my.toInt(), r, r, 1f - 2f / r,
                0f, 360f, colors[i] or black
            )
        }
        val time = Time.nanoTime
        if (vsync || time >= lastTime + delayNanos) {
            lastTime = time
            val pos = previous.removeLast()
            previous.add(0, pos.set(mx, my))
        }
    }
}