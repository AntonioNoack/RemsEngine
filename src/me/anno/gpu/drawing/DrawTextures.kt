package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.FlatShaders.depthArrayShader
import me.anno.gpu.shader.FlatShaders.depthShader
import me.anno.gpu.shader.FlatShaders.flatShader2dArraySlice
import me.anno.gpu.shader.FlatShaders.flatShader3dSlice
import me.anno.gpu.shader.FlatShaders.flatShaderCubemap
import me.anno.gpu.shader.FlatShaders.flatShaderTexture
import me.anno.gpu.shader.FlatShaders.flatShaderTextureArray
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib
import me.anno.utils.Color.white4
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toInt
import me.anno.video.formats.gpu.GPUFrame
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

object DrawTextures {

    fun drawProjection(
        x: Int, y: Int, w: Int, h: Int,
        texture: CubemapTexture, ignoreAlpha: Boolean, color: Int,
        applyToneMapping: Boolean,
        showDepth: Boolean
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderCubemap.value
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        shader.v1b("ignoreTexAlpha", ignoreAlpha)
        shader.v1b("applyToneMapping", applyToneMapping)
        shader.v1b("showDepth", showDepth)
        texture.bind(0, texture.filtering, Clamping.CLAMP)
        flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean, color: Int = -1, tiling: Vector4f? = null,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderTexture.value
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        shader.v1i("alphaMode", ignoreAlpha.toInt())
        shader.v1b("applyToneMapping", applyToneMapping)
        GFXx2D.tiling(shader, tiling)
        texture.bind(0)
        flat01.draw(shader)
        GFX.check()
    }

    fun drawTextureArray(
        x: Int, y: Int, w: Int, h: Int,
        texture: Texture2DArray, layer: Float,
        ignoreAlpha: Boolean, color: Int = -1, tiling: Vector4f? = null,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderTextureArray.value
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        shader.v1i("alphaMode", ignoreAlpha.toInt())
        shader.v1b("applyToneMapping", applyToneMapping)
        shader.v1f("layer", layer)
        GFXx2D.tiling(shader, tiling)
        texture.bind(0)
        flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean,
        color: Vector4f, tiling: Vector4f? = null,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderTexture.value
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        shader.v1i("alphaMode", ignoreAlpha.toInt())
        shader.v1b("applyToneMapping", applyToneMapping)
        GFXx2D.tiling(shader, tiling)
        texture.bind(0)
        flat01.draw(shader)
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
        posSize(shader, x, y + h - 1, w, -h)
        GFXx2D.noTiling(shader)
        texture.bind(0)
        val depthFunc = texture.depthFunc
        texture.depthFunc = null
        flat01.draw(shader)
        texture.depthFunc = depthFunc
        GFX.check()
    }

    fun drawDepthTextureArray(
        x: Int, y: Int, w: Int, h: Int,
        texture: Texture2DArray, layer: Float
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = depthArrayShader.value
        shader.use()
        posSize(shader, x, y + h - 1, w, -h)
        GFXx2D.noTiling(shader)
        shader.v1f("layer", layer)
        texture.bind(0, texture.filtering, texture.clamping)
        val depthFunc = texture.depthFunc
        texture.depthFunc = null
        flat01.draw(shader)
        texture.depthFunc = depthFunc
        GFX.check()
    }

    fun drawTextureAlpha(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D,
        color: Int = -1, tiling: Vector4f? = null,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderTexture.value
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        shader.v1i("alphaMode", 2)
        shader.v1b("applyToneMapping", applyToneMapping)
        GFXx2D.tiling(shader, tiling)
        texture.bind(0)
        flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int, texture: ITexture2D,
        color: Int = -1, tiling: Vector4f? = null, applyToneMapping: Boolean = false
    ) = drawTexture(x, y, w, h, texture, false, color, tiling, applyToneMapping)

    fun drawTransparentBackground(x: Int, y: Int, w: Int, h: Int, numVerticalStripes: Float = 5f) {
        val tiling = JomlPools.vec4f.create()
        tiling.set(numVerticalStripes * w.toFloat() / h.toFloat(), numVerticalStripes, 0f, 0f)
        val texture = TextureLib.colorShowTexture
        texture.bind(0, Filtering.TRULY_NEAREST, Clamping.REPEAT)
        drawTexture(x, y, w, h, texture, -1, tiling, false)
        JomlPools.vec4f.sub(1)
    }

    fun drawTexture(matrix: Matrix4fArrayList, w: Int, h: Int, texture: Texture2D, color: Int, tiling: Vector4f?) {
        matrix.scale(w.toFloat() / GFX.viewportWidth, h.toFloat() / GFX.viewportHeight, 1f)
        GFXx3D.draw3DPlanar(
            matrix, texture, texture.width, texture.height, color,
            Filtering.LINEAR, Clamping.CLAMP, tiling
        )
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int, texture: GPUFrame,
        flipY: Boolean = false
    ) {
        drawTexture(x, y, w, h, texture, Filtering.LINEAR, Clamping.CLAMP, flipY)
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int, texture: GPUFrame,
        filtering: Filtering, clamping: Clamping, flipY: Boolean = false
    ) {
        if (!texture.isCreated) throw IllegalArgumentException("Frame must be loaded to be rendered!")
        val shader = texture.get2DShader()
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4f("tiling", 1f, if (flipY) -1f else 1f, 0f, 0f)
        shader.v4f("tint", white4)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    fun draw3dSlice(
        x: Int, y: Int, w: Int, h: Int,
        z: Float,
        texture: Texture3D,
        ignoreAlpha: Boolean,
        color: Int,
        applyToneMapping: Boolean,
        showDepth: Boolean
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShader3dSlice.value
        shader.use()
        posSize(shader, x, y + h - 1, w, -h)
        shader.v4f("color", color)
        shader.v1b("ignoreTexAlpha", ignoreAlpha)
        shader.v1b("applyToneMapping", applyToneMapping)
        shader.v1b("showDepth", showDepth)
        shader.v1f("z", z)
        texture.bind(0)
        flat01.draw(shader)
        GFX.check()
    }

    fun draw2dArraySlice(
        x: Int, y: Int, w: Int, h: Int,
        z: Int,
        texture: Texture2DArray,
        ignoreAlpha: Boolean,
        color: Int,
        applyToneMapping: Boolean,
        showDepth: Boolean
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShader2dArraySlice.value
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        shader.v1b("ignoreTexAlpha", ignoreAlpha)
        shader.v1b("applyToneMapping", applyToneMapping)
        shader.v1b("showDepth", showDepth)
        shader.v1f("z", z + 0.5f)
        texture.bind(0)
        flat01.draw(shader)
        GFX.check()
    }
}