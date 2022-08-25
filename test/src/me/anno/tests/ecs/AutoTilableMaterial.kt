package me.anno.tests.ecs

import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.anisotropic16
import me.anno.gpu.shader.ShaderLib.rgb2yuv
import me.anno.gpu.shader.ShaderLib.yuv2rgb
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.image.ImageCPUCache
import me.anno.input.Input
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.SQRT2F
import me.anno.maths.Maths.clamp
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS.pictures
import me.anno.utils.types.Floats.f3
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.*

fun main() {

    // implements https://benedikt-bitterli.me/histogram-tiling/
    // "Histogram-preserving Blending for Randomized Texture Tiling"

    GFX.forceLoadRenderDoc()

    testUI {

        StudioBase.instance?.enableVSync = true

        // for triangle pattern
        val latToWorld = Matrix2f(cos(PIf / 3f), sin(PIf / 3f), 1f, 0f).scale(0.25f)
        val worldToLat = Matrix2f(latToWorld).invert()

        val input = pictures.getChild("coffee.jpg")

        val image = ImageCPUCache.getImage(input, false)!!

        val randRes = 512
        val randTex = Texture2D("rand", randRes, randRes, 1)
        randTex.createRGBA(ByteArray(randRes * randRes * 4) {
            (Math.random() * 256).toInt().toByte()
        }, false)

        fun erf(x: Float): Float {

            val a1 = +0.2548296f
            val a2 = -0.28449672f
            val a3 = +1.4214138f
            val a4 = -1.4531521f
            val a5 = +1.0614054f
            val p = +0.3275911f

            val sign = x.sign

            val t = 1f / (1f + p * abs(x))
            val y = 1f - ((((a5 * t + a4) * t + a3) * t + a2) * t + a1) * t * exp(-x * x)

            return sign * y
        }

        fun derf(x: Float): Float = 2f * exp(-x * x) / sqrt(PIf)
        fun erfInv(x: Float): Float {
            var y = 0f
            var ctr = 0
            do {
                val err = erf(y) - x
                y -= err / derf(y)
            } while (abs(err) > 1e-7f && ctr++ < 32)
            return y
        }

        fun C(sigma: Float): Float = 1f / erf(0.5f / (sigma * SQRT2F))

        // todo erf() is just a polynomial, and erfInv is just trying to traverse that polynomial;
        // todo surely, we can express truncCdfInv as some simple polynomial
        fun truncCdfInv(x: Float, sigma: Float) = 0.5f + SQRT2F * sigma * erfInv((2f * x - 1f) / C(sigma))

        val imageW = image.width
        val imageH = image.height

        val yv = Vector3f(0.299f, 0.587f, 0.114f).mul(255f)

        val tmp = Vector3f()
        val hist = IntArray(256)
        for (y in 0 until imageH) {
            for (x in 0 until imageW) {
                val rgb = image.getRGB(x, y)
                tmp.set(rgb.r01(), rgb.g01(), rgb.b01())
                hist[clamp(tmp.dot(yv).toInt(), 0, 255)]++
            }
        }

        for (i in 1 until 256) {
            hist[i] += hist[i - 1]
        }

        val lutF = FloatArray(256)
        val lut = IntArray(256)
        val invHistMax = 1f / (imageW * imageH)
        val sigma = 1f / 6f

        for (i in 0..1000) {
            val f = i / 1000f
            println("${f.f3()}: ${truncCdfInv(f, sigma)}")
        }

        for (i in 0 until 256) {
            lutF[i] = truncCdfInv(hist[i] * invHistMax, sigma)
            lut[i] = (lutF[i] * 255).toInt().shl(16)
        }

        val iLutRes = 16
        val lutData = ByteArray(iLutRes * 2) { -1 }

        var lastJ = 0
        for (i in 0 until iLutRes) {
            val f = (i + 0.5f) / iLutRes
            for (j in lastJ until 256) { // O(1) thanks to caching
                if (f < lutF[j]) {
                    lutData[i] = j.toByte()
                    lastJ = j
                    break
                }
            }
        }

        // add forward lut
        for (i in 0 until iLutRes) {
            lutData[i + iLutRes] = lut[i * 256 / iLutRes].shr(16).toByte()
        }

        val invLUTTex = Texture2D("iLut", iLutRes, 2, 1)
        invLUTTex.createMonochrome(lutData, false)

        val patternTex = Texture2D("pattern", imageW, imageH, 1)
        image.createTexture(patternTex, true, false)
        // Texture2D.switchRGB2BGR(yuvImage)
        // patternTex.createRGBA(yuvImage, false)

        val shader = Shader(
            "tileable", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V2F, "offset"),
                Variable(GLSLType.V2F, "scale"),
                Variable(GLSLType.V1F, "invRandRes"),
                Variable(GLSLType.S2D, "randTex"),
                Variable(GLSLType.S2D, "patternTex"),
                Variable(GLSLType.S2D, "invLUTTex"),
                Variable(GLSLType.M2x2, "worldToLat"),
                Variable(GLSLType.M2x2, "latToWorld"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    rgb2yuv + yuv2rgb +
                    anisotropic16 +
                    "vec3 sampleTile(vec2 uv, vec2 vertex, vec2 offset) {\n" +
                    "   vec2 rands = texture(randTex, vertex*invRandRes).xy;\n" +
                    "   vec2 pos = 0.25 + rands.xy*0.5 + offset;\n" +
                    // less costly, isotropic filtering
                    // "   float diff = length(vec4(dFdx(uv),dFdy(uv)))*textureSize(patternTex,0).y;\n" +
                    // "   vec3 rgb = textureLod(patternTex, pos, log2(diff)).rgb;\n" +
                    // 16x anisotropic filtering
                    "   vec3 rgb = textureAnisotropic(patternTex, pos, uv).rgb;\n" +
                    "   vec3 yuv = rgb2yuv(rgb);\n" +
                    "   yuv.x = textureLod(invLUTTex, vec2(yuv.x, 1.0), 0.0).x;\n" +
                    "   return yuv;\n" +
                    "}\n" +
                    "vec3 getTexture(vec2 pos) {\n" +
                    "   vec2 lattice = worldToLat*pos;\n" +
                    "   vec2 cell = floor(lattice);\n" +
                    "   vec2 uv = lattice - cell;\n" +
                    "   \n" +
                    "   vec2 v0 = cell;\n" +
                    "   if (uv.x + uv.y >= 1.0) {\n" +
                    "       v0 += 1.0;\n" +
                    "       uv = 1.0 - uv.yx;\n" +
                    "   }\n" +
                    "   vec2 v1 = cell + vec2(1, 0);\n" +
                    "   vec2 v2 = cell + vec2(0, 1);\n" +

                    // to do test as tri-planar material
                    "   vec3 color0 = sampleTile(pos, v0, pos - latToWorld*v0);\n" +
                    "   vec3 color1 = sampleTile(pos, v1, pos - latToWorld*v1);\n" +
                    "   vec3 color2 = sampleTile(pos, v2, pos - latToWorld*v2);\n" +

                    "   vec3 uvw = vec3(1.0 - uv.x - uv.y, uv.x, uv.y);\n" +
                    "   uvw = uvw*uvw*uvw;\n" +
                    "   uvw /= uvw.x + uvw.y + uvw.z;\n" +

                    "   vec3 yuv = uvw.x*color0 + uvw.y*color1 + uvw.z*color2;\n" +

                    "   yuv.x = textureLod(invLUTTex, vec2(yuv.x, 0.0), 0.0).x;\n" +
                    "   return yuv2rgb(yuv);\n" +
                    "   return yuv;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   vec2 uvPlanar = (uv-0.5) * scale + offset;\n" +
                    // test anisotropy by projecting onto a plane
                    "   vec2 uvProject = (uv-0.5)/(1.0-uv.y) * scale + offset;\n" +
                    "   result = vec4(getTexture(uvProject), 1.0);\n" +
                    "}"
        )
        shader.setTextureIndices("randTex", "patternTex", "invLUTTex")

        var scale = 1f
        val offset = Vector2f()

        lateinit var panel: TestDrawPanel
        object : TestDrawPanel(
            {

                shader.use()

                shader.m2x2("latToWorld", latToWorld)
                shader.m2x2("worldToLat", worldToLat)

                shader.v2f("offset", offset)
                shader.v2f("scale", scale * imageH / imageW * panel.w / panel.h, -scale)
                shader.v1f("invRandRes", 1f / randRes)

                patternTex.bind(shader, "patternTex", GPUFiltering.LINEAR, Clamping.CLAMP)
                invLUTTex.bind(shader, "invLUTTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                randTex.bind(shader, "randTex", GPUFiltering.TRULY_NEAREST, Clamping.REPEAT)

                flat01.draw(shader)

            }
        ) {

            init {
                panel = this
            }

            override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                if (Input.mouseKeysDown.isNotEmpty()) {
                    val scale2 = scale / this.h
                    offset.sub(dx * scale2, dy * scale2)
                }
            }

            override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
                scale *= 1.05f.pow(-dy)
            }

        }
    }

}