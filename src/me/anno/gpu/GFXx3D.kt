package me.anno.gpu

import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
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
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

object GFXx3D {

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Vector4f,
        tiling: Vector4f?, filtering: FilteringMode,
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

        stack.get(GFX.matrixBuffer)
        GL20.glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        stack.popMatrix()

        GFX.shaderColor(shader, "tint", color)
        if (tiling != null) shader.v4("tiling", tiling)
        else shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", GFX.drawMode.id)
        shader.v1("uvProjection", uvProjection?.id ?: UVProjection.Planar.id)

    }


    fun shader3DUniforms(shader: Shader, stack: Matrix4f, color: Vector4f) {
        GFX.check()
        shader.use()
        stack.get(GFX.matrixBuffer)
        GL30.glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        GFX.shaderColor(shader, "tint", color)
        shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", GFX.drawMode.id)
    }

    fun transformUniform(shader: Shader, stack: Matrix4f) {
        GFX.check()
        shader.use()
        stack.get(GFX.matrixBuffer)
        GL30.glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
    }

    fun draw3DMasked(
        stack: Matrix4fArrayList, color: Vector4f,
        maskType: MaskType,
        useMaskColor: Float,
        pixelSize: Float,
        isInverted: Float
    ) {
        val shader = ShaderLib.shader3DMasked.shader
        shader3DUniforms(shader, stack, color)
        shader.v1("useMaskColor", useMaskColor)
        shader.v1("invertMask", isInverted)
        shader.v1("maskType", maskType.id)
        shader.v2("pixelating", pixelSize * GFX.windowHeight / GFX.windowWidth, pixelSize)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun draw3D(
        that: GFXTransform?, time: Double, offset: Vector3f,
        stack: Matrix4fArrayList, buffer: StaticBuffer, texture: Texture2D, w: Int, h: Int, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?
    ) {
        val shader = ShaderLib.shader3D.shader
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, null)
        shader.v3("offset", offset)
        that?.uploadAttractors(shader, time) ?: GFXTransform.uploadAttractors0(shader)
        texture.bind(0, filtering, clampMode)
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3D(
        that: GFXTransform?, time: Double, offset: Vector3f,
        stack: Matrix4fArrayList, buffer: StaticBuffer, texture: Texture2D, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?
    ) {
        draw3D(that, time, offset, stack, buffer, texture, texture.w, texture.h, color, filtering, clampMode, tiling)
    }

    fun draw3DOffset(
        buffer: StaticBuffer,
        offset: Vector3f
    ) {
        val shader = ShaderLib.shader3D.shader
        shader.v3("offset", offset)
        buffer.draw(shader)
    }

    fun colorGradingUniforms(video: Video?, time: Double, shader: Shader) {
        if(video == null){
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
        filtering: FilteringMode, clampMode: ClampMode
    ) {
        val shader = ShaderLib.shader3DPolygon.shader
        shader.use()
        polygon.uploadAttractors(shader, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, null, filtering, null)
        shader.v1("inset", inset)
        texture.bind(0, filtering, clampMode)
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: VFrame, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isLoaded) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().shader
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clampMode)
        if (shader == ShaderLib.shader3DYUV.shader) {
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
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isLoaded) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().shader
        shader.use()
        video.uploadAttractors(shader, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        colorGradingUniforms(video, time, shader)
        texture.bind(0, filtering, clampMode)
        if (shader == ShaderLib.shader3DYUV.shader) {
            val w = texture.w
            val h = texture.h
            shader.v2("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
        }
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        draw3D(stack, texture, texture.w, texture.h, color, filtering, clampMode, tiling, uvProjection)
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, w: Int, h: Int, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3D.shader
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, uvProjection)
        shader.v3("offset", 0f, 0f, 0f)
        texture.bind(0, filtering, clampMode)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3DVideo(
        video: Video, time: Double,
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3DRGBA.shader
        shader.use()
        video.uploadAttractors(shader, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        colorGradingUniforms(video, time, shader)
        texture.bind(0, filtering, clampMode)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3DBlur(
        stack: Matrix4fArrayList,
        size: Float, w: Int, h: Int,
        threshold: Float, isFirst: Boolean
    ) {
        val shader = ShaderLib.shader3DBlur
        transformUniform(shader, stack)
        if (isFirst) shader.v2("stepSize", 0f, 1f / h)
        else shader.v2("stepSize", 1f / w, 0f)
        shader.v1("steps", size * h)
        shader.v1("threshold", threshold)
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
        val shader = ShaderLib.shader3DCircle.shader
        shader3DUniforms(shader, stack, 1, 1, color, null, FilteringMode.NEAREST, null)
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