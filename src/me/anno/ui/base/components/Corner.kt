package me.anno.ui.base.components

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.ShaderLib
import me.anno.utils.Color.a
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Corner {

    private fun corner(mx: Boolean, my: Boolean): StaticBuffer {

        val sides = 50
        val buffer = StaticBuffer(cornerAttr, 3 * sides)
        fun put(x: Float, y: Float) {
            buffer.put(if (mx) 1 - x else x, if (my) 1 - y else y)
        }

        val angle = (PI * 0.5).toFloat()
        for (i in 0 until sides) {
            val a0 = i * angle / sides
            val a1 = (i + 1) * angle / sides
            put(0f, 0f)
            put(cos(a0), sin(a0))
            put(cos(a1), sin(a1))
        }

        return buffer

    }

    private val cornerAttr = listOf(Attribute("attr0", 2))
    val topLeft = corner(true, my = true)
    val topRight = corner(false, my = true)
    val bottomLeft = corner(true, my = false)
    val bottomRight = corner(false, my = false)

    fun drawRoundedRect(
        x: Int, y: Int, w: Int, h: Int,
        radiusX: Int, radiusY: Int,
        color: Int,
        topLeft: Boolean, topRight: Boolean, bottomLeft: Boolean, bottomRight: Boolean
    ) {
        if (w > 0 && h > 0) {
            if (radiusX > 0 && radiusY > 0 && (topLeft || topRight || bottomLeft || bottomRight)) {

                // val bottomFree = !bottomLeft && !bottomRight
                // val topFree = !topLeft && !topRight
                // val leftFree = !topLeft && !bottomLeft
                // val rightFree = !topRight && !bottomRight

                // todo optimize to use less draw calls if 1 or 2 corners are drawn only

                GFXx2D.flatColor(color)

                // draw center part
                drawRect(x, y + radiusY, w, h - radiusY * 2)

                // draw top bar
                drawRect(x + radiusX, y, w - 2 * radiusX, radiusY)
                // draw bottom bar
                drawRect(x + radiusX, y + h - radiusY, w - 2 * radiusX, radiusY)

                // draw corners
                if (topLeft) drawCorner(x, y, radiusX, radiusY, this.topLeft)
                else drawRect(x, y, radiusX, radiusY)

                if (topRight) drawCorner(x + w - radiusX, y, radiusX, radiusY, this.topRight)
                else drawRect(x + w - radiusX, y, radiusX, radiusY)

                if (bottomLeft) drawCorner(x, y + h - radiusY, radiusX, radiusY, this.bottomLeft)
                else drawRect(x, y + h - radiusY, radiusX, radiusY)

                if (bottomRight) drawCorner(x + w - radiusX, y + h - radiusY, radiusX, radiusY, this.bottomRight)
                else drawRect(x + w - radiusX, y + h - radiusY, radiusX, radiusY)

            } else drawRect(x, y, w, h, color)
        }
    }

    fun drawCorner(x: Int, y: Int, w: Int, h: Int, corner: StaticBuffer) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        corner.draw(shader)
        GFX.check()
    }

    fun drawCorner(x: Int, y: Int, w: Int, h: Int, color: Int, corner: StaticBuffer) {
        if (w == 0 || h == 0 || color.a() <= 0f) return
        GFX.check()
        val shader = ShaderLib.flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        corner.draw(shader)
        GFX.check()
    }

}