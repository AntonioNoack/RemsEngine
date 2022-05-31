package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D.defineAdvancedGraphicalFeatures
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.FlatShaders.depthShader
import me.anno.gpu.shader.FlatShaders.flatShaderCubemap
import me.anno.gpu.shader.FlatShaders.flatShaderTexture
import me.anno.gpu.texture.*
import me.anno.utils.types.Booleans.toInt
import me.anno.video.formats.gpu.GPUFrame
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.joml.Vector4fc

object DrawTextures {

    fun drawProjection(
        x: Int, y: Int, w: Int, h: Int,
        texture: CubemapTexture, ignoreAlpha: Boolean, color: Int,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderCubemap.value
        shader.use()
        posSize(shader, x, y, w, h)
        defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", color)
        shader.v1b("ignoreTexAlpha", ignoreAlpha)
        shader.v1b("applyToneMapping", applyToneMapping)
        texture.bind(0, texture.filtering, Clamping.CLAMP)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean, color: Int = -1, tiling: Vector4fc? = null,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderTexture.value
        shader.use()
        posSize(shader, x, y, w, h)
        defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", color)
        shader.v1i("alphaMode", ignoreAlpha.toInt())
        shader.v1b("applyToneMapping", applyToneMapping)
        GFXx2D.tiling(shader, tiling)
        val tex = texture as? Texture2D
        texture.bind(
            0,
            tex?.filtering ?: GPUFiltering.NEAREST,
            tex?.clamping ?: Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawDepthTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = depthShader.value
        shader.use()
        posSize(shader, x, y+h-1, w, -h)
        defineAdvancedGraphicalFeatures(shader)
        GFXx2D.noTiling(shader)
        val tex = texture as? Texture2D
        texture.bind(
            0,
            tex?.filtering ?: GPUFiltering.NEAREST,
            tex?.clamping ?: Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawTextureAlpha(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D,
        color: Int = -1, tiling: Vector4fc? = null,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderTexture.value
        shader.use()
        posSize(shader, x, y, w, h)
        defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", color)
        shader.v1i("alphaMode", 2)
        shader.v1b("applyToneMapping", applyToneMapping)
        GFXx2D.tiling(shader, tiling)
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
        x: Int, y: Int, w: Int, h: Int, texture: ITexture2D,
        color: Int, tiling: Vector4fc?, applyToneMapping: Boolean = false
    ) {
        drawTexture(x, y, w, h, texture, false, color, tiling, applyToneMapping)
    }

    private val tiling = Vector4f()
    fun drawTransparentBackground(x: Int, y: Int, w: Int, h: Int, numVerticalStripes: Float = 5f) {
        tiling.set(numVerticalStripes * w.toFloat() / h.toFloat(), numVerticalStripes, 0f, 0f)
        drawTexture(x, y, w, h, TextureLib.colorShowTexture, -1, tiling, false)
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