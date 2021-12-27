package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.GFXx3D.draw3DCircle
import me.anno.gpu.shader.Shader
import me.anno.objects.GFXTransform
import me.anno.objects.GFXTransform.Companion.uploadAttractors0
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.utils.maths.Maths.clamp
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

object GFXx2D {

    // todo implement a global matrix here, which can be used to draw GUI element inside the world
    // todo mesh or image backgrounds for panels
    // todo create UI in editor from components somehow
    // todo if we use this, we no longer can rely on clipping
    // todo if we use this, we get subpixel errors, and need to switch to other rendering techniques

    // val uiTransform = Matrix4f()

    fun drawBorder(x: Int, y: Int, w: Int, h: Int, color: Int, size: Int) {
        flatColor(color)
        drawRect(x, y, w, size)
        drawRect(x, y + h - size, w, size)
        drawRect(x, y + size, size, h - 2 * size)
        drawRect(x + w - size, y + size, size, h - 2 * size)
    }

    fun flatColor(color: Int) {
        val shader = ShaderLib.flatShader.value
        shader.use()
        shader.v4("color", color)
    }

    // the background color is important for correct subpixel rendering, because we can't blend per channel
    /*fun drawText(
        x: Int, y: Int, font: Font, text: String,
        color: Int, backgroundColor: Int, widthLimit: Int, centerX: Boolean = false
    ) =
        drawText(x, y, font, text, color, backgroundColor, widthLimit, centerX)*/

    fun getSizeX(value: Int) = value.and(0xffff)
    fun getSizeY(value: Int) = value.shr(16).and(0xffff)
    fun getSize(x: Int, y: Int) = clamp(x, 0, 0xffff) or clamp(y, 0, 0xffff).shl(16)

    fun drawCircle(
        x: Int, y: Int,
        radiusX: Float, radiusY: Float, innerRadius: Float, startDegrees: Float, endDegrees: Float, color: Vector4f
    ) {

        val rx = (x - GFX.windowX).toFloat() / GFX.windowWidth * 2 - 1
        val ry = (y - GFX.windowY).toFloat() / GFX.windowHeight * 2 - 1

        val stack = Matrix4fArrayList()
        stack.translate(rx, ry, 0f)
        stack.scale(2f * radiusX / GFX.windowWidth, 2f * radiusY / GFX.windowHeight, 1f)

        // GFX.drawMode = ShaderPlus.DrawMode.COLOR
        // RenderSettings.renderer.use(Renderer.colorRenderer) {
        // not perfect, but pretty good
        // anti-aliasing for the rough edges
        // not very economical, could be improved
        color.w /= 25f
        for (dx in 0 until 5) {
            for (dy in 0 until 5) {
                stack.pushMatrix()
                stack.translate((dx - 2f) / (2.5f * GFX.windowWidth), (dy - 2f) / (2.5f * GFX.windowHeight), 0f)
                draw3DCircle(null, 0.0, stack, innerRadius, startDegrees, endDegrees, color)
                stack.popMatrix()
            }
        }

    }

    fun posSize(shader: Shader, x: Int, y: Int, w: Int, h: Int) {
        val posX = (x - GFX.windowX).toFloat() / GFX.windowWidth
        val posY = 1f - (y - GFX.windowY).toFloat() / GFX.windowHeight
        val relW = +w.toFloat() / GFX.windowWidth
        val relH = -h.toFloat() / GFX.windowHeight
        shader.v2("pos", posX, posY)
        shader.v2("size", relW, relH)
    }

    fun posSize(shader: Shader, x: Float, y: Float, w: Float, h: Float) {
        val posX = (x - GFX.windowX) / GFX.windowWidth
        val posY = 1f - (y - GFX.windowY) / GFX.windowHeight
        val relW = +w / GFX.windowWidth
        val relH = -h / GFX.windowHeight
        shader.v2("pos", posX, posY)
        shader.v2("size", relW, relH)
    }

    fun defineAdvancedGraphicalFeatures(shader: Shader) {
        disableAdvancedGraphicalFeatures(shader)
    }

    fun defineAdvancedGraphicalFeatures(shader: Shader, transform: Transform?, time: Double) {
        (transform as? GFXTransform)?.uploadAttractors(shader, time) ?: uploadAttractors0(shader)
        GFXx3D.colorGradingUniforms(transform as? Video, time, shader)
    }

    fun disableAdvancedGraphicalFeatures(shader: Shader) {
        shader.v1("forceFieldUVCount", 0)
        shader.v1("forceFieldColorCount", 0)
        shader.v3("cgSlope", 1f)
        shader.v3("cgOffset", 0f)
        shader.v3("cgPower", 1f)
        shader.v1("cgSaturation", 1f)
    }


}