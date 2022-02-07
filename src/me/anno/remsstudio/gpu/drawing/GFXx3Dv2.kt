package me.anno.remsstudio.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.circleBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.drawing.UVProjection
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.objects.GFXTransform
import me.anno.remsstudio.objects.Video
import me.anno.remsstudio.objects.geometric.Polygon
import me.anno.video.formats.gpu.GPUFrame
import ofx.mio.OpticalFlow
import org.joml.*
import kotlin.math.min

object GFXx3Dv2 {

    fun getScale(w: Int, h: Int): Float = getScale(w.toFloat(), h.toFloat())
    fun getScale(w: Float, h: Float): Float {
        return if (w * RemsStudio.targetHeight > h * RemsStudio.targetWidth) RemsStudio.targetWidth / (w * RemsStudio.targetHeight) else 1f / h
    }

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

        GFXx3D.transformUniform(shader, stack)
        shader.v1i("filtering", filtering.id)
        shader.v2f("textureDeltaUV", 1f / w, 1f / h)

        stack.popMatrix()

        if (tiling != null) shader.v4f("tiling", tiling)
        else shader.v4f("tiling", 1f, 1f, 0f, 0f)
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

    fun draw3DText(
        that: GFXTransform, time: Double, offset: Vector3fc,
        stack: Matrix4fArrayList, buffer: StaticBuffer, color: Vector4fc
    ) {
        val shader = ShaderLib.shader3DforText.value
        shader.use()
        GFXx3D.shader3DUniforms(shader, stack, color)
        shader.v3f("offset", offset)
        GFXTransform.uploadAttractors(that, shader, time)
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DVideo(
        video: GFXTransform, time: Double,
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4fc,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3DRGBA.value
        shader.use()
        GFXx2Dv2.defineAdvancedGraphicalFeatures(shader, video, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3DVideo(
        video: GFXTransform, time: Double,
        stack: Matrix4fArrayList, v0: GPUFrame, v1: GPUFrame, interpolation: Float, color: Vector4fc,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) {

        if (!v0.isCreated || !v1.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")

        val t0 = v0.getTextures()
        val t1 = v1.getTextures()

        val lambda = 0.01f
        val blurAmount = 0.05f

        OpenGL.renderPurely {
            // interpolate all textures
            val interpolated = t0.zip(t1).map { (x0, x1) -> OpticalFlow.run(lambda, blurAmount, interpolation, x0, x1) }
            // bind them
            v0.bind2(0, filtering, clamping, interpolated)
        }

        val shader0 = v0.get3DShader()
        val shader = shader0.value
        shader.use()
        GFXx2Dv2.defineAdvancedGraphicalFeatures(shader, video, time)
        shader3DUniforms(shader, stack, v0.w, v0.h, color, tiling, filtering, uvProjection)
        v0.bindUVCorrection(shader)
        uvProjection.getBuffer().draw(shader)
        GFX.check()

    }

    fun draw3DVideo(
        video: GFXTransform, time: Double,
        stack: Matrix4fArrayList, texture: GPUFrame, color: Vector4fc,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?, uvProjection: UVProjection
    ) {
        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader0 = texture.get3DShader()
        val shader = shader0.value
        shader.use()
        GFXx2Dv2.defineAdvancedGraphicalFeatures(shader, video, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3DPolygon(
        polygon: Polygon, time: Double,
        stack: Matrix4fArrayList, buffer: StaticBuffer,
        texture: Texture2D, color: Vector4fc,
        inset: Float,
        filtering: Filtering, clamping: Clamping
    ) {
        val shader = ShaderLib.shader3DPolygon.value
        shader.use()
        polygon.uploadAttractors(shader, time)
        shader3DUniforms(shader, stack, texture.w, texture.h, color, null, filtering, null)
        shader.v1f("inset", inset)
        texture.bind(0, filtering, clamping)
        buffer.draw(shader)
        GFX.check()
    }

    fun drawOutlinedText(
        that: GFXTransform,
        time: Double,
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

        GFXx2Dv2.defineAdvancedGraphicalFeatures(shader, that, time)

        GFX.shaderColor(shader, "tint", color)

        shader.v1i("drawMode", GFX.drawMode.id)

        val cc = min(colorCount, ShaderLib.maxOutlineColors)

        /**
         * u4[ maxColors ] colors
         * u2[ maxColors ] distSmooth
         * uniform int colorCount
         * */
        val buffer = GFXx3D.outlineStatsBuffer
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

        GFXx3D.drawOutlinedText(stack, offset, scale, texture, hasUVAttractors)

    }

    fun draw3DCircle(
        that: GFXTransform, time: Double,
        stack: Matrix4fArrayList,
        innerRadius: Float,
        startDegrees: Float,
        endDegrees: Float,
        color: Vector4fc
    ) {
        val shader = ShaderLib.shader3DCircle.value
        shader.use()
        GFXx2Dv2.defineAdvancedGraphicalFeatures(shader, that, time)
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

    fun draw3DMasked(
        stack: Matrix4fArrayList,
        color: Vector4fc,
        maskType: Int,
        useMaskColor: Float,
        pixelSize: Float,
        offset: Vector2fc,
        isInverted: Float,
        isFullscreen: Boolean,
        settings: Vector4f
    ) {
        val shader = ShaderLib.shader3DMasked.value
        shader.use()
        GFXx3D.shader3DUniforms(shader, stack, color)
        shader.v1f("useMaskColor", useMaskColor)
        shader.v1f("invertMask", isInverted)
        shader.v1i("maskType", maskType)
        shader.v2f("pixelating", pixelSize * GFX.viewportHeight / GFX.viewportWidth, pixelSize)
        shader.v4f("settings", settings)
        shader.v2f("offset", offset)
        shader.v2f("windowSize", GFX.viewportWidth.toFloat(), GFX.viewportHeight.toFloat())
        val buffer = if (isFullscreen) SimpleBuffer.flatLarge else SimpleBuffer.flat11
        buffer.draw(shader)
        GFX.check()
    }

    fun colorGradingUniforms(video: Video, time: Double, shader: Shader) {
        GFXx3D.tmp0.set(video.cgOffsetAdd[time, GFXx3D.tmp0])
        GFXx3D.tmp1.set(video.cgOffsetSub[time, GFXx3D.tmp1])
        shader.v3f("cgOffset", GFXx3D.tmp0.sub(GFXx3D.tmp1))
        shader.v3X("cgSlope", video.cgSlope[time, GFXx3D.tmp2])
        shader.v3X("cgPower", video.cgPower[time, GFXx3D.tmp2])
        shader.v1f("cgSaturation", video.cgSaturation[time])
    }

}