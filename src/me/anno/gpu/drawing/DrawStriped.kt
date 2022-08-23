package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.shader.FlatShaders.flatShaderStriped
import me.anno.gpu.shader.Shader
import org.joml.Vector4f

object DrawStriped {

    fun drawRectStriped(x: Int, y: Int, w: Int, h: Int, offset: Int, stride: Int, color: Vector4f) {
        if (w == 0 || h == 0) return
        val shader = flatShaderStriped.value
        shader.use()
        shader.v4f("color", color)
        drawRectStriped(x, y, w, h, offset, stride, shader)
    }

    fun drawRectStriped(x: Int, y: Int, w: Int, h: Int, offset: Int, stride: Int, color: Int) {
        if (w == 0 || h == 0) return
        val shader = flatShaderStriped.value
        shader.use()
        shader.v4f("color", color)
        drawRectStriped(x, y, w, h, offset, stride, shader)
    }

    fun drawRectStriped(x: Int, y: Int, w: Int, h: Int, offset: Int, stride: Int, shader: Shader) {
        if (w == 0 || h == 0) return
        GFX.check()
        GFXx2D.posSize(shader, x, y, w, h)
        var o = offset % stride
        if (o < 0) o += stride
        shader.v1i("offset", o)
        shader.v1i("stride", stride)
        GFX.flat01.draw(shader)
        GFX.check()
    }

}