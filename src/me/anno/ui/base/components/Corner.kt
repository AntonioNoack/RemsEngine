package me.anno.ui.base.components

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.FlatShaders.flatShader
import me.anno.gpu.shader.ShaderLib.simpleVertexShader
import me.anno.gpu.shader.ShaderLib.simplestVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.utils.Color.a
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Corner {

    private fun corner(mx: Boolean, my: Boolean): StaticBuffer {

        val sides = 50
        val buffer = StaticBuffer(cornerAttr, 3 * sides)
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

    private val cornerAttr = listOf(Attribute("attr0", 2))
    val topLeft = corner(true, my = true)
    val topRight = corner(false, my = true)
    val bottomLeft = corner(true, my = false)
    val bottomRight = corner(false, my = false)

    val roundedShader = BaseShader(
        "roundedRectShader", simpleVertexShader, uvList, "" +
                "uniform vec4 centerColor, outlineColor, backgroundColor;\n" +
                "uniform vec4 radius;\n" +
                "uniform vec2 size2;\n" +
                "uniform float smoothness, outlineThickness;\n" +
                // https://www.iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm
                "float sdRoundedBox(vec2 p, vec2 b, vec4 r){\n" +
                "   r.xy = (p.x>0.0) ? r.xy : r.zw;\n" +
                "   r.x  = (p.y>0.0) ? r.x  : r.y;\n" +
                "   vec2 q = abs(p)-b+r.x;\n" +
                "   return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r.x;\n" +
                "}\n" +
                "void main(){\n" +
                "   vec2 uv2 = (uv-0.5)*size2;\n" +
                "   float sdf = sdRoundedBox(uv2, size2*0.5, radius);\n" +
                "   float delta = smoothness * dFdx(uv2.x);\n" +
                "   float f0 = clamp((sdf+outlineThickness)/delta+0.5, 0.0, 1.0);\n" +
                "   float f1 = clamp((sdf)/delta+0.5, 0.0, 1.0);\n" +
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
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
        shader.v4f("centerColor", centerColor)
        shader.v4f("outlineColor", if (outlineThickness > 0f) outlineColor else centerColor)
        shader.v4f("backgroundColor", backgroundColor)
        shader.v1f("outlineThickness", outlineThickness)
        shader.v4f("radius", tr, br, tl, bl)
        shader.v1f("smoothness", smoothness)
        shader.v2f("size2", w.toFloat(), h.toFloat())
        GFX.flat01.draw(shader)

        /*if (w > 0 && h > 0) {
            if (radius > 0 && (topLeft || topRight || bottomLeft || bottomRight)) {

                // val bottomFree = !bottomLeft && !bottomRight
                // val topFree = !topLeft && !topRight
                // val leftFree = !topLeft && !bottomLeft
                // val rightFree = !topRight && !bottomRight

                // todo optimize to use less draw calls if 1 or 2 corners are drawn only

                GFXx2D.flatColor(centerColor)

                // draw center part
                drawRect(x, y + radius, w, h - radius * 2)

                // draw top bar
                drawRect(x + radius, y, w - 2 * radius, radius)
                // draw bottom bar
                drawRect(x + radius, y + h - radius, w - 2 * radius, radius)

                // draw corners
                if (topLeft) drawCorner(x, y, radius, radius, this.topLeft)
                else drawRect(x, y, radius, radius)

                if (topRight) drawCorner(x + w - radius, y, radius, radius, this.topRight)
                else drawRect(x + w - radius, y, radius, radius)

                if (bottomLeft) drawCorner(x, y + h - radius, radius, radius, this.bottomLeft)
                else drawRect(x, y + h - radius, radius, radius)

                if (bottomRight) drawCorner(x + w - radius, y + h - radius, radius, radius, this.bottomRight)
                else drawRect(x + w - radius, y + h - radius, radius, radius)

            } else drawRect(x, y, w, h, centerColor)
        }*/
    }

    fun drawCorner(x: Int, y: Int, w: Int, h: Int, corner: StaticBuffer) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        corner.draw(shader)
        GFX.check()
    }

    fun drawCorner(x: Int, y: Int, w: Int, h: Int, color: Int, corner: StaticBuffer) {
        if (w == 0 || h == 0 || color.a() <= 0f) return
        GFX.check()
        val shader = flatShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("color", color)
        corner.draw(shader)
        GFX.check()
    }

}