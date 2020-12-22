package me.anno.gpu

import me.anno.fonts.FontManager
import me.anno.gpu.GFX.v4
import me.anno.gpu.GFXx3D.draw3D
import me.anno.gpu.GFXx3D.draw3DCircle
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.objects.GFXTransform.Companion.uploadAttractors0
import me.anno.objects.modes.UVProjection
import me.anno.ui.base.Font
import me.anno.video.VFrame
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

object GFXx2D {

    fun drawRectGradient(x: Int, y: Int, w: Int, h: Int, lColor: Vector4f, rColor: Vector4f) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShaderGradient
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("lColor", lColor)
        shader.v4("rColor", rColor)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Vector4f) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawBorder(x: Int, y: Int, w: Int, h: Int, color: Int, size: Int) {
        flatColor(color)
        drawRect(x, y, w, size)
        drawRect(x, y + h - size, w, size)
        drawRect(x, y + size, size, h - 2 * size)
        drawRect(x + w - size, y + size, size, h - 2 * size)
    }

    fun flatColor(color: Int) {
        val shader = ShaderLib.flatShader
        shader.use()
        shader.v4("color", color)
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val shader = ShaderLib.flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        GFX.flat01.draw(shader)
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        GFX.check()
        val shader = ShaderLib.flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    // the background color is important for correct subpixel rendering, because we can't blend per channel
    fun drawText(
        x: Int, y: Int, font: String, fontSize: Float, bold: Boolean, italic: Boolean, text: String,
        color: Int, backgroundColor: Int, widthLimit: Int, centerX: Boolean = false
    ) =
        writeText(x, y, font, fontSize, bold, italic, text, color, backgroundColor, widthLimit, centerX)

    fun drawText(
        x: Int, y: Int, font: Font, text: String,
        color: Int, backgroundColor: Int, widthLimit: Int, centerX: Boolean = false
    ) =
        writeText(
            x, y, font.name, font.size, font.isBold, font.isItalic,
            text, color, backgroundColor, widthLimit, centerX
        )

    fun writeText(
        x: Int, y: Int,
        font: String, fontSize: Float,
        bold: Boolean, italic: Boolean,
        text: String,
        color: Int,
        backgroundColor: Int,
        widthLimit: Int,
        centerX: Boolean = false
    ): Pair<Int, Int> {

        GFX.check()
        val tex0 = FontManager.getString(font, fontSize, text, italic, bold, widthLimit)
        val texture = tex0 ?: return 0 to fontSize.toInt()
        // check()
        val w = texture.w
        val h = texture.h
        if (text.isNotBlank() && (texture !is Texture2D || texture.isCreated)) {
            texture.bind(GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            val shader = ShaderLib.subpixelCorrectTextShader
            // check()
            shader.use()
            var x2 = x
            if (centerX) x2 -= w / 2
            shader.v2(
                "pos",
                (x2 - GFX.windowX).toFloat() / GFX.windowWidth,
                1f - (y - GFX.windowY).toFloat() / GFX.windowHeight
            )
            shader.v2("size", w.toFloat() / GFX.windowWidth, -h.toFloat() / GFX.windowHeight)
            shader.v4("textColor", color)
            shader.v4("backgroundColor", backgroundColor)
            GFX.flat01.draw(shader)
            GFX.check()
        }/* else {
            drawRect(x, y, w, h, backgroundColor or DefaultStyle.black)
        }*/
        return w to h
    }

    fun getTextSize(font: Font, text: String, widthLimit: Int) =
        getTextSize(font.name, font.size, font.isBold, font.isItalic, text, widthLimit)

    fun getTextSize(
        font: String, fontSize: Float, bold: Boolean, italic: Boolean,
        text: String, widthLimit: Int
    ): Pair<Int, Int> {
        // count how many spaces there are at the end
        // get accurate space and tab widths
        val spaceWidth = 0//text.endSpaceCount() * fontSize / 4
        val texture = FontManager.getString(font, fontSize, text, bold, italic, widthLimit)
            ?: return spaceWidth to fontSize.toInt()
        return (texture.w + spaceWidth) to texture.h
    }

    fun drawTexture(x: Int, y: Int, w: Int, h: Int, texture: Texture2D, color: Int, tiling: Vector4f?) {
        GFX.check()
        val shader = ShaderLib.flatShaderTexture
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color)
        if (tiling != null) shader.v4("tiling", tiling)
        else shader.v4("tiling", 1f, 1f, 0f, 0f)
        texture.bind(0, texture.filtering, texture.clamping)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawTexture(matrix: Matrix4fArrayList, w: Int, h: Int, texture: Texture2D, color: Int, tiling: Vector4f?) {
        matrix.scale(w.toFloat() / GFX.windowWidth, h.toFloat() / GFX.windowHeight, 1f)
        GFX.drawMode = ShaderPlus.DrawMode.COLOR
        draw3D(
            matrix, texture, color.v4(),
            Filtering.LINEAR, Clamping.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawTexture(w: Int, h: Int, texture: VFrame, color: Int, tiling: Vector4f?) {
        val matrix = Matrix4fArrayList()
        matrix.scale(w.toFloat() / GFX.windowWidth, h.toFloat() / GFX.windowHeight, 1f)
        GFX.drawMode = ShaderPlus.DrawMode.COLOR
        uploadAttractors0(texture.get3DShader())
        draw3D(
            matrix, texture, color.v4(),
            Filtering.LINEAR, Clamping.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawCircle(
        x: Int, y: Int,
        radiusX: Float, radiusY: Float, innerRadius: Float, startDegrees: Float, endDegrees: Float, color: Vector4f
    ) {

        val rx = (x - GFX.windowX).toFloat() / GFX.windowWidth * 2 - 1
        val ry = (y - GFX.windowY).toFloat() / GFX.windowHeight * 2 - 1

        val matrix = Matrix4fArrayList()
        matrix.translate(rx, ry, 0f)
        matrix.scale(2f * radiusX / GFX.windowWidth, 2f * radiusY / GFX.windowHeight, 1f)

        GFX.drawMode = ShaderPlus.DrawMode.COLOR

        // not perfect, but pretty good
        // anti-aliasing for the rough edges
        // not very economical, could be improved
        color.w /= 25f
        for (dx in 0 until 5) {
            for (dy in 0 until 5) {
                matrix.pushMatrix()
                matrix.translate((dx - 2f) / (2.5f * GFX.windowWidth), (dy - 2f) / (2.5f * GFX.windowHeight), 0f)
                draw3DCircle(null, 0.0, matrix, innerRadius, startDegrees, endDegrees, color)
                matrix.popMatrix()
            }
        }

    }

    fun posSize(shader: Shader, x: Int, y: Int, w: Int, h: Int) {
        shader.v2(
            "pos",
            (x - GFX.windowX).toFloat() / GFX.windowWidth,
            1f - (y - GFX.windowY).toFloat() / GFX.windowHeight
        )
        shader.v2("size", w.toFloat() / GFX.windowWidth, -h.toFloat() / GFX.windowHeight)
    }

    fun posSize(shader: Shader, x: Float, y: Float, w: Float, h: Float) {
        shader.v2("pos", (x - GFX.windowX) / GFX.windowWidth, 1f - (y - GFX.windowY) / GFX.windowHeight)
        shader.v2("size", w / GFX.windowWidth, -h / GFX.windowHeight)
    }

    fun disableAdvancedGraphicalFeatures(shader: Shader) {
        shader.v1("forceFieldUVCount", 0)
        shader.v1("forceFieldColorCount", 0)
        shader.v3("cgSlope", 1f)
        shader.v3("cgOffset", 0f)
        shader.v3("cgPower", 1f)
        shader.v1("cgSaturation", 1f)
    }

    fun draw2D(texture: VFrame) {

        if (!texture.isLoaded) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader()

        GFX.check()

        shader.use()
        shader.v1("filtering", Filtering.LINEAR.id)
        shader.v2("textureDeltaUV", 1f / texture.w, 1f / texture.h)
        shader.m4x4("transform", Matrix4f())
        shader.v4("tint", 1f)
        shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v1("uvProjection", UVProjection.Planar.id)

        disableAdvancedGraphicalFeatures(shader)

        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        if (shader == ShaderLib.shader3DYUV) {
            val w = texture.w
            val h = texture.h
            shader.v2("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
        }

        UVProjection.Planar.getBuffer().draw(shader)
        GFX.check()

    }

}