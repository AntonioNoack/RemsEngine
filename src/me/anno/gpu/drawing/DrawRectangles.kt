package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.shader.FlatShaders.flatShader
import me.anno.utils.Color.a
import org.joml.Vector4f

object DrawRectangles {

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Vector4f) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val shader = flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        GFX.flat01.draw(shader)
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Vector4f) {
        GFX.check()
        val shader = flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        GFX.check()
        val shader = flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawBorder(x: Int, y: Int, w: Int, h: Int, color: Int, thicknessX: Int = 1, thicknessY: Int = thicknessX) {
        if (color.a() == 0) return
        drawRect(x, y, w, thicknessY, color)
        drawRect(x, y + h - thicknessY, w, thicknessY, color)
        drawRect(x, y, thicknessX, h, color)
        drawRect(x + w - thicknessX, y, thicknessX, h, color)
    }

    fun drawBorder(x: Int, y: Int, w: Int, h: Int, color: Int, size: Int) {
        GFXx2D.flatColor(color)
        drawRect(x, y, w, size)
        drawRect(x, y + h - size, w, size)
        drawRect(x, y + size, size, h - 2 * size)
        drawRect(x + w - size, y + size, size, h - 2 * size)
    }


}