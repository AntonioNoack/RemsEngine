package me.anno.tests.shader

import me.anno.config.DefaultStyle
import me.anno.gpu.drawing.DrawCurves
import me.anno.ui.debug.TestDrawPanel

fun main() {
    TestDrawPanel.testDrawing {
        it.drawBackground(it.x, it.y, it.x + it.w, it.y + it.h)
        val s = 300f
        val dx = (it.w - s) / 2f
        val dy = (it.h - s) / 2f
        val bg = it.backgroundColor
        DrawCurves.drawCubicBezier(
            dx, dy,
            dx + s, dy,
            dx, dy + s,
            dx + s, dy + s,
            10f,
            -1, bg,
            false
        )
        DrawCurves.drawQuadraticBezier(
            dx, dy,
            dx + s, dy,
            dx + s, dy + s,
            5f,
            0x777777 or DefaultStyle.black,
            0x777777,
            false
        )
        DrawCurves.drawLine(
            dx, dy,
            dx + s, dy + s,
            5f,
            0xff0000 or DefaultStyle.black,
            0xff0000,
            false
        )
    }
}