package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.ShaderLib.shader3D
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.*
import me.anno.objects.GFXTransform
import me.anno.objects.modes.UVProjection
import me.anno.video.VFrame
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc

object DrawTextures {

    fun drawTexture(x: Int, y: Int, w: Int, h: Int, texture: ITexture2D, color: Int, tiling: Vector4fc?) {
        GFX.check()
        val shader = ShaderLib.flatShaderTexture.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4("color", color)
        if (tiling != null) shader.v4("tiling", tiling)
        else shader.v4("tiling", 1f, 1f, 0f, 0f)
        val tex = texture as? Texture2D
        texture.bind(
            0,
            tex?.filtering ?: GPUFiltering.NEAREST,
            tex?.clamping ?: Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(matrix: Matrix4fArrayList, w: Int, h: Int, texture: Texture2D, color: Int, tiling: Vector4fc?) {
        matrix.scale(w.toFloat() / GFX.windowWidth, h.toFloat() / GFX.windowHeight, 1f)
        GFXx3D.draw3D(
            matrix, texture, color,
            Filtering.LINEAR, Clamping.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawTexture(w: Int, h: Int, texture: VFrame, color: Int, tiling: Vector4fc?) {
        val matrix = Matrix4fArrayList()
        matrix.scale(w.toFloat() / GFX.windowWidth, h.toFloat() / GFX.windowHeight, 1f)
        GFXTransform.uploadAttractors0(texture.get3DShader().value)
        GFXx3D.draw3D(
            matrix, texture, color,
            Filtering.LINEAR, Clamping.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawTexture(texture: VFrame) {

        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().value

        GFX.check()

        shader.use()
        GFXTransform.uploadAttractors0(shader)
        GFXx3D.shader3DUniforms(shader, null, -1)
        /*shader.v1("filtering", Filtering.LINEAR.id)
        shader.v2("textureDeltaUV", 1f / texture.w, 1f / texture.h)
        shader.m4x4("transform", null)
        shader.v4("tint", 1f)
        shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v1("uvProjection", UVProjection.Planar.id)*/
        // GFXx2D.disableAdvancedGraphicalFeatures(shader)

        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        texture.bindUVCorrection(shader)

        UVProjection.Planar.getBuffer().draw(shader)
        GFX.check()

    }

}