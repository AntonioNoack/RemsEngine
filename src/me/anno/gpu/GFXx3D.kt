package me.anno.gpu

import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.objects.GFXTransform
import me.anno.objects.Video
import me.anno.objects.effects.MaskType
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.modes.UVProjection
import me.anno.studio.RemsStudio
import me.anno.video.VFrame
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.roundToInt

object GFXx3D {

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Vector4f,
        tiling: Vector4f?, filtering: Filtering,
        uvProjection: UVProjection?
    ) {
        GFX.check()

        shader.use()
        stack.pushMatrix()

        val doScale2 = (uvProjection?.doScale ?: true) && w != h

        shader.v1("filtering", filtering.id)
        shader.v2("textureDeltaUV", 1f / w, 1f / h)

        // val avgSize = sqrt(w * h.toFloat())
        if (doScale2) {
            val avgSize =
                if (w * RemsStudio.targetHeight > h * RemsStudio.targetWidth) w.toFloat() * RemsStudio.targetHeight / RemsStudio.targetWidth else h.toFloat()
            val sx = w / avgSize
            val sy = h / avgSize
            stack.scale(sx, -sy, 1f)
        } else {
            stack.scale(1f, -1f, 1f)
        }

        shader.m4x4("transform", stack)
        stack.popMatrix()

        GFX.shaderColor(shader, "tint", color)
        if (tiling != null) shader.v4("tiling", tiling)
        else shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", GFX.drawMode.id)
        shader.v1("uvProjection", uvProjection?.id ?: UVProjection.Planar.id)

    }

    fun drawDebugCube(matrix: Matrix4fArrayList, size: Float, color: Vector4f?) {
        matrix.scale(0.5f * size, -0.5f * size, 0.5f * size) // flip inside out
        val tex = TextureLib.whiteTexture
        draw3D(
            matrix, tex, color ?: Vector4f(1f),
            Filtering.NEAREST, tex.clamping, null, UVProjection.TiledCubemap
        )
    }

    fun shader3DUniforms(shader: Shader, stack: Matrix4f, color: Vector4f) {
        GFX.check()
        shader.use()
        shader.m4x4("transform", stack)
        GFX.shaderColor(shader, "tint", color)
        shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", GFX.drawMode.id)
    }

    fun transformUniform(shader: Shader, stack: Matrix4f) {
        GFX.check()
        shader.use()
        shader.m4x4("transform", stack)
    }

    fun draw3DMasked(
        stack: Matrix4fArrayList, color: Vector4f,
        maskType: MaskType,
        useMaskColor: Float,
        pixelSize: Float,
        isInverted: Float,
        isFullscreen: Boolean,
        greenScreenSettings: Vector3f
    ) {
        val shader = ShaderLib.shader3DMasked
        shader3DUniforms(shader, stack, color)
        shader.v1("useMaskColor", useMaskColor)
        shader.v1("invertMask", isInverted)
        shader.v1("maskType", maskType.id)
        shader.v2("pixelating", pixelSize * GFX.windowHeight / GFX.windowWidth, pixelSize)
        shader.v3("greenScreenSettings", greenScreenSettings)
        val buffer = if (isFullscreen) SimpleBuffer.flatLarge else SimpleBuffer.flat11
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DText(
        that: GFXTransform?, time: Double, offset: Vector3f,
        stack: Matrix4fArrayList, buffer: StaticBuffer, color: Vector4f
    ) {
        // todo remove the y-scale of -1 everywhere...
        stack.pushMatrix()
        stack.scale(1f, -1f, 1f)
        val shader = ShaderLib.shader3DforText
        shader3DUniforms(shader, stack, color)
        shader.v3("offset", offset)
        that?.uploadAttractors(shader, time) ?: GFXTransform.uploadAttractors0(shader)
        buffer.draw(shader)
        GFX.check()
        stack.popMatrix()
    }

    fun draw3DTextWithOffset(
        buffer: StaticBuffer,
        offset: Vector3f
    ) {
        val shader = ShaderLib.shader3DforText
        shader.v3("offset", offset)
        buffer.draw(shader)
    }

    fun colorGradingUniforms(video: Video?, time: Double, shader: Shader) {
        if (video == null) {
            shader.v3("cgOffset", Vector3f())
            shader.v3X("cgSlope", Vector4f(1f))
            shader.v3X("cgPower", Vector4f(1f))
            shader.v1("cgSaturation", 1f)
        } else {
            shader.v3("cgOffset", video.cgOffset[time])
            shader.v3X("cgSlope", video.cgSlope[time])
            shader.v3X("cgPower", video.cgPower[time])
            shader.v1("cgSaturation", video.cgSaturation[time])
        }
    }

    fun draw3DPolygon(
        polygon: Polygon, time: Double,
        stack: Matrix4fArrayList, buffer: StaticBuffer,
        texture: Texture2D, color: Vector4f,
        inset: Float,
        filtering: Filtering, clamping: Clamping
    ) {
        val shader = ShaderLib.shader3DPolygon
        shader.use()
        polygon.uploadAttractors(shader, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, null, filtering, null)
        shader.v1("inset", inset)
        texture.bind(0, filtering, clamping)
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: VFrame, color: Vector4f,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isLoaded) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader()
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        if (shader == ShaderLib.shader3DYUV) {
            val w = texture.w
            val h = texture.h
            shader.v2("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
        }
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3DVideo(
        video: Video, time: Double,
        stack: Matrix4fArrayList, texture: VFrame, color: Vector4f,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isLoaded) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader()
        shader.use()
        video.uploadAttractors(shader, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        colorGradingUniforms(video, time, shader)
        texture.bind(0, filtering, clamping)
        if (shader == ShaderLib.shader3DYUV) {
            val w = texture.w
            val h = texture.h
            shader.v2("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
        }
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4f,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        draw3D(stack, texture, texture.w, texture.h, color, filtering, clamping, tiling, uvProjection)
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, w: Int, h: Int, color: Vector4f,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3D
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3DVideo(
        video: Video, time: Double,
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4f,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3DRGBA
        shader.use()
        video.uploadAttractors(shader, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        colorGradingUniforms(video, time, shader)
        texture.bind(0, filtering, clamping)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3DGaussianBlur(
        stack: Matrix4fArrayList,
        size: Float, w: Int, h: Int,
        threshold: Float, isFirst: Boolean
    ) {
        val shader = ShaderLib.shader3DGaussianBlur
        transformUniform(shader, stack)
        if (isFirst) shader.v2("stepSize", 0f, 1f / h)
        else shader.v2("stepSize", 1f / w, 0f)
        shader.v1("steps", size * h)
        shader.v1("threshold", threshold)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun draw3DBoxBlur(
        stack: Matrix4fArrayList,
        steps: Int, w: Int, h: Int,
        isFirst: Boolean
    ) {
        val shader = ShaderLib.shader3DBoxBlur
        transformUniform(shader, stack)
        if (isFirst) {
            shader.v2("stepSize", 0f, 1f / h)
            shader.v1("steps", steps)
        } else {
            shader.v2("stepSize", 1f / w, 0f)
            shader.v1("steps", steps)
        }
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun draw3DCircle(
        that: GFXTransform?, time: Double,
        stack: Matrix4fArrayList,
        innerRadius: Float,
        startDegrees: Float,
        endDegrees: Float,
        color: Vector4f
    ) {
        val shader = ShaderLib.shader3DCircle
        shader3DUniforms(shader, stack, 1, 1, color, null, Filtering.NEAREST, null)
        that?.uploadAttractors(shader, time) ?: GFXTransform.uploadAttractors0(shader)
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
        shader.v3("circleParams", 1f - innerRadius, angle0, angle1)
        Circle.drawBuffer(shader)
        GFX.check()
    }

}