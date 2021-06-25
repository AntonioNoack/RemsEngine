package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import org.joml.Vector4fc

object DrawRectangles {

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Vector4fc) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val shader = ShaderLib.flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        GFX.flat01.draw(shader)
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Vector4fc) {
        GFX.check()
        val shader = ShaderLib.flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        GFX.check()
        val shader = ShaderLib.flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

}