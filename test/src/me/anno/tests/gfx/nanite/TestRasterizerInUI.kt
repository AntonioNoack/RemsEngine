package me.anno.tests.gfx.nanite

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.maths.geometry.Rasterizer
import me.anno.tests.shader.drawMovablePoints
import me.anno.utils.Color
import org.joml.AABBf

fun main() {
    testRasterizerAlgorithm()
}

fun testRasterizerAlgorithm() {
    val bounds = AABBf()
    drawMovablePoints("CPU Rasterizer", 3) { panel, points ->
        bounds
            .setMin(panel.x.toFloat(), panel.y.toFloat(), 0f)
            .setMax(panel.x + panel.width - 1f, panel.y + panel.height - 1f, 0f)
        Rasterizer.rasterize(points[0], points[1], points[2], bounds) { minX1, maxX1, y ->
            drawRect(minX1, y, maxX1 - minX1 + 1, 1, -1)
        }
        for (i in 0 until 3) {
            val c3 = 0x00ff00 or Color.black
            val scale = panel.scale.y
            val r = (3f * scale.toFloat()).toInt()
            drawRect(points[i].x.toInt(), points[i].y.toInt(), r, r, c3)
        }
    }
}
