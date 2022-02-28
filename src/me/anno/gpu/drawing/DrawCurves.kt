package me.anno.gpu.drawing

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.Maths.length
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI

// inspired by https://www.shadertoy.com/view/XdVBWd
object DrawCurves {

    val quadraticBezierShader = BaseShader(
        "quadraticBezier", "" +
                "${OpenGLShader.attribute} vec2 attr0;\n" + // we need to use a high-def mesh here
                "uniform vec2 pos, size;\n" +
                "uniform float extrusion, tScale;\n" +
                "uniform vec2 p0, p1, p2;\n" + // control points of cubic bezier curve, [0,1]²
                "vec2 rot90(vec2 v){ return vec2(-v.y, v.x); }\n" +
                "vec2 point(float t){\n" +
                "    float s = 1.0-t;\n" +
                "    return p0*s*s + p1*2.0*s*t + p2*t*t;\n" +
                "}\n" +
                "void main(){\n" +
                "   t = .5 + (attr0.x-.5) * tScale;\n" +
                "   float dt = 0.01;\n" +
                "   vec2 p0 = point(t - dt);\n" +
                "   vec2 p1 = point(t);\n" +
                "   vec2 p2 = point(t + dt);\n" +
                "   uv = p1 + rot90(normalize(p2-p0)) * attr0.y * extrusion;\n" +
                "   gl_Position = vec4((pos + uv * size)*2.0-1.0, 0.0, 1.0);\n" +
                "}", listOf(Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V1F, "t")), "" +
                "uniform vec4 color, backgroundColor;\n" +
                "uniform vec2 p0, p1, p2;\n" + // control points of cubic bezier curve
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
                "    return p0*s*s + p1*2.0*s*t + p2*t*t;\n" +
                "}\n" +
                "void main(){\n" +
                "   float dt = 0.01;\n" +
                "   vec2 p0i = point(t-dt);\n" +
                "   vec2 p1i = point(t);\n" +
                "   vec2 p2i = point(t+dt);\n" +
                "   float d0 = sdSegmentSq(uv, p0i, p1i);\n" +
                "   float d1 = sdSegmentSq(uv, p1i, p2i);\n" +
                "   float res = sqrt(min(d0, d1));\n" +
                "   float delta = smoothness * 2.0 * dFdx(uv.x);\n" +
                "   vec4 color = mix(color, backgroundColor, clamp((res-thickness)/delta+.5, 0.0, 1.0));\n" +
                "   if(color.a < 0.004) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    val cubicBezierShader = BaseShader(
        "cubicBezier", "" +
                "${OpenGLShader.attribute} vec2 attr0;\n" + // we need to use a high-def mesh here
                "uniform vec2 pos, size;\n" +
                "uniform float extrusion, tScale;\n" +
                "uniform vec2 p0, p1, p2, p3;\n" + // control points of cubic bezier curve, [0,1]²
                "vec2 rot90(vec2 v){ return vec2(-v.y, v.x); }\n" +
                "vec2 point(float t){\n" +
                "    float s = 1.0-t;\n" +
                "    return p0*s*s*s + p1*3.0*s*s*t + p2*3.0*s*t*t + p3*t*t*t;\n" +
                "}\n" +
                "void main(){\n" +
                "   t = .5 + (attr0.x-.5) * tScale;\n" +
                "   float dt = 0.01;\n" +
                "   vec2 p0 = point(t - dt);\n" +
                "   vec2 p1 = point(t);\n" +
                "   vec2 p2 = point(t + dt);\n" +
                "   uv = p1 + rot90(normalize(p2-p0)) * attr0.y * extrusion;\n" +
                "   gl_Position = vec4((pos + uv * size)*2.0-1.0, 0.0, 1.0);\n" +
                "}", listOf(Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V1F, "t")), "" +
                "uniform vec4 color, backgroundColor;\n" +
                "uniform vec2 p0, p1, p2, p3;\n" + // control points of cubic bezier curve
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
                "    return p0*s*s*s + p1*3.0*s*s*t + p2*3.0*s*t*t + p3*t*t*t;\n" +
                "}\n" +
                "void main(){\n" +
                "   float dt = 0.01;\n" +
                "   vec2 p0i = point(t-dt);\n" +
                "   vec2 p1i = point(t);\n" +
                "   vec2 p2i = point(t+dt);\n" +
                "   float d0 = sdSegmentSq(uv, p0i, p1i);\n" +
                "   float d1 = sdSegmentSq(uv, p1i, p2i);\n" +
                "   float res = sqrt(min(d0, d1));\n" +
                "   float delta = smoothness * 2.0 * dFdx(uv.x);\n" +
                "   vec4 color = mix(color, backgroundColor, clamp((res-thickness)/delta+.5, 0.0, 1.0));\n" +
                "   if(color.a < 0.004) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    fun drawQuadraticBezier(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        thickness: Float,
        color: Int, background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        GFX.check()
        val shader = quadraticBezierShader.value
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4f("color", color)
        shader.v4f("backgroundColor", background)
        shader.v1f("thickness", thickness)
        shader.v1f("smoothness", smoothness)
        shader.v2f("p0", x0, y0)
        shader.v2f("p1", x1, y1)
        shader.v2f("p2", x2, y2)
        // the correct extrusion depends on the curviness and number of mesh subdivisions
        shader.v1f("extrusion", 1.2f * (thickness + smoothness))
        shader.v1f(
            "tScale", if (flatEnds) 1f else
                1f + (thickness + smoothness) * 2f / curveLength(x0, y0, x1, y1, x2, y2)
        )
        SimpleBuffer.flat11x50.draw(shader)
        GFX.check()
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
        GFX.check()
        val shader = cubicBezierShader.value
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4f("color", color)
        shader.v4f("backgroundColor", background)
        shader.v1f("thickness", thickness)
        shader.v1f("smoothness", smoothness)
        shader.v2f("p0", x0, y0)
        shader.v2f("p1", x1, y1)
        shader.v2f("p2", x2, y2)
        shader.v2f("p3", x3, y3)
        // the correct extrusion depends on the curviness and number of mesh subdivisions
        shader.v1f("extrusion", 1.2f * (thickness + smoothness))
        shader.v1f(
            "tScale", if (flatEnds) 1f else
                1f + (thickness + smoothness) * 2f / curveLength(x0, y0, x1, y1, x2, y2, x3, y3)
        )
        SimpleBuffer.flat11x50.draw(shader)
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
            val bx = x0 * s * s + 2f * x1 * s * t + x2 * t * t
            val by = y0 * s * s + 2f * y1 * s * t + y2 * t * t
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
            val bx = x0 * s * s * s + 3f * (x1 * s * s * t + x2 * s * t * t) + x3 * t * t * t
            val by = y0 * s * s * s + 3f * (y1 * s * s * t + y2 * s * t * t) + y3 * t * t * t
            sum += length(bx - ax, by - ay)
            ax = bx
            ay = by
        }
        return sum
    }

    @JvmStatic
    fun main(args: Array<String>) {
        testUI {
            object : Panel(style) {
                override fun tickUpdate() {
                    invalidateDrawing()
                }

                override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                    drawBackground(x0, y0, x1, y1)
                    val s = 300f
                    val dx = (w - s) / 2f
                    val dy = (h - s) / 2f
                    drawCubicBezier(
                        dx, dy,
                        dx + s, dy,
                        dx, dy + s,
                        dx + s, dy + s,
                        10f,
                        -1, backgroundColor,
                        false
                    )
                    drawQuadraticBezier(
                        dx, dy,
                        dx + s, dy,
                        dx + s, dy + s,
                        5f,
                        backgroundColor,
                        backgroundColor and 0xffffff,
                        false
                    )
                }
            }.setWeight(1f)
        }
    }

}