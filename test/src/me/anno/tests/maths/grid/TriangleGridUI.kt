package me.anno.tests.maths.grid

import me.anno.ecs.components.chunks.triangles.TriangleGridMaths.coordsToIndex
import me.anno.ecs.components.chunks.triangles.TriangleGridMaths.getClosestLine
import me.anno.ecs.components.chunks.triangles.TriangleGridMaths.getClosestVertex
import me.anno.ecs.components.chunks.triangles.TriangleGridMaths.getVertex
import me.anno.ecs.components.chunks.triangles.TriangleGridMaths.indexToCenter
import me.anno.ecs.components.chunks.triangles.TriangleGridMaths.indexToCoords
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawRectangles
import me.anno.input.Input
import me.anno.maths.Maths.mix
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import org.joml.Vector2d
import org.joml.Vector2i

/**
 * create a UI to test the triangle grid visually 😄, and to show how it can be used 😊
 *
 * drag with your right mouse button pressed to move around
 * */
fun main() {
    TestDrawPanel.testDrawing("Triangle Grid") {
        it.clear()

        val dy = 10
        val dx = it.width * 2 * dy / it.height
        val scale = it.height.toDouble() * 0.6 / dy

        val cx = it.x + it.width / 2 + it.mx.toInt()
        val cy = it.y + it.height / 2 + it.my.toInt()

        val window = it.window!!
        val mouseCoords = Vector2d((window.mouseX - cx) / scale, (window.mouseY - cy) / scale)
        val hovCell = coordsToIndex(mouseCoords, Vector2i(), Vector2d(), false)
        val hovLine = getClosestLine(mouseCoords, Vector2d(), Vector2d(), Vector2i())
        val hovVert = getClosestVertex(mouseCoords, Input.isShiftDown, Vector2d(), Vector2d(), Vector2i())

        // cannot be mixed yet
        /*val lineBatch = DrawCurves.lineBatch.start()
        val rectBatch = DrawRectangles.batch.start()*/

        val tmp = Vector2d()
        val tmp1 = Vector2d()
        val tmp2 = Vector2d()
        for (j in -dy..dy) {
            for (i in -dx..dx) {
                val isHovered = hovCell.x == i && hovCell.y == j
                val color = if (isHovered && hovVert == 3) {
                    0x00ff00.withAlpha(255)
                } else {
                    white.withAlpha(if (isHovered) 255 else 100)
                }
                val c = indexToCoords(i, j, tmp).mul(scale)
                val c2 = indexToCenter(i, j, tmp1).mul(scale)
                val x = c.x
                val y = c.y
                if (Input.isControlDown) {
                    DrawRectangles.drawRect(c2.x.toInt() + cx - 1, c2.y.toInt() + cy - 1, 3, 3, color)
                } else {
                    DrawRectangles.drawRect(x.toInt() + cx - 1, y.toInt() + cy - 1, 3, 3, color)
                }
                val a = getVertex(i, j, 0, tmp1).mul(scale)
                for (k in 0 until 3) {

                    val b = getVertex(i, j, (k + 1) % 3, tmp2).mul(scale)
                    val f = 0.05
                    val color1 = white.withAlpha(if (isHovered && k == hovLine) 255 else 100)
                    DrawCurves.drawLine(
                        mix(a.x, x, f).toFloat() + cx,
                        mix(a.y, y, f).toFloat() + cy,
                        mix(b.x, x, f).toFloat() + cx,
                        mix(b.y, y, f).toFloat() + cy,
                        1f, color1, it.backgroundColor.withAlpha(0),
                        false
                    )

                    if (isHovered) {
                        val color2 = (if (k == hovVert) 0x00ff00 else 0x777777).withAlpha(255)
                        DrawRectangles.drawRect(a.x.toInt() + cx - 3, a.y.toInt() + cy - 3, 7, 7, color2)
                    }

                    a.set(b)
                }
            }
        }

        /*DrawCurves.lineBatch.finish(lineBatch)
        DrawRectangles.batch.finish(rectBatch)*/
    }
}
