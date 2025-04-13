package me.anno.gpu.drawing

import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.uiVertexShader
import me.anno.gpu.shader.ShaderLib.uiVertexShaderList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.Maths.mix
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.Color.withAlpha
import kotlin.math.min

/**
 * Draws a mix between a rectangle and a circle;
 * subpixel-rendering not implemented, because it's barely noticeable
 * */
object DrawSquircle {

    private val squircleShader = BaseShader(
        "squircleShader", uiVertexShaderList, uiVertexShader, uvList,
        listOf(
            Variable(GLSLType.V4F, "centerColor"),
            Variable(GLSLType.V4F, "outlineColor"),
            Variable(GLSLType.V4F, "backgroundColor"),
            Variable(GLSLType.V2F, "power"),
            Variable(GLSLType.V1F, "hardness"),
        ), "" +
                // https://en.wikipedia.org/wiki/Squircle
                "void main() {\n" +
                "   vec2 uv2 = (uv-0.5)*2.0;\n" +
                "   vec2 uv3 = pow(abs(uv2), power);\n" +

                "   float dx = abs(dFdx(uv3.x)), dy = abs(dFdy(uv3.y));\n" +
                "   float invDelta = hardness / (dx + dy);\n" +
                "   float sdf = (uv3.x + uv3.y - 1.0) * invDelta;\n" +

                "   float factor = clamp(sdf + 0.5, 0.0, 1.0);\n" +
                "   vec4 color = mix(centerColor, backgroundColor, vec4(factor));\n" +
                "   if(color.a <= 0.004) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    fun drawSquircle(
        x: Int, y: Int, w: Int, h: Int,
        powerX: Float, powerY: Float,
        outlineThickness: Float,
        centerColor: Int, outlineColor: Int, backgroundColor: Int,
        smoothness: Float,
    ) {
        drawSquircle(
            x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(),
            powerX, powerY, outlineThickness, centerColor, outlineColor, backgroundColor, smoothness
        )
    }

    fun drawSquircle(
        x: Float, y: Float, w: Float, h: Float,
        powerX: Float, powerY: Float,
        outlineThickness: Float,
        centerColor: Int, outlineColor: Int, backgroundColor: Int,
        smoothness: Float,
    ) {
        val shader = squircleShader.value
        shader.use()
        GFXx2D.noTiling(shader)
        shader.v2f("power", powerX, powerY)
        shader.v1f("hardness", 1f / smoothness)
        if (outlineThickness > 0f) {
            if (outlineThickness * 2f < min(w, h)) {
                val th = outlineThickness + 0f
                draw(shader, x, y, w, h, outlineColor, backgroundColor)
                draw(
                    shader, x + th, y + th, w - th * 2f, h - th * 2f, centerColor,
                    outlineColor.withAlpha(0)
                )
            } else draw(shader, x, y, w, h, outlineColor, backgroundColor)
        } else draw(shader, x, y, w, h, centerColor, backgroundColor)
    }

    private fun draw(
        shader: Shader, x: Float, y: Float, w: Float, h: Float,
        centerColor: Int, backgroundColor: Int,
    ) {
        posSize(shader, x, y, w, h)
        shader.v4f("centerColor", centerColor)
        val bgAlpha = backgroundColor.a01()
        shader.v4f(
            "backgroundColor",
            mix(centerColor.r01(), backgroundColor.r01(), bgAlpha),
            mix(centerColor.g01(), backgroundColor.g01(), bgAlpha),
            mix(centerColor.b01(), backgroundColor.b01(), bgAlpha),
            bgAlpha,
        )
        SimpleBuffer.flat01.draw(shader)
    }
}