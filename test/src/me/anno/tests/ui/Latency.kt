package me.anno.tests.ui

import me.anno.gpu.drawing.GFXx2D.drawCircle
import me.anno.maths.Maths.length
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.black

/**
 * - move your mouse with constant speed
 * - watch, which ring your mouse is on,
 * - n-th ring = n frames latency
 * */
fun main() {
    var lx = 0f
    var ly = 0f
    testDrawing("Latency") {
        StudioBase.instance?.enableVSync = true
        it.clear()
        val window = it.window!!
        val radius1 = length(window.mouseX - lx, window.mouseY - ly)
        val colors = intArrayOf(
            0x00ff00 or black,
            0xffff00 or black,
            0xff0000 or black
        )
        for (i in colors.indices) {
            val radiusI = radius1 * (i + 1)
            drawCircle(
                window.mouseX, window.mouseY, radiusI, radiusI,
                (radiusI - 1f) / radiusI, 0f, 0f, colors[i]
            )
        }
        lx = window.mouseX
        ly = window.mouseY
    }
}