package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.circleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01Cube
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01CubeX10
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx2D.defineAdvancedGraphicalFeatures
import me.anno.gpu.drawing.GFXx2D.disableAdvancedGraphicalFeatures
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.maxOutlineColors
import me.anno.gpu.texture.*
import me.anno.utils.Color.toARGB
import me.anno.video.formats.gpu.GPUFrame
import org.joml.*
import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import kotlin.math.min

@Suppress("unused")
object GFXx3D {

    fun getScale(w: Int, h: Int) = 1f / h
    fun getScale(w: Float, h: Float) = 1f / h

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int,
        tiling: Vector4fc?, filtering: Filtering,
        uvProjection: UVProjection?
    ) {

        stack.pushMatrix()

        val doScale2 = (uvProjection?.doScale ?: true) && w != h
        if (doScale2) {
            val scale = getScale(w, h)
            val sx = w * scale
            val sy = h * scale
            stack.scale(sx, -sy, 1f)
        } else {
            stack.scale(1f, -1f, 1f)
        }

        transformUniform(shader, stack)
        shader.v1i("filtering", filtering.id)
        shader.v2f("textureDeltaUV", 1f / w, 1f / h)

        stack.popMatrix()

        GFXx2D.tiling(shader, tiling)
        shader.v1i("drawMode", GFX.drawMode.id)
        shader.v1i("uvProjection", uvProjection?.id ?: UVProjection.Planar.id)

    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Vector4fc?,
        tiling: Vector4fc?, filtering: Filtering,
        uvProjection: UVProjection?
    ) {
        shader3DUniforms(shader, stack, w, h, tiling, filtering, uvProjection)
        GFX.shaderColor(shader, "tint", color)
    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Int,
        tiling: Vector4fc?, filtering: Filtering,
        uvProjection: UVProjection?
    ) {
        shader3DUniforms(shader, stack, w, h, tiling, filtering, uvProjection)
        GFX.shaderColor(shader, "tint", color)
    }

    fun drawDebugCube(matrix: Matrix4fArrayList, size: Float, color: Vector4fc?) {
        matrix.scale(0.5f * size, -0.5f * size, 0.5f * size) // flip inside out
        val tex = TextureLib.whiteTexture
        draw3D(
            matrix, tex, color,
            Filtering.NEAREST, tex.clamping!!, null, UVProjection.TiledCubemap
        )
    }

    fun shader3DUniforms(shader: Shader, stack: Matrix4f?, color: Int) {
        transformUniform(shader, stack)
        GFX.shaderColor(shader, "tint", color)
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
        shader.v1i("drawMode", GFX.drawMode.id)
    }

    fun shader3DUniforms(shader: Shader, stack: Matrix4f, color: Vector4fc) {
        transformUniform(shader, stack)
        GFX.shaderColor(shader, "tint", color)
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
        shader.v1i("drawMode", GFX.drawMode.id)
    }

    fun transformUniform(shader: Shader, stack: Matrix4fc?) {
        GFX.check()
        shader.m4x4("transform", stack)
    }

    fun draw3DText(
        offset: Vector3fc,
        stack: Matrix4fArrayList, buffer: StaticBuffer, color: Vector4fc
    ) = draw3DText(offset, stack, buffer, color.toARGB())

    fun draw3DText(
        offset: Vector3fc,
        stack: Matrix4fArrayList, buffer: StaticBuffer, color: Int
    ) {
        val shader = ShaderLib.shader3DText.value
        shader.use()
        shader3DUniforms(shader, stack, color)
        shader.v3f("offset", offset)
        uploadAttractors0(shader)
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DTextWithOffset(
        buffer: StaticBuffer,
        offset: Vector3fc
    ) {
        val shader = ShaderLib.shader3DText.value
        shader.use()
        shader.v3f("offset", offset)
        buffer.draw(shader)
    }

    fun colorGradingUniforms(shader: Shader) {
        shader.v3f("cgOffset", 0f)
        shader.v3f("cgSlope", 1f)
        shader.v3f("cgPower", 1f)
        shader.v1f("cgSaturation", 1f)
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: GPUFrame, color: Vector4fc,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) {
        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader0 = texture.get3DShader()
        val shader = shader0.value
        shader.use()
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        disableAdvancedGraphicalFeatures(shader)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: GPUFrame, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) {
        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().value
        shader.use()
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        defineAdvancedGraphicalFeatures(shader)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4fc?,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) = draw3D(stack, texture, texture.w, texture.h, color, filtering, clamping, tiling, uvProjection)

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) = draw3D(stack, texture, texture.w, texture.h, color, filtering, clamping, tiling, uvProjection)

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, w: Int, h: Int, color: Vector4fc?,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3D.value
        shader.use()
        defineAdvancedGraphicalFeatures(shader)
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, w: Int, h: Int, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3D.value
        shader.use()
        defineAdvancedGraphicalFeatures(shader)
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    val outlineStatsBuffer: FloatBuffer = BufferUtils.createFloatBuffer(maxOutlineColors * 4)
    fun drawOutlinedText(
        stack: Matrix4fArrayList,
        offset: Vector2fc,
        scale: Vector2fc,
        texture: Texture2D,
        color: Vector4fc,
        colorCount: Int,
        colors: Array<Vector4fc>,
        distances: FloatArray,
        smoothness: FloatArray,
        depth: Float,
        hasUVAttractors: Boolean
    ) {

        val shader = ShaderLib.shaderSDFText.value
        shader.use()

        defineAdvancedGraphicalFeatures(shader)

        GFX.shaderColor(shader, "tint", color)

        shader.v1i("drawMode", GFX.drawMode.id)

        val cc = min(colorCount, maxOutlineColors)

        /**
         * u4[ maxColors ] colors
         * u2[ maxColors ] distSmooth
         * uniform int colorCount
         * */
        val buffer = outlineStatsBuffer
        buffer.position(0)
        for (i in 0 until cc) {
            val colorI = colors[i]
            buffer.put(colorI.x())
            buffer.put(colorI.y())
            buffer.put(colorI.z())
            buffer.put(colorI.w())
        }
        buffer.position(0)
        shader.v4Array("colors", buffer)
        buffer.position(0)
        for (i in 0 until cc) {
            buffer.put(distances[i])
            buffer.put(smoothness[i])
        }
        buffer.position(0)
        shader.v2Array("distSmoothness", buffer)
        shader.v1i("colorCount", cc)
        shader.v1f("depth", depth * 0.00001f)

        drawOutlinedText(stack, offset, scale, texture, hasUVAttractors)

    }

    fun drawOutlinedText(
        stack: Matrix4fArrayList,
        offset: Vector2fc,
        scale: Vector2fc,
        texture: Texture2D,
        hasUVAttractors: Boolean
    ) {
        val shader = ShaderLib.shaderSDFText.value
        shader.use()
        transformUniform(shader, stack)
        shader.v2f("offset", offset)
        shader.v2f("scale", scale)
        texture.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
        // if we have a force field applied, subdivide the geometry
        val buffer = if (hasUVAttractors) flat01CubeX10 else flat01Cube
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DGaussianBlur(
        stack: Matrix4fArrayList,
        size: Float, w: Int, h: Int,
        threshold: Float, isFirst: Boolean,
        isFullscreen: Boolean
    ) {
        val shader = ShaderLib.shader3DGaussianBlur.value
        shader.use()
        transformUniform(shader, stack)
        if (isFirst) shader.v2f("stepSize", 0f, 1f / h)
        else shader.v2f("stepSize", 1f / w, 0f)
        shader.v1f("steps", size * h)
        shader.v1f("threshold", threshold)
        val buffer = if (isFullscreen) SimpleBuffer.flatLarge else SimpleBuffer.flat11
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DBoxBlur(
        stack: Matrix4fArrayList,
        steps: Int, w: Int, h: Int,
        isFirst: Boolean
    ) {
        val shader = ShaderLib.shader3DBoxBlur.value
        shader.use()
        transformUniform(shader, stack)
        if (isFirst) {
            shader.v2f("stepSize", 0f, 1f / h)
            shader.v1i("steps", steps)
        } else {
            shader.v2f("stepSize", 1f / w, 0f)
            shader.v1i("steps", steps)
        }
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun draw3DCircle(
        stack: Matrix4fArrayList,
        innerRadius: Float,
        startDegrees: Float,
        endDegrees: Float,
        color: Vector4fc
    ) {
        val shader = ShaderLib.shader3DCircle.value
        shader.use()
        defineAdvancedGraphicalFeatures(shader)
        shader3DUniforms(shader, stack, 1, 1, color, null, Filtering.NEAREST, null)
        var a0 = startDegrees
        var a1 = endDegrees
        // if the two arrows switch sides, flip the circle
        if (a0 > a1) {// first start for checker pattern
            val tmp = a0
            a0 = a1
            a1 = tmp - 360f
        }
        // fix edge resolution loss
        if (a0 > a1 + 360) {
            a0 = a1 + 360
        } else if (a1 > a0 + 360) {
            a1 = a0 + 360
        }
        val angle0 = GFX.toRadians(a0)
        val angle1 = GFX.toRadians(a1)
        shader.v3f("circleParams", 1f - innerRadius, angle0, angle1)
        circleBuffer.draw(shader)
        GFX.check()
    }

    fun uploadAttractors0(shader: Shader) {

        // localScale, localOffset not needed
        shader.v1i("forceFieldColorCount", 0)
        shader.v1i("forceFieldUVCount", 0)

    }

}