package me.anno.ui.base.components

import me.anno.gpu.GFX
import me.anno.gpu.GFX.a
import me.anno.gpu.GFX.b
import me.anno.gpu.GFX.g
import me.anno.gpu.GFX.r
import me.anno.gpu.GFXx2D
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.ShaderLib
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Corner {

    private fun corner(mx: Boolean, my: Boolean): StaticBuffer {
        val sides = 10
        val buffer = StaticBuffer(cornerAttr, 3 * sides)
        fun put(x: Float, y: Float) {
            buffer.put(if (mx) 1 - x else x, if (my) 1 - y else y)
        }
        for (i in 0 until sides) {
            val a0 = (i * PI / sides).toFloat()
            val a1 = ((i + 1) * PI / sides).toFloat()
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
        val shader = ShaderLib.flatShader
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        corner.draw(shader)
        GFX.check()
    }

    fun drawCorner(x: Int, y: Int, w: Int, h: Int, color: Int, corner: StaticBuffer) {
        if (w == 0 || h == 0 || color.a() <= 0f) return
        GFX.check()
        val shader = ShaderLib.flatShader
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("color", color)
        corner.draw(shader)
        GFX.check()
    }

}