package me.anno.gpu

import me.anno.config.DefaultStyle
import me.anno.fonts.FontManager
import me.anno.gpu.GFX.a
import me.anno.gpu.GFX.b
import me.anno.gpu.GFX.g
import me.anno.gpu.GFX.r
import me.anno.gpu.GFX.v4
import me.anno.gpu.GFXx3D.draw3D
import me.anno.gpu.GFXx3D.draw3DCircle
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.objects.modes.UVProjection
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
        shader.v4("color", color.x, color.y, color.z, color.w)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = ShaderLib.flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
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
        shader.v4("color", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
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
        shader.v4("color", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    // the background color is important for correct subpixel rendering, because we can't blend per channel
    fun drawText(
        x: Int, y: Int, font: String, fontSize: Int, bold: Boolean, italic: Boolean, text: String,
        color: Int, backgroundColor: Int, widthLimit: Int, centerX: Boolean = false
    ) =
        writeText(x, y, font, fontSize, bold, italic, text, color, backgroundColor, widthLimit, centerX)

    fun writeText(
        x: Int, y: Int,
        font: String, fontSize: Int,
        bold: Boolean, italic: Boolean,
        text: String,
        color: Int,
        backgroundColor: Int,
        widthLimit: Int,
        centerX: Boolean = false
    ): Pair<Int, Int> {

        GFX.check()
        val texture =
            FontManager.getString(font, fontSize.toFloat(), text, italic, bold, widthLimit) ?: return 0 to fontSize
        // check()
        val w = texture.w
        val h = texture.h
        if (text.isNotBlank()) {
            texture.bind(GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            val shader = ShaderLib.subpixelCorrectTextShader
            // check()
            shader.use()
            var x2 = x
            if (centerX) x2 -= w / 2
            shader.v2("pos", (x2 - GFX.windowX).toFloat() / GFX.windowWidth, 1f - (y - GFX.windowY).toFloat() / GFX.windowHeight)
            shader.v2("size", w.toFloat() / GFX.windowWidth, -h.toFloat() / GFX.windowHeight)
            shader.v4("textColor", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
            shader.v3(
                "backgroundColor",
                backgroundColor.r() / 255f,
                backgroundColor.g() / 255f,
                backgroundColor.b() / 255f
            )
            GFX.flat01.draw(shader)
            GFX.check()
        } else {
            drawRect(x, y, w, h, backgroundColor or DefaultStyle.black)
        }
        return w to h
    }

    // fun getTextSize(fontSize: Int, bold: Boolean, italic: Boolean, text: String) = getTextSize(defaultFont, fontSize, bold, italic, text)
    fun getTextSize(
        font: String,
        fontSize: Int,
        bold: Boolean,
        italic: Boolean,
        text: String,
        widthLimit: Int
    ): Pair<Int, Int> {
        // count how many spaces there are at the end
        // get accurate space and tab widths
        val spaceWidth = 0//text.endSpaceCount() * fontSize / 4
        val texture = FontManager.getString(font, fontSize.toFloat(), text, bold, italic, widthLimit)
            ?: return spaceWidth to fontSize
        return (texture.w + spaceWidth) to texture.h
    }

    fun drawTexture(x: Int, y: Int, w: Int, h: Int, texture: Texture2D, color: Int, tiling: Vector4f?) {
        GFX.check()
        val shader = ShaderLib.flatShaderTexture
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
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
        draw3D(
            matrix, texture, color.v4(),
            Filtering.LINEAR, Clamping.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawCircle(
        w: Int, h: Int, innerRadius: Float, startDegrees: Float, endDegrees: Float, color: Vector4f
    ) {
        // not perfect, but pretty good
        // anti-aliasing for the rough edges
        // not very economical, could be improved
        val matrix = Matrix4fArrayList()
        matrix.scale(w.toFloat() / GFX.windowWidth, h.toFloat() / GFX.windowHeight, 1f)
        GFX.drawMode = ShaderPlus.DrawMode.COLOR
        color.w /= 25f
        for (dx in 0 until 5) {
            for (dy in 0 until 5) {
                draw3DCircle(null, 0.0, matrix, innerRadius, startDegrees, endDegrees, color)
            }
        }
    }

    fun posSize(shader: Shader, x: Int, y: Int, w: Int, h: Int) {
        shader.v2("pos", (x - GFX.windowX).toFloat() / GFX.windowWidth, 1f - (y - GFX.windowY).toFloat() / GFX.windowHeight)
        shader.v2("size", w.toFloat() / GFX.windowWidth, -h.toFloat() / GFX.windowHeight)
    }

    fun posSize(shader: Shader, x: Float, y: Float, w: Float, h: Float) {
        shader.v2("pos", (x - GFX.windowX) / GFX.windowWidth, 1f - (y - GFX.windowY) / GFX.windowHeight)
        shader.v2("size", w / GFX.windowWidth, -h / GFX.windowHeight)
    }

    fun draw2D(texture: VFrame) {

        if (!texture.isLoaded) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().shader

        GFX.check()

        shader.use()
        shader.v1("filtering", Filtering.LINEAR.id)
        shader.v2("textureDeltaUV", 1f / texture.w, 1f / texture.h)
        shader.m4x4("transform", Matrix4f())
        shader.v4("tint", 1f, 1f, 1f, 1f)
        shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v1("uvProjection", UVProjection.Planar.id)

        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        if (shader == ShaderLib.shader3DYUV.shader) {
            val w = texture.w
            val h = texture.h
            shader.v2("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
        }

        UVProjection.Planar.getBuffer().draw(shader)
        GFX.check()

    }

}