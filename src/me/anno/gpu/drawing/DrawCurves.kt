package me.anno.gpu.drawing

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.Maths.length
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing

// inspired by https://www.shadertoy.com/view/XdVBWd "Cubic Bezier - 2D BBox " from Inigo Quilez
object DrawCurves {

    private fun parametricShader(name: String, parametricFunction: String, numParams: Int): BaseShader {
        return BaseShader(
            name, "" +
                    "${OpenGLShader.attribute} vec2 coords;\n" + // we need to use a high-def mesh here
                    "uniform vec2 pos, size;\n" +
                    "uniform mat4 transform;\n" +
                    "uniform float extrusion, tScale;\n" +
                    "uniform vec2 ${(0 until numParams).joinToString { "p$it" }};\n" + // control points of cubic bezier curve, [0,1]²
                    "vec2 rot90(vec2 v){ return vec2(-v.y, v.x); }\n" +
                    "vec2 point(float t){\n" +
                    "    float s = 1.0-t;\n" +
                    "    $parametricFunction;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   t = .5 + (coords.x-.5) * tScale;\n" +
                    "   float dt = 0.01;\n" +
                    "   vec2 p0 = point(t - dt);\n" +
                    "   vec2 p1 = point(t);\n" +
                    "   vec2 p2 = point(t + dt);\n" +
                    "   uv = p1 + rot90(normalize(p2-p0)) * coords.y * extrusion;\n" +
                    "   gl_Position = transform * vec4((pos + uv * size)*2.0-1.0, 0.0, 1.0);\n" +
                    "}", listOf(Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V1F, "t")), "" +
                    "uniform vec4 ${(0 until numParams).joinToString { "c$it" }}, backgroundColor;\n" +
                    "uniform vec2 ${(0 until numParams).joinToString { "p$it" }};\n" + // control points of cubic bezier curve
                    "uniform float thickness, smoothness;\n" +
                    "float length2(in vec2 v) { return dot(v,v); }\n" +
                    "float sdSegmentSq(in vec2 p, in vec2 a, in vec2 b){\n" +
                    "   vec2 pa = p-a, ba = b-a;\n" +
                    "   float h = clamp(dot(pa,ba)/dot(ba,ba), 0.0, 1.0);\n" +
                    "   return length2(pa - ba*h);\n" +
                    "}\n" +
                    "vec2 point(float t){\n" +
                    "    t = clamp(t, 0.0, 1.0);\n" + // for end caps
                    "    float s = 1.0-t;\n" +
                    "    $parametricFunction;\n" +
                    "}\n" +
                    "vec4 point(float t, ${(0 until numParams).joinToString { "vec4 p$it" }}){\n" +
                    "    t = clamp(t, 0.0, 1.0);\n" + // for end caps
                    "    float s = 1.0-t;\n" +
                    "    $parametricFunction;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   float dt = 0.01;\n" +
                    "   vec2 p0i = point(t-dt);\n" +
                    "   vec2 p1i = point(t);\n" +
                    "   vec2 p2i = point(t+dt);\n" +
                    "   float d0 = sdSegmentSq(uv, p0i, p1i);\n" +
                    "   float d1 = sdSegmentSq(uv, p1i, p2i);\n" +
                    "   float res = sqrt(min(d0, d1));\n" +
                    "   float delta = smoothness * dFdx(uv.x);\n" +
                    "   vec4 color = sqrt(point(t,${(0 until numParams).joinToString { "c$it" }}));\n" +
                    "        color = mix(color,\n" +
                    "           vec4(mix(color.rgb, backgroundColor.rgb, backgroundColor.a), backgroundColor.a),\n" +
                    "           clamp((res-thickness)/delta+.5, 0.0, 1.0));\n" +
                    "   if(color.a < 0.004) discard;\n" +
                    "   gl_FragColor = color;\n" +
                    "}"
        )
    }

    val quadraticBezierShader = parametricShader("quadraticBezier", "return (s*s)*p0+(2.0*s*t)*p1+(t*t)*p2", 3)
    val cubicBezierShader = parametricShader("cubicBezier", "return (s*s*s)*p0+(3.0*s*t)*(p1*s+p2*t)+(t*t*t)*p3", 4)
    val quartBezierShader =
        parametricShader("quartBezier", "return (s*s*s*s)*p0+(4.0*s*t)*(p1*s*s+p3*t*t)+6.0*s*s*t*t*p2+(t*t*t*t)*p4", 5)

    fun drawLine(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        thickness: Float,
        color: Int, background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        // a little overkill ^^, could use its own shader
        drawQuadraticBezier(
            x0, y0, (x0 + x1) * 0.5f, (y0 + y1) * 0.5f, x1, y1,
            thickness, color, background, flatEnds, smoothness
        )
    }

    fun drawQuadraticBezier(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        thickness: Float,
        color: Int, background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        drawQuadraticBezier(
            x0, y0, color, x1, y1, color, x2, y2, color,
            thickness, background, flatEnds, smoothness
        )
    }

    fun drawQuadraticBezier(
        x0: Float, y0: Float, c0: Int,
        x1: Float, y1: Float, c1: Int,
        x2: Float, y2: Float, c2: Int,
        thickness: Float,
        background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        GFX.check()
        val shader = quadraticBezierShader.value
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4fs("c0", c0)
        shader.v4fs("c1", c1)
        shader.v4fs("c2", c2)
        shader.v4f("backgroundColor", background)
        shader.v1f("thickness", thickness)
        shader.v1f("smoothness", smoothness)
        shader.v2f("p0", x0, y0)
        shader.v2f("p1", x1, y1)
        shader.v2f("p2", x2, y2)
        // the correct extrusion depends on the curviness and number of mesh subdivisions
        shader.v1f("extrusion", 1.2f * (thickness + smoothness))
        val curveLength = curveLength(x0, y0, x1, y1, x2, y2)
        shader.v1f(
            "tScale", if (flatEnds) 1f else
                1f + (thickness + smoothness) * 2f / curveLength
        )
        getBezierBuffer(curveLength).draw(shader)
        GFX.check()
    }

    private fun getBezierBuffer(length: Float): StaticBuffer {
        // if the segment is short or straight, use less steps
        return when {
            length < 6f -> SimpleBuffer.flat11x3
            length < 12f -> SimpleBuffer.flat11x6
            length < 24f -> SimpleBuffer.flat11x12
            length < 50f -> SimpleBuffer.flat11x25
            else -> SimpleBuffer.flat11x50
        }
    }

    fun drawCubicBezier(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        thickness: Float,
        color: Int, background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        drawCubicBezier(
            x0, y0, color,
            x1, y1, color,
            x2, y2, color,
            x3, y3, color,
            thickness, background, flatEnds, smoothness
        )
    }

    fun drawCubicBezier(
        x0: Float, y0: Float, c0: Int,
        x1: Float, y1: Float, c1: Int,
        x2: Float, y2: Float, c2: Int,
        x3: Float, y3: Float, c3: Int,
        thickness: Float,
        background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        GFX.check()
        val shader = cubicBezierShader.value
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4fs("c0", c0)
        shader.v4fs("c1", c1)
        shader.v4fs("c2", c2)
        shader.v4fs("c3", c3)
        shader.v4f("backgroundColor", background)
        shader.v1f("thickness", thickness)
        shader.v1f("smoothness", smoothness)
        shader.v2f("p0", x0, y0)
        shader.v2f("p1", x1, y1)
        shader.v2f("p2", x2, y2)
        shader.v2f("p3", x3, y3)
        // the correct extrusion depends on the curviness and number of mesh subdivisions
        shader.v1f("extrusion", 1.2f * (thickness + smoothness))
        val curveLength = curveLength(x0, y0, x1, y1, x2, y2, x3, y3)
        shader.v1f(
            "tScale", if (flatEnds) 1f else
                1f + (thickness + smoothness) * 2f / curveLength
        )
        getBezierBuffer(curveLength).draw(shader)
        GFX.check()
    }

    fun drawQuartBezier(
        x0: Float, y0: Float, c0: Int,
        x1: Float, y1: Float, c1: Int,
        x2: Float, y2: Float, c2: Int,
        x3: Float, y3: Float, c3: Int,
        x4: Float, y4: Float, c4: Int,
        thickness: Float,
        background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        GFX.check()
        val shader = quartBezierShader.value
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4fs("c0", c0)
        shader.v4fs("c1", c1)
        shader.v4fs("c2", c2)
        shader.v4fs("c3", c3)
        shader.v4fs("c4", c4)
        shader.v4f("backgroundColor", background)
        shader.v1f("thickness", thickness)
        shader.v1f("smoothness", smoothness)
        shader.v2f("p0", x0, y0)
        shader.v2f("p1", x1, y1)
        shader.v2f("p2", x2, y2)
        shader.v2f("p3", x3, y3)
        shader.v2f("p4", x4, y4)
        // the correct extrusion depends on the curviness and number of mesh subdivisions
        shader.v1f("extrusion", 1.2f * (thickness + smoothness))
        val curveLength = curveLength(x0, y0, x1, y1, x2, y2, x3, y3, x4, y4)
        shader.v1f(
            "tScale", if (flatEnds) 1f else
                1f + (thickness + smoothness) * 2f / curveLength
        )
        getBezierBuffer(curveLength).draw(shader)
        GFX.check()
    }

    fun curveLength(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        steps: Int = 5
    ): Float {
        var sum = 0f
        var ax = x0
        var ay = y0
        for (i in 0 until steps) {
            val t = i / (steps - 1f)
            val s = 1f - t
            val v0 = s * s
            val v1 = 2f * s * t
            val v2 = t * t
            val bx = x0 * v0 + x1 * v1 + x2 * v2
            val by = y0 * v0 + y1 * v1 + y2 * v2
            sum += length(bx - ax, by - ay)
            ax = bx
            ay = by
        }
        return sum
    }

    fun curveLength(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        steps: Int = 10
    ): Float {
        var sum = 0f
        var ax = x0
        var ay = y0
        for (i in 0 until steps) {
            val t = i / (steps - 1f)
            val s = 1f - t
            val v0 = s * s * s
            val v1 = 3f * s * s * t
            val v2 = 3f * s * t * t
            val v3 = t * t * t
            val bx = x0 * v0 + x1 * v1 + x2 * v2 + x3 * v3
            val by = y0 * v0 + y1 * v1 + y2 * v2 + y3 * v3
            sum += length(bx - ax, by - ay)
            ax = bx
            ay = by
        }
        return sum
    }

    fun curveLength(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float,
        steps: Int = 15
    ): Float {
        var sum = 0f
        var ax = x0
        var ay = y0
        for (i in 0 until steps) {
            val t = i / (steps - 1f)
            val s = 1f - t
            val v0 = s * s * s * s
            val v1 = 4f * s * s * s * t
            val v2 = 6f * s * s * t * t
            val v3 = 4f * s * t * t * t
            val v4 = t * t * t * t
            val bx = x0 * v0 + x1 * v1 + x2 * v2 + x3 * v3 + x4 * v4
            val by = y0 * v0 + y1 * v1 + y2 * v2 + y3 * v3 + y4 * v4
            sum += length(bx - ax, by - ay)
            ax = bx
            ay = by
        }
        return sum
    }

    @JvmStatic
    fun main(args: Array<String>) {
        testDrawing {
            it.drawBackground(it.x, it.y, it.x + it.w, it.y + it.h)
            val s = 300f
            val dx = (it.w - s) / 2f
            val dy = (it.h - s) / 2f
            val bg = it.backgroundColor
            drawCubicBezier(
                dx, dy,
                dx + s, dy,
                dx, dy + s,
                dx + s, dy + s,
                10f,
                -1, bg,
                false
            )
            drawQuadraticBezier(
                dx, dy,
                dx + s, dy,
                dx + s, dy + s,
                5f,
                0x777777 or black,
                0x777777,
                false
            )
            drawLine(
                dx, dy,
                dx + s, dy + s,
                5f,
                0xff0000 or black,
                0xff0000,
                false
            )
        }
    }

}