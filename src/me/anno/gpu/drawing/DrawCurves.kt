package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat11x2
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths
import java.nio.ByteBuffer
import kotlin.math.hypot

// inspired by https://www.shadertoy.com/view/XdVBWd "Cubic Bezier - 2D BBox " from Inigo Quilez
object DrawCurves {

    val lineBatch = object : Batch(
        flat11x2, listOf(
            Attribute("pi0", 2),
            Attribute("pi1", 2),
            Attribute("ci0", AttributeType.UINT8_NORM, 4),
            Attribute("ci1", AttributeType.UINT8_NORM, 4),
            Attribute("bg0", AttributeType.UINT8_NORM, 4),
            Attribute("thickness0", 1),
            Attribute("smoothness0", 1),
            Attribute("flatEnds", 1),
        )
    ) {
        private val lineBatchShader = Shader(
            "lineBatch", listOf(
                Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
                Variable(GLSLType.V4F, "posSize"),
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.V1F, "thickness0", VariableMode.ATTR),
                Variable(GLSLType.V1F, "smoothness0", VariableMode.ATTR),
                Variable(GLSLType.V4F, "bg0", VariableMode.ATTR),
                Variable(GLSLType.V1F, "flatEnds", VariableMode.ATTR),
            ) + vars(VariableMode.ATTR, "i", 2),  // control points of cubic bezier curve, [0,1]²
            "" +// we need to use a high-def mesh here
                    "vec2 rot90(vec2 v){ return vec2(-v.y, v.x); }\n" +
                    "void main(){\n" +
                    "   thickness = thickness0;\n" +
                    "   smoothness = smoothness0;\n" +
                    "   backgroundColor = bg0;\n" +
                    "   float extrusion = thickness0 + smoothness0;\n" +
                    "   float tScale = flatEnds != 0.0 ? 1.0 : 1.0 + extrusion * 2.0 / length(pi1-pi0);\n" + // at least for lines...
                    "   t = .5 + (coords.x-.5) * tScale;\n" +
                    "   float dt = 0.01;\n" +
                    "   vec2 p1x = mix(pi0,pi1,t);\n" +
                    "   uv = p1x + rot90(normalize(pi1-pi0)) * coords.y * extrusion;\n" +
                    "   p0 = pi0;c0 = ci0;\n" +
                    "   p1 = pi1;c1 = ci1;\n" +
                    "   gl_Position = transform * vec4((posSize.xy + uv * posSize.zw)*2.0-1.0, 0.0, 1.0);\n" +
                    "}",
            listOf(
                Variable(GLSLType.V2F, "uv"),
                Variable(GLSLType.V1F, "t"),
                Variable(GLSLType.V4F, "backgroundColor"),
                Variable(GLSLType.V1F, "thickness"),
                Variable(GLSLType.V1F, "smoothness"),
            ) + vars(VariableMode.IN, "", 2),
            emptyList(),
            parametricFShader("return mix(p0,p1,t)", 2)
        )

        override fun bindShader(): Shader {
            val shader = lineBatchShader
            shader.use()
            GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
            return shader
        }
    }

    private fun parametricFShader(parametricFunction: String, numParams: Int): String {
        return "" +
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
                "   vec4 color = sqrt(point(t,${(0 until numParams).joinToString(",") { "c$it" }}));\n" +
                "        color = mix(color,\n" +
                "           vec4(mix(color.rgb, backgroundColor.rgb, backgroundColor.a), backgroundColor.a),\n" +
                "           clamp((res-thickness)/delta+.5, 0.0, 1.0));\n" +
                "   if(color.a < 0.004) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    }

    private fun parametricShader(name: String, parametricFunction: String, numParams: Int): Shader {
        return Shader(
            name, listOf(
                Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
                Variable(GLSLType.V4F, "posSize"),
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.V1F, "extrusion"),
                Variable(GLSLType.V1F, "tScale"),
            ) + (0 until numParams).map {
                Variable(GLSLType.V2F, "p$it")
            },  // control points of cubic bezier curve, [0,1]²
            "" +// we need to use a high-def mesh here
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
                    "   gl_Position = transform * vec4((posSize.xy + uv * posSize.zw)*2.0-1.0, 0.0, 1.0);\n" +
                    "}", listOf(Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V1F, "t")),
            listOf(
                Variable(GLSLType.V4F, "backgroundColor"),
                Variable(GLSLType.V1F, "thickness"),
                Variable(GLSLType.V1F, "smoothness")
            ) + // control points of cubic bezier curve
                    (0 until numParams).map { Variable(GLSLType.V4F, "c$it") } +
                    (0 until numParams).map { Variable(GLSLType.V2F, "p$it") },
            parametricFShader(parametricFunction, numParams)
        )
    }

    private fun vars(type: VariableMode, x: String, numParams: Int): List<Variable> {
        return (0 until numParams).map {
            Variable(GLSLType.V2F, "p$x$it", type)
        } + (0 until numParams).map {
            Variable(GLSLType.V4F, "c$x$it", type)
        }
    }

    val lineShader = parametricShader("line", "return s*p0+t*p1", 2)
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
    ) = drawLine(x0, y0, color, x1, y1, color, thickness, background, flatEnds, smoothness)

    fun ByteBuffer.putRGBA(v: Int): ByteBuffer {
        // argb -> rgba -> abgr
        val agMask = 0xff00ff00.toInt()
        val r = v.shr(16).and(0xff)
        val ag = v.and(agMask)
        val b = v.and(0xff)
        return putInt(ag or r or b.shl(16))
    }

    fun drawLine(
        x0: Float, y0: Float, c0: Int,
        x1: Float, y1: Float, c1: Int,
        thickness: Float,
        background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) {
        if (lineBatch.active) {
            val data = lineBatch.data
            data.putFloat(x0).putFloat(y0)
            data.putFloat(x1).putFloat(y1)
            data.putRGBA(c0).putRGBA(c1).putRGBA(background)
            data.putFloat(thickness).putFloat(smoothness).putFloat(if (flatEnds) 1f else 0f)
            lineBatch.next()
        } else {
            GFX.check()
            val shader = lineShader
            shader.use()
            GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
            shader.v4fSq("c0", c0)
            shader.v4fSq("c1", c1)
            shader.v4f("backgroundColor", background)
            shader.v1f("thickness", thickness)
            shader.v1f("smoothness", smoothness)
            shader.v2f("p0", x0, y0)
            shader.v2f("p1", x1, y1)
            shader.v1f("extrusion", thickness + smoothness)
            shader.v1f(
                "tScale", if (flatEnds) 1f else
                    1f + (thickness + smoothness) * 2f / hypot(x1 - x0, y1 - y0)
            )
            flat11x2.draw(shader)
            GFX.check()
        }
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
        val shader = quadraticBezierShader
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4fSq("c0", c0)
        shader.v4fSq("c1", c1)
        shader.v4fSq("c2", c2)
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
    ) = drawCubicBezier(
        x0, y0, color,
        x1, y1, color,
        x2, y2, color,
        x3, y3, color,
        thickness, background, flatEnds, smoothness
    )

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
        val shader = cubicBezierShader
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4fSq("c0", c0)
        shader.v4fSq("c1", c1)
        shader.v4fSq("c2", c2)
        shader.v4fSq("c3", c3)
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
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float,
        thickness: Float,
        color: Int,
        background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f
    ) = drawQuartBezier(
        x0, y0, color,
        x1, y1, color,
        x2, y2, color,
        x3, y3, color,
        x4, y4, color,
        thickness, background,
        flatEnds, smoothness
    )

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
        val shader = quartBezierShader
        shader.use()
        GFXx2D.posSize(shader, 0f, 0f, 1f, 1f)
        shader.v4fSq("c0", c0)
        shader.v4fSq("c1", c1)
        shader.v4fSq("c2", c2)
        shader.v4fSq("c3", c3)
        shader.v4fSq("c4", c4)
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
        x2: Float, y2: Float
    ): Float {
        val bx = (x0 + x1 + x1 + x2) * 0.25f
        val by = (y0 + y1 + y1 + y2) * 0.25f
        return hypot(bx - x0, by - y0) + hypot(bx - x2, by - y2)
    }

    @Suppress("unused")
    fun curveLength(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        steps: Int
    ): Float {
        var sum = 0f
        var ax = x0
        var ay = y0
        val f = 1f / steps
        for (i in 1 until steps) {
            val t = i * f
            val s = 1f - t
            val v0 = s * s
            val v1 = 2f * s * t
            val v2 = t * t
            val bx = x0 * v0 + x1 * v1 + x2 * v2
            val by = y0 * v0 + y1 * v1 + y2 * v2
            sum += Maths.length(bx - ax, by - ay)
            ax = bx
            ay = by
        }
        sum += hypot(x2 - ax, y2 - ay)
        return sum
    }

    fun curveLength(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        steps: Int = 3
    ): Float {
        var sum = 0f
        var ax = x0
        var ay = y0
        val f = 1f / steps
        for (i in 1 until steps) {
            val t = i * f
            val s = 1f - t
            val v0 = s * s * s
            val v1 = 3f * s * s * t
            val v2 = 3f * s * t * t
            val v3 = t * t * t
            val bx = x0 * v0 + x1 * v1 + x2 * v2 + x3 * v3
            val by = y0 * v0 + y1 * v1 + y2 * v2 + y3 * v3
            sum += hypot(bx - ax, by - ay)
            ax = bx
            ay = by
        }
        sum += hypot(x3 - ax, y3 - ay)
        return sum
    }

    fun curveLength(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float,
        steps: Int = 4
    ): Float {
        var sum = 0f
        var ax = x0
        var ay = y0
        val f = 1f / steps
        for (i in 1 until steps) {
            val t = i * f
            val s = 1f - t
            val v0 = s * s * s * s
            val v1 = 4f * s * s * s * t
            val v2 = 6f * s * s * t * t
            val v3 = 4f * s * t * t * t
            val v4 = t * t * t * t
            val bx = x0 * v0 + x1 * v1 + x2 * v2 + x3 * v3 + x4 * v4
            val by = y0 * v0 + y1 * v1 + y2 * v2 + y3 * v3 + y4 * v4
            sum += hypot(bx - ax, by - ay)
            ax = bx
            ay = by
        }
        sum += hypot(x4 - ax, y4 - ay)
        return sum
    }

}