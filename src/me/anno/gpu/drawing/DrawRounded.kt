package me.anno.gpu.drawing

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.uiVertexShader
import me.anno.gpu.shader.ShaderLib.uiVertexShaderList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a rectangle with rounded corners;
 * subpixel-rendering not implemented -> not worth it
 * */
@Suppress("unused")
object DrawRounded {

    private fun corner(mx: Boolean, my: Boolean): StaticBuffer {

        val sides = 50
        val buffer = StaticBuffer("corners", cornerAttr, 3 * sides)
        fun put(x: Float, y: Float) {
            buffer.put(if (mx) 1 - x else x, if (my) 1 - y else y)
        }

        val angle = (PI * 0.5).toFloat()
        for (i in 0 until sides) {
            val a0 = i * angle / sides
            val a1 = (i + 1) * angle / sides
            put(0f, 0f)
            put(cos(a0), sin(a0))
            put(cos(a1), sin(a1))
        }

        return buffer
    }

    private val cornerAttr = listOf(Attribute("coords", 2))
    val topLeft = corner(true, my = true)
    val topRight = corner(false, my = true)
    val bottomLeft = corner(true, my = false)
    val bottomRight = corner(false, my = false)

    val roundedShader = BaseShader(
        "roundedRectShader", uiVertexShaderList, uiVertexShader, uvList,
        listOf(
            Variable(GLSLType.V4F, "centerColor"),
            Variable(GLSLType.V4F, "outlineColor"),
            Variable(GLSLType.V4F, "backgroundColor"),
            Variable(GLSLType.V4F, "radius"),
            Variable(GLSLType.V2F, "size2"),
            Variable(GLSLType.V1F, "smoothness"),
            Variable(GLSLType.V1F, "outlineThickness")
        ), "" +
                // https://www.iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm
                // todo we could use a squircle, too, if w = h
                "float sdRoundedBox(vec2 p, vec2 b, vec4 r){\n" +
                "   r.xy = (p.x>0.0) ? r.xy : r.zw;\n" +
                "   r.x  = (p.y>0.0) ? r.x  : r.y;\n" +
                "   vec2 q = abs(p)-b+r.x;\n" +
                "   return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r.x;\n" +
                "}\n" +
                "void main(){\n" +
                "   vec2 uv2 = (uv-0.5)*size2;\n" +
                "   float sdf = sdRoundedBox(uv2, size2*0.5, radius);\n" +
                "   float invDelta = 1.0 / (smoothness * dFdx(uv2.x));\n" +
                "   float f0 = clamp((sdf+outlineThickness)*invDelta+0.5, 0.0, 1.0);\n" +
                "   float f1 = clamp((sdf)*invDelta+0.5, 0.0, 1.0);\n" +
                "   vec4 color = mix(centerColor, outlineColor, f0);\n" +
                "   color = mix(color, vec4(mix(color.rgb, backgroundColor.rgb, backgroundColor.a), backgroundColor.a), f1);\n" +
                "   if(color.a <= 0.004) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    fun drawRoundedRect(
        x: Int, y: Int, w: Int, h: Int,
        tr: Float, br: Float,
        tl: Float, bl: Float,
        outlineThickness: Float,
        centerColor: Int,
        outlineColor: Int,
        backgroundColor: Int,
        smoothness: Float,
    ) {
        val shader = roundedShader.value
        shader.use()
        posSize(shader, x, y, w, h)
        GFXx2D.noTiling(shader)
        shader.v4f("centerColor", centerColor)
        shader.v4f("outlineColor", if (outlineThickness > 0f) outlineColor else centerColor)
        shader.v4f("backgroundColor", backgroundColor)
        shader.v1f("outlineThickness", outlineThickness)
        shader.v4f("radius", br, tr, bl, tl)
        shader.v1f("smoothness", smoothness)
        shader.v2f("size2", w.toFloat(), h.toFloat())
        SimpleBuffer.flat01.draw(shader)
    }
}