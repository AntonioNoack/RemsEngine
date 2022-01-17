package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D.defineAdvancedGraphicalFeatures
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.*
import me.anno.objects.modes.UVProjection
import me.anno.video.formats.gpu.GPUFrame
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.joml.Vector4fc

object DrawTextures {

    fun drawProjection(
        x: Int, y: Int, w: Int, h: Int,
        texture: CubemapTexture, ignoreAlpha: Boolean, color: Int
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderCubemap.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", color)
        shader.v1b("ignoreTexAlpha", ignoreAlpha)
        texture.bind(0, texture.filtering, Clamping.CLAMP)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawTexturePure(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean,
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderTexture.value
        shader.use()
        val posX = (x - GFX.viewportX).toFloat() / GFX.viewportWidth
        val posY = (y - GFX.viewportY).toFloat() / GFX.viewportHeight
        val relW = +w.toFloat() / GFX.viewportWidth
        val relH = +h.toFloat() / GFX.viewportHeight
        shader.v2f("pos", posX, posY)
        shader.v2f("size", relW, relH)
        defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", -1)
        shader.v1b("ignoreTexAlpha", ignoreAlpha)
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
        val tex = texture as? Texture2D
        texture.bind(
            0,
            tex?.filtering ?: GPUFiltering.NEAREST,
            tex?.clamping ?: Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean, color: Int, tiling: Vector4fc?
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderTexture.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", color)
        shader.v1b("ignoreTexAlpha", ignoreAlpha)
        if (tiling != null) shader.v4f("tiling", tiling)
        else shader.v4f("tiling", 1f, 1f, 0f, 0f)
        val tex = texture as? Texture2D
        texture.bind(
            0,
            tex?.filtering ?: GPUFiltering.NEAREST,
            tex?.clamping ?: Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(x: Int, y: Int, w: Int, h: Int, texture: ITexture2D, color: Int, tiling: Vector4fc?) {
        drawTexture(x, y, w, h, texture, false, color, tiling)
    }

    private val tiling = Vector4f()
    fun drawTransparentBackground(x: Int, y: Int, w: Int, h: Int, numVerticalStripes: Float = 5f) {
        tiling.set(numVerticalStripes * w.toFloat() / h.toFloat(), numVerticalStripes, 0f, 0f)
        drawTexture(x, y, w, h, TextureLib.colorShowTexture, -1, tiling)
    }

    fun drawTexture(matrix: Matrix4fArrayList, w: Int, h: Int, texture: Texture2D, color: Int, tiling: Vector4fc?) {
        matrix.scale(w.toFloat() / GFX.viewportWidth, h.toFloat() / GFX.viewportHeight, 1f)
        GFXx3D.draw3D(
            matrix, texture, color,
            Filtering.LINEAR, Clamping.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawTexture(w: Int, h: Int, texture: GPUFrame, color: Int, tiling: Vector4fc?) {
        val matrix = Matrix4fArrayList()
        matrix.scale(w.toFloat() / GFX.viewportWidth, h.toFloat() / GFX.viewportHeight, 1f)
        GFXx3D.draw3D(
            matrix, texture, color,
            Filtering.LINEAR, Clamping.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawTexture(texture: GPUFrame) {

        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().value

        GFX.check()

        shader.use()
        defineAdvancedGraphicalFeatures(shader)
        GFXx3D.shader3DUniforms(shader, null, -1)

        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        texture.bindUVCorrection(shader)

        UVProjection.Planar.getBuffer().draw(shader)
        GFX.check()

    }

}