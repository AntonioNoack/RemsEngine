package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.FlatShaders.flatShader
import me.anno.gpu.shader.FlatSymbols.flatShaderCircle
import me.anno.gpu.shader.FlatSymbols.flatShaderHalfArrow
import me.anno.gpu.shader.Shader
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

object GFXx2D {

    val transform = Matrix4fArrayList()

    // done implement transformed UI rendering:
    // subpixel errors -> non subpixel font textures, maybe sdf,
    // clipping no longer works
    // done implement a global matrix here, which can be used to draw GUI element inside the world

    fun tiling(shader: Shader, tiling: Vector4f?) {
        if (tiling != null) shader.v4f("tiling", tiling)
        else noTiling(shader)
    }

    fun noTiling(shader: Shader) {
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
    }

    fun flatColor(color: Int) {
        val shader = flatShader.value
        shader.use()
        shader.v4f("color", color)
    }

    fun getSizeX(value: Int) = value.and(0xffff)
    fun getSizeY(value: Int) = value.ushr(16)
    fun getSize(x: Int, y: Int) = clamp(x, 0, 0xffff) or clamp(y, 0, 0xffff).shl(16)

    private val circleStack = Matrix4fArrayList()
    fun drawCircleOld(
        x: Int, y: Int,
        radiusX: Float, radiusY: Float, innerRadius: Float,
        startDegrees: Float, endDegrees: Float,
        color: Vector4f
    ) {

        val rx = (x - GFX.viewportX).toFloat() / GFX.viewportWidth * 2 - 1
        val ry = (1f - (y - GFX.viewportY).toFloat() / GFX.viewportHeight) * 2 - 1

        val stack = circleStack
        stack.identity()
        stack.translate(rx, ry, 0f)
        stack.scale(2f * radiusX / GFX.viewportWidth, 2f * radiusY / GFX.viewportHeight, 1f)

        // GFX.drawMode = ShaderPlus.DrawMode.COLOR
        // RenderSettings.renderer.use(Renderer.colorRenderer) {
        // not perfect, but pretty good
        // anti-aliasing for the rough edges
        // not very economical, could be improved
        color.w /= 25f
        for (dx in 0 until 5) {
            for (dy in 0 until 5) {
                stack.pushMatrix()
                stack.translate((dx - 2f) / (2.5f * GFX.viewportWidth), (dy - 2f) / (2.5f * GFX.viewportHeight), 0f)
                GFXx3D.draw3DCircle(stack, innerRadius, startDegrees, endDegrees, color)
                stack.popMatrix()
            }
        }

    }

    fun drawCircle(
        x: Int, y: Int,
        radiusX: Float, radiusY: Float, innerRadius: Float,
        startDegrees: Float, endDegrees: Float,
        color: Vector4f
    ) {

        val shader = flatShaderCircle.value
        shader.use()

        posSize(shader, x - radiusX, y - radiusY, radiusX * 2f, radiusY * 2f)

        shader.v1f("innerRadius", innerRadius)
        if (innerRadius > 0f) {
            shader.v4f("innerColor", 0f, 0f, 0f, 0f)
        } else {
            shader.v4f("innerColor", color)
        }
        noTiling(shader)
        shader.v4f("circleColor", color)
        shader.v4f("backgroundColor", 0f, 0f, 0f, 0f)
        shader.v2f("angleLimits", startDegrees.toRadians(), endDegrees.toRadians())
        shader.v1f("smoothness", 1.5f)

        flat01.draw(shader)

    }

    fun drawCircle(
        x: Int, y: Int,
        radiusX: Float, radiusY: Float, innerRadius: Float,
        startDegrees: Float, endDegrees: Float,
        color: Int
    ) = drawCircle(x.toFloat(), y.toFloat(), radiusX, radiusY, innerRadius, startDegrees, endDegrees, color)

    fun drawCircle(
        x: Float, y: Float,
        radiusX: Float, radiusY: Float, innerRadius: Float,
        startDegrees: Float, endDegrees: Float,
        color: Int
    ) {

        val shader = flatShaderCircle.value
        shader.use()

        posSize(shader, x - radiusX, y - radiusY, radiusX * 2f, radiusY * 2f)

        val factor = radiusX / (radiusX + 2f)
        shader.v1f("outerRadius", factor)
        shader.v1f("innerRadius", innerRadius * factor)

        // shader.v1f("innerRadius", innerRadius)
        if (innerRadius > 0f) {
            shader.v4f("innerColor", 0f, 0f, 0f, 0f)
        } else {
            shader.v4f("innerColor", color)
        }

        noTiling(shader)
        shader.v4f("circleColor", color)
        shader.v4f("backgroundColor", 0f, 0f, 0f, 0f)
        shader.v2f("angleLimits", startDegrees.toRadians(), endDegrees.toRadians())
        shader.v1f("smoothness", 1.5f)

        flat01.draw(shader)

    }

    fun drawCircle(
        x: Int, y: Int,
        radiusX: Float, radiusY: Float,
        innerRadius: Float,
        innerColor: Int,
        circleColor: Int,
        backgroundColor: Int,
        smoothness: Float = 1f
    ) {

        val shader = flatShaderCircle.value
        shader.use()

        // plus padding of 1 for better border smoothness
        posSize(shader, x - radiusX - 1f, y - radiusY - 1f, radiusX * 2f + 2f, radiusY * 2f + 2f)

        val factor = radiusX / (radiusX + 2f)
        shader.v1f("outerRadius", factor)
        shader.v1f("innerRadius", innerRadius * factor)
        shader.v4f("innerColor", if (innerRadius > 0f) innerColor else circleColor)

        noTiling(shader)
        shader.v4f("circleColor", circleColor)
        shader.v4f("backgroundColor", backgroundColor)
        shader.v2f("angleLimits", 0f, 0f)
        shader.v1f("smoothness", smoothness)

        flat01.draw(shader)

    }

    fun drawHalfArrow(
        x: Float, y: Float,
        w: Float, h: Float,
        color: Int,
        backgroundColor: Int,
        smoothness: Float = 1f
    ) {

        val shader = flatShaderHalfArrow.value
        shader.use()

        // plus padding of 1 for better border smoothness
        posSize(shader, x, y, w, h)

        noTiling(shader)
        shader.v4f("color", color)
        shader.v4f("backgroundColor", backgroundColor)
        shader.v1f("smoothness", smoothness)

        flat01.draw(shader)

    }

    fun posSize(shader: Shader, x: Int, y: Int, w: Int, h: Int) {
        posSize(shader, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
    }

    fun posSizeDraw(shader: ComputeShader, x: Int, y: Int, w: Int, h: Int, padding: Int = 0) {
        val fb = GFXState.currentBuffer as? Framebuffer
        // check how much this out of bounds
        val minX = max(x - padding, GFX.viewportX)
        val minY = max(y - padding, GFX.viewportY)
        val maxX = min(x + w + padding, GFX.viewportX + GFX.viewportWidth)
        val maxY = min(y + h + padding, GFX.viewportY + GFX.viewportHeight)
        if (minX < maxX && minY < maxY) {
            shader.v2i("srcOffset", minX - x, minY - y)
            shader.v2i("dstOffset", minX - (fb?.offsetX ?: 0), minY - (fb?.offsetY ?: 0))
            shader.v2i("invokeSize", maxX - minX, maxY - minY)
            shader.runBySize(maxX - minX, maxY - minY)
        }
    }

    fun posSize(shader: Shader, x: Float, y: Float, w: Float, h: Float) {
        val posX = (x - GFX.viewportX) / GFX.viewportWidth
        val posY = 1f - (y - GFX.viewportY) / GFX.viewportHeight
        val relW = +w / GFX.viewportWidth
        val relH = -h / GFX.viewportHeight
        shader.m4x4("transform", transform)
        shader.v4f("posSize", posX, posY, relW, relH)
    }

    fun defineAdvancedGraphicalFeatures(shader: Shader) {
        disableAdvancedGraphicalFeatures(shader)
    }

    fun disableAdvancedGraphicalFeatures(shader: Shader) {
        shader.v1i("forceFieldUVCount", 0)
        shader.v1i("forceFieldColorCount", 0)
        shader.v3f("cgSlope", 1f)
        shader.v3f("cgOffset", 0f)
        shader.v3f("cgPower", 1f)
        shader.v1f("cgSaturation", 1f)
    }

}