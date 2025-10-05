package me.anno.bench.sdftexture

import me.anno.Engine
import me.anno.bench.sdftexture.ContourOptimizer.optimizeContours
import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.Contour.Companion.calculateContours
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField.padding
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField.sdfResolution
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField2
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.image.raw.FloatImage
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.structures.Collections.filterIsInstance2
import kotlin.math.min

/**
 * calculate SDF texture on GPU
 *
 * todo GPU has precision issues, e.g., '5'
 * render whole alphabet as benchmark
 * to do GPU is 3x faster, so use it :)
 * */
fun main() {

    val clock = Clock("SDF-Textures")
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val roundEdges = false
    val maxError = 0.1f

    val font = Font("Verdana", 100f)

    val dstCPU = desktop.getChild("SDF-CPU")
    val dstGPU = desktop.getChild("SDF-GPU")
    dstCPU.tryMkdirs()
    dstGPU.tryMkdirs()
    clock.stop("Init")

    for (ch in 32 until 128) {
        val contours0 = calculateContours(font, ch.toChar().toString())
        val contours1 = optimizeContours(contours0.contours, maxError)
        val field = SignedDistanceField.computeDistances(contours1, roundEdges) ?: continue
        val image = FloatImage(field.w, field.h, 1, field.getDistances()!!)
        image.flipY()
        image.mul(0.2f).write(dstCPU.getChild("$ch.png"))
    }
    clock.stop("CPU-Generation")

    for (ch in 32 until 128) {
        val contours0 = calculateContours(font, ch.toChar().toString())
        val contours1 = optimizeContours(contours0.contours, maxError)
        val field = calculateField(contours1, roundEdges) ?: continue
        val image = FloatImage(field.w, field.h, 1, field.getDistances()!!)
        image.flipY()
        image.mul(0.2f).write(dstGPU.getChild("$ch.png"))
    }
    clock.stop("GPU-Generation")

    Engine.requestShutdown()
}

val linearShader by lazy { createSegmentShader(true) }
val quadraticShader by lazy { createSegmentShader(false) }

fun calculateField(contours: List<Contour>, roundEdges: Boolean): SignedDistanceField2? {
    if (contours.isEmpty()) return null
    val field = SignedDistanceField2(contours, roundEdges, sdfResolution, padding, false)

    val fb0 = Framebuffer("sdf0", field.w, field.h, 1, TargetType.Float32x1, DepthBufferType.NONE)
    val fb1 = Framebuffer("sdf1", field.w, field.h, 1, TargetType.Float32x1, DepthBufferType.NONE)

    var src: Framebuffer?
    val linearSegments = field.segments.filterIsInstance2(LinearSegment::class)
    val quadraticSegments = field.segments.filterIsInstance2(QuadraticSegment::class)

    src = drawShader(linearShader, field, linearSegments, null, fb0, fb1)
    src = drawShader(quadraticShader, field, quadraticSegments, src, fb0, fb1)

    val solution = FloatArray(field.w * field.h)
    val tex = src!!.getTexture0() as Texture2D
    tex.readFloatPixels(0, 0, tex.width, tex.height, solution)

    fb0.destroy()
    fb1.destroy()

    field.distancesI = solution
    return field
}

fun createSegmentShader(isLinear: Boolean): Shader {
    return Shader(
        if (isLinear) "linear" else "quadratic", coordsList, coordsUVVertexShader, uvList, listOf(
            Variable(GLSLType.V4F, "bounds"),
            Variable(GLSLType.V1I, "numSegments"),
            Variable(GLSLType.V2F, "segments", batchSize * 2),
            Variable(GLSLType.V1F, "maxDistance"),
            Variable(GLSLType.S2D, "srcTexture"),
            Variable(GLSLType.V1B, "hasSrcTexture"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                (if (isLinear) linear else quadratic).joinToString("") +
                "void main(){\n" +
                "   vec2 origin = mix(bounds.xy,bounds.zw,uv);\n" +
                "   vec2 bestDistance = hasSrcTexture ?\n" +
                "       texture(srcTexture,uv).xy :\n" +
                "       vec2(maxDistance, 0.0);\n" +
                "   for(int i=0;i<numSegments;i++){\n" +
                (if (isLinear)
                    "" +
                            "vec2 p0 = segments[i*2];\n" +
                            "vec2 p1 = segments[i*2+1];\n" else
                    "" +
                            "vec2 p0 = segments[i*3];\n" +
                            "vec2 p1 = segments[i*3+1];\n" +
                            "vec2 p2 = segments[i*3+2];\n") +
                "       vec2 candidate = getSignedDistance(p0,p1${if (isLinear) "" else ",p2"},origin);\n" +
                "       float bestAbs = abs(bestDistance.x);\n" +
                "       float candidateAbs = abs(candidate.x);\n" +
                "       if(distLessThan(candidate, bestDistance)) {\n" +
                "           bestDistance = candidate;\n" +
                "       }\n" +
                "   }\n" +
                "   result = vec4(bestDistance,0.0,1.0);\n" +
                "}\n"
    )
}

val batchSize = 64
val segment1 = FloatArray(batchSize * 6)

fun drawShader(
    shader: Shader, field: SignedDistanceField2, segments: List<EdgeSegment>,
    src: Framebuffer?, fb0: Framebuffer, fb1: Framebuffer,
): Framebuffer? {
    var dst = src
    for (i in segments.indices step batchSize) {
        val subSegments = segments.subList(i, min(i + batchSize, segments.size))
        dst = drawShader1(shader, field, subSegments, dst, fb0, fb1)
    }
    return dst
}

fun drawShader1(
    shader: Shader, field: SignedDistanceField2, segments: List<EdgeSegment>,
    src: Framebuffer?, fb0: Framebuffer, fb1: Framebuffer
): Framebuffer {
    val useFb0 = src == fb1
    val target = if (useFb0) fb0 else fb1
    useFrame(target) {
        drawShader(shader, field, segments, src?.getTexture0())
    }
    return target
}

fun drawShader(
    shader: Shader, field: SignedDistanceField2, segments: List<EdgeSegment>,
    srcTexture: ITexture2D?
) {
    shader.use()
    // y is flipped for OpenGL
    shader.v4f("bounds", field.minX, field.maxY, field.maxX, field.minY)
    shader.v1i("numSegments", segments.size)

    for (i in segments.indices) {
        when (val segment = segments[i]) {
            is LinearSegment -> {
                segment.p0.get(segment1, i * 4)
                segment.p1.get(segment1, i * 4 + 2)
            }
            is QuadraticSegment -> {
                segment.p0.get(segment1, i * 6)
                segment.p1.get(segment1, i * 6 + 2)
                segment.p2.get(segment1, i * 6 + 4)
            }
            else -> throw NotImplementedError()
        }
    }

    shader.v2fs("segments", segment1)
    shader.v1f("maxDistance", field.maxDistance)
    shader.v1b("hasSrcTexture", srcTexture != null)
    (srcTexture ?: TextureLib.whiteTexture)
        .bindTrulyNearest(shader, "srcTexture")
    flat01.draw(shader)
}

val cbrt = "" +
        "float cbrt(float f){\n" +
        "   return pow(f,1.0/3.0);\n" +
        "}\n"

val distLessThan = "" +
        "bool distLessThan(vec2 a, vec2 b){\n" +
        "   float aa = abs(a.x), ab = abs(b.x);\n" +
        "   return aa == ab ? a.y < b.y : aa < ab;\n" +
        "}\n"

val cross = "" +
        "float cross2d(vec2 a, vec2 b){\n" +
        "   return a.x*b.y - a.y*b.x;\n" +
        "}\n"

val angleCos = "" +
        "float angleCos(vec2 a, vec2 b){\n" +
        "   return dot(a,b) / sqrt(max(dot(a,a) * dot(b,b), 0.0));\n" +
        "}\n"

val absAngleCos = "" +
        "float absAngleCos(vec2 a, vec2 b){\n" +
        "   return abs(angleCos(a,b));\n" +
        "}\n"

val nonZeroSign = "" +
        "float nonZeroSign(float f){\n" +
        "   return f >= 0.0 ? 1.0 : -1.0;\n" +
        "}\n"

val linear = listOf(
    distLessThan, cross, cbrt,
    angleCos, absAngleCos, nonZeroSign,
    "" +
            "vec2 getOrthoNormal(vec2 self) {\n" +
            "   float length = length(self);\n" +
            "   return length == 0.0 ? vec2(0.0, -1.0) : vec2(self.y,-self.x) / length;\n" +
            "}\n",
    "" +
            "vec2 getSignedDistance(vec2 p0, vec2 p1, vec2 origin) {" +
            "   vec2 aq = origin - p0;\n" +
            "   vec2 ab = p1 - p0;\n" +
            "   float outT = dot(aq,ab) / dot(ab,ab);\n" +
            "   if(outT > 0.0 && outT < 1.0) {\n" +
            "       vec2 orthoNormal = getOrthoNormal(ab);\n" +
            "       float orthoDistance = dot(orthoNormal,aq);\n" +
            "       return vec2(orthoDistance, 0.0);\n" +
            "   } else {\n" +
            "       vec2 eqRef = outT > 0.5 ? p1 : p0;\n" +
            "       float endpointDistance = length(eqRef - origin);\n" +
            "       float distance = nonZeroSign(cross2d(aq, ab)) * endpointDistance;\n" +
            "       float dotDistance = absAngleCos(ab, eqRef - origin);\n" +
            "       return vec2(distance, dotDistance);\n" +
            "   }\n" +
            "}\n"
)

val quadratic = listOf(
    distLessThan, cross, cbrt,
    angleCos, absAngleCos, nonZeroSign,
    "" +
            "int solveQuadratic(out float[3] dst, float a, float b, float c) {\n" +
            "   const float TOO_LARGE_RATIO = 1e9;\n" +
            "   if (a == 0.0 || abs(b) + abs(c) > TOO_LARGE_RATIO * abs(a)) {\n" +
            "       if (b == 0.0 || abs(c) > TOO_LARGE_RATIO * abs(b)) {\n" +
            "           return c == 0.0 ? -1 : 0;\n" +
            "       }\n" +
            "       dst[0] = -c / b;\n" +
            "       return 1;\n" +
            "   }\n" +
            "   float div = b * b - 4.0 * a * c;\n" +
            "   if (div > 0.0) {\n" +
            "       div = sqrt(div);\n" +
            "       dst[0] = (-b + div) / (2.0 * a);\n" +
            "       dst[1] = (-b - div) / (2.0 * a);\n" +
            "       return 2;\n" +
            "   } else if (div == 0.0) {\n" +
            "       dst[0] = -b / (2.0 * a);\n" +
            "       return 1;\n" +
            "   } else return 0;\n" +
            "}\n",
    "" +
            "int solveCubicNormed(out float[3] dst, float a0, float b, float c) {\n" +
            "   const float TAU = 6.28318530718;\n" +
            "   float a = a0;\n" +
            "   float a2 = a * a;\n" +
            "   float q = (a2 - 3.0 * b) / 9.0;\n" +
            "   float r = (a * (2.0 * a2 - 9.0 * b) + 27.0 * c) / 54.0;\n" +
            "   float r2 = r * r;\n" +
            "   float q3 = q * q * q;\n" +
            "   if (r2 < q3) {\n" +
            "       float t = r / sqrt(q3);\n" +
            "       if (t < -1.0) t = -1.0;\n" +
            "       if (t > 1.0) t = 1.0;\n" +
            "       t = acos(t);\n" +
            "       a /= 3.0;\n" +
            "       q = -2.0 * sqrt(q);\n" +
            "       dst[0] = q * cos(t / 3f) - a;\n" +
            "       dst[1] = q * cos((t + TAU) / 3.0) - a;\n" +
            "       dst[2] = q * cos((t - TAU) / 3.0) - a;\n" +
            "       return 3;\n" +
            "   } else {\n" +
            "       float a3 = -cbrt(abs(r) + sqrt(r2 - q3));\n" +
            "       if (r < 0.0) a3 = -a3;\n" +
            "       float b3 = a3 == 0.0 ? 0.0 : q / a3;\n" +
            "       a /= 3.0;\n" +
            "       dst[0] = a3 + b3 - a;\n" +
            "       dst[1] = -0.5f * (a3 + b3) - a;\n" +
            "       float cond = +0.5f * sqrt(3f) * (a3 - b3);\n" +
            "       return abs(cond) < 1e-14 ? 2 : 1;\n" +
            "   }\n" +
            "}\n",
    "" +
            "int solveCubic(out float[3] dst, float a, float b, float c, float d) {\n" +
            "   const float TOO_LARGE_RATIO = 1e9;\n" +
            "   if (abs(a) > 1e-7) {\n" +
            "       float bn = b / a;\n" +
            "       float cn = c / a;\n" +
            "       float dn = d / a;\n" +
            "       if (abs(bn) < TOO_LARGE_RATIO && abs(cn) < TOO_LARGE_RATIO && abs(dn) < TOO_LARGE_RATIO)\n" +
            "           return solveCubicNormed(dst, bn, cn, dn);\n" +
            "   }\n" +
            "   return solveQuadratic(dst, b, c, d);\n" +
            "}",
    "" +
            "vec2 getDirectionAt(vec2 p0, vec2 p1, vec2 p2, float t) {\n" +
            "   float b = 1.0 - t;\n" +
            "   float b2 = b * b;\n" +
            "   float a2 = t * t;\n" +
            "   float ba = b - t;\n" +
            "   vec2 dst = -p0 * b2 + p1 * ba + p2 * a2;\n" +
            "   if (dot(dst,dst) < 1e-12) return p2-p0;\n" +
            "   return dst;\n" +
            "}\n",
    "" +
            "vec2 getSignedDistance(vec2 p0, vec2 p1, vec2 p2, vec2 origin) {\n" +
            "   vec2 qa = p0 - origin;\n" +
            "   vec2 ab = p1 - p0;\n" +
            "   vec2 br = p2 - p1 - ab;\n" + // p2-2*p1+p0

            "   float a = dot(br,br);\n" +
            "   float b = 3.0 * dot(ab,br);\n" +
            "   float c = 2.0 * dot(ab,ab) + dot(qa,br);\n" +
            "   float d = dot(qa,ab);\n" +
            "   float tmp3[3];\n" +
            "   int solutions = solveCubic(tmp3, a, b, c, d);\n" +

            "   vec2 epDir = getDirectionAt(p0,p1,p2,0.0);\n" +
            "   float minDistance = nonZeroSign(cross2d(epDir,qa)) * length(qa);\n" +
            "   float t = -dot(qa,epDir) / dot(epDir,epDir);\n" +

            "   epDir = getDirectionAt(p0,p1,p2,1.0);\n" +
            "   float distance = distance(p2,origin);\n" +
            "   if (distance < abs(minDistance)) {\n" +
            "       float cross1 = cross2d(epDir, p2-origin);\n" +
            "       minDistance = cross1 >= 0.0 ? distance : -distance;\n" +
            "       t = dot(origin-p1, epDir) / dot(epDir,epDir);\n" +
            "   }\n" +

            "   for (int i=0;i<solutions;i++) {\n" +
            "        float tmpI = tmp3[i];\n" +
            "        if (tmpI > 0.0 && tmpI < 1.0) {\n" +
            "            float tmpI2 = tmpI * tmpI;\n" +
            "            vec2 qe = p0 + ab * (2.0 * tmpI) + br * (tmpI * tmpI) - origin;\n" +
            "            float distance2 = length(qe);\n" +
            "            if (distance2 <= abs(minDistance)) {\n" +
            "                vec2 dir = getDirectionAt(p0,p1,p2,tmpI);\n" +
            "                float cross1 = cross2d(dir,qe);\n" +
            "                minDistance = cross1 >= 0.0 ? distance2 : -distance2;\n" +
            "                t = tmpI;\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +

            "   float dotDistance = \n" +
            "            t >= 0.0 && t <= 1.0 ? 0.0 :\n" +
            "            t < 0.0 ? absAngleCos(getDirectionAt(p0,p1,p2,0.0), qa) :\n" +
            "            absAngleCos(getDirectionAt(p0,p1,p2,1.0), p2-origin);\n" +
            // outT = t;
            "   return vec2(minDistance, dotDistance);\n" +
            "}\n"
)