package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.TextureLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.video.VFrame
import org.joml.Vector4fc

object DrawGradients {

    fun drawRectGradient(x: Int, y: Int, w: Int, h: Int, leftColor: Vector4fc, rightColor: Vector4fc) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderGradient.value
        shader.use()
        TextureLib.whiteTexture.bind(0, TextureLib.whiteTexture.filtering, TextureLib.whiteTexture.clamping)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("lColor", leftColor)
        shader.v4("rColor", rightColor)
        shader.v1("code", -1)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRectGradient(x: Int, y: Int, w: Int, h: Int, leftColor: Int, rightColor: Int) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderGradient.value
        shader.use()
        TextureLib.whiteTexture.bind(0, TextureLib.whiteTexture.filtering, TextureLib.whiteTexture.clamping)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("lColor", leftColor)
        shader.v4("rColor", rightColor)
        shader.v1("code", -1)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int, leftColor: Vector4fc, rightColor: Vector4fc,
        frame: VFrame, uvs: Vector4fc
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderGradient.value
        shader.use()
        frame.bind(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("lColor", leftColor)
        shader.v4("rColor", rightColor)
        shader.v4("uvs", uvs)
        shader.v1("code", frame.code)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int, leftColor: Int, rightColor: Int,
        frame: VFrame, uvs: Vector4fc
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderGradient.value
        shader.use()
        frame.bind(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("lColor", leftColor)
        shader.v4("rColor", rightColor)
        shader.v4("uvs", uvs)
        shader.v1("code", frame.code)
        GFX.flat01.draw(shader)
        GFX.check()
    }

}