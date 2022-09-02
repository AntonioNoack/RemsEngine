package me.anno.ecs.components.shaders

import me.anno.cache.CacheSection
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib.anisotropic16
import me.anno.gpu.shader.ShaderLib.rgb2yuv
import me.anno.gpu.shader.ShaderLib.yuv2rgb
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.image.Image
import me.anno.maths.LinearRegression
import me.anno.maths.Maths
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import org.joml.Matrix2f
import org.joml.Vector2d
import kotlin.math.*

/**
 * implements https://benedikt-bitterli.me/histogram-tiling/
 * "Histogram-preserving Blending for Randomized Texture Tiling"
 * */
object AutoTileableShader : ECSMeshShader("auto-tileable") {

    val cache = CacheSection("auto-tileable")

    // for triangle pattern
    val latToWorld = Matrix2f(cos(Maths.PIf / 3f), sin(Maths.PIf / 3f), 1f, 0f).scale(0.25f)
    val worldToLat = Matrix2f(latToWorld).invert()

    const val sampleTile = "" +
            "vec4 textureAnisotropic(sampler2D, vec2, vec2);\n" +
            "float random(vec2);\n" +
            "vec3 rgb2yuv(vec3);\n" +
            "vec4 sampleTile(sampler2D T, vec2 uv, vec2 seed, vec2 offset) {\n" +
            // todo optional random rotation
            // optional ofc
            "   seed *= 0.001;\n" +
            "   vec2 rands = vec2(random(seed), random(vec2(seed.x,-seed.y)));\n" +
            "   vec2 pos = 0.25 + rands*0.5 + offset;\n" + // todo customizable tile size
            "   vec4 rgb;\n" +
            "   if(anisotropic){\n" +
            // 16x anisotropic filtering
            "       rgb = textureAnisotropic(T, pos, uv);\n" +
            "   } else {\n" +
            // less costly, isotropic filtering
            "       float diff = length(vec4(dFdx(uv),dFdy(uv)))*textureSize(T,0).y;\n" +
            "       rgb = textureLod(T, pos, log2(diff));\n" +
            "   }\n" +
            "   vec3 yuv = rgb2yuv(rgb.xyz);\n" +
            "   yuv.x = textureLod(invLUTTex, vec2(yuv.x, 1.0), 0.0).x;\n" +
            "   return vec4(yuv, rgb.a);\n" +
            "}\n"

    const val getTexture = "" +
            "vec4 sampleTile(sampler2D, vec2, vec2, vec2);\n" +
            "vec3 yuv2rgb(vec3);\n" +
            "vec4 getTexture(sampler2D T, vec2 pos) {\n" +
            "   vec2 lattice = worldToLat*pos;\n" +
            "   vec2 cell = floor(lattice);\n" +
            "   vec2 uv = lattice - cell;\n" +
            "   vec2 v0 = cell;\n" +
            "   if (uv.x + uv.y >= 1.0) {\n" +
            "       v0 += 1.0;\n" +
            "       uv = 1.0 - uv.yx;\n" +
            "   }\n" +
            "   vec2 v1 = cell + vec2(1, 0);\n" +
            "   vec2 v2 = cell + vec2(0, 1);\n" +

            // to do test as tri-planar material
            "   vec4 color0 = sampleTile(T, pos, v0, pos - latToWorld*v0);\n" +
            "   vec4 color1 = sampleTile(T, pos, v1, pos - latToWorld*v1);\n" +
            "   vec4 color2 = sampleTile(T, pos, v2, pos - latToWorld*v2);\n" +

            "   vec3 uvw = vec3(1.0 - uv.x - uv.y, uv.x, uv.y);\n" +
            "   uvw = uvw*uvw*uvw;\n" +
            "   uvw /= uvw.x + uvw.y + uvw.z;\n" +

            "   vec4 yuv = uvw.x*color0 + uvw.y*color1 + uvw.z*color2;\n" +

            "   yuv.x = textureLod(invLUTTex, vec2(yuv.x, 0.0), 0.0).x;\n" +
            "   return vec4(yuv2rgb(yuv.xyz), yuv.a);\n" +
            "}\n"

    override fun createFragmentStage(isInstanced: Boolean, isAnimated: Boolean, motionVectors: Boolean): ShaderStage {
        val vars = createFragmentVariables(isInstanced, isAnimated, motionVectors)
        vars.add(Variable(GLSLType.V1B, "anisotropic"))
        vars.add(Variable(GLSLType.V3F, "tileOffset"))
        vars.add(Variable(GLSLType.V3F, "tileDirU"))
        vars.add(Variable(GLSLType.V3F, "tileDirV"))
        vars.add(Variable(GLSLType.V1B, "anisotropic"))
        vars.add(Variable(GLSLType.S2D, "invLUTTex"))
        vars.add(Variable(GLSLType.M2x2, "worldToLat"))
        vars.add(Variable(GLSLType.M2x2, "latToWorld"))
        return ShaderStage(
            "material", vars,
            discardByCullingPlane +
                    // step by step define all material properties
                    "vec3 colorPos = finalPosition - tileOffset;\n" +
                    "vec4 texDiffuseMap = getTexture(diffuseMap, vec2(dot(colorPos, tileDirU), dot(colorPos, tileDirV)));\n" +
                    // "vec4 texDiffuseMap = getTexture(diffuseMap, uv);\n" +
                    "vec4 color = vec4(vertexColor.rgb, 1.0) * diffuseBase * texDiffuseMap;\n" +
                    "if(color.a < ${1f / 255f}) discard;\n" +
                    "finalColor = color.rgb;\n" +
                    "finalAlpha = color.a;\n" +
                    // todo apply mapping to normal map, emissive, metallic and roughness as well
                    normalTanBitanCalculation +
                    normalMapCalculation +
                    emissiveCalculation +
                    occlusionCalculation +
                    metallicCalculation +
                    roughnessCalculation +
                    reflectionPlaneCalculation +
                    v0 + sheenCalculation +
                    clearCoatCalculation +
                    (if (motionVectors) finalMotionCalculation else "")
        ).add(rgb2yuv).add(yuv2rgb).add(anisotropic16).add(noiseFunc)
            .add(getTexture).add(sampleTile)
    }

    object TileMath {
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

        fun dErf(x: Float): Float = 2f * exp(-x * x) / sqrt(Maths.PIf)
        fun erfInv(x: Float): Float {
            var y = 0f
            var ctr = 0
            do {
                val err = erf(y) - x
                y -= err / dErf(y)
            } while (abs(err) > 1e-7f && ctr++ < 32)
            return y
        }

        fun C(sigma: Float): Float = 1f / erf(0.5f / (sigma * Maths.SQRT2F))
        fun truncCdfInv(x: Float, sigma: Float = 1f / 6f) =
            0.5f + Maths.SQRT2F * sigma * erfInv((2f * x - 1f) / C(sigma))

        fun buildYHistogram(image: Image): IntArray {

            val imageW = image.width
            val imageH = image.height
            val hist = IntArray(256)
            for (y in 0 until imageH) {
                for (x in 0 until imageW) {
                    val rgb = image.getRGB(x, y)
                    // tmp.set(rgb.r().toFloat(), rgb.g().toFloat(), rgb.b().toFloat())
                    // hist[clamp(tmp.dot(0.299f, 0.587f, 0.114f).toInt(), 0, 255)]++
                    hist[(rgb.r() * 306 + rgb.g() * 601 + rgb.b() * 117) shr 10]++
                }
            }

            for (i in 1 until 256) {
                hist[i] += hist[i - 1]
            }

            return hist
        }

        fun buildLUT(hist: IntArray): ByteArray {

            // could this be built with a compute shader? maybe :)

            val lut = IntArray(256)
            val invHistMax = 1f / hist.last()

            for (i in 0 until 256) {
                val lutF = truncCdfInv(hist[i] * invHistMax)
                lut[i] = (lutF * 255).toInt()
            }

            val iLutRes = 16
            val lutData = ByteArray(iLutRes * 2) { -1 }

            // how complex is this LUT? can it be packed into a formula maybe? (needing 1 texture less)
            // no, it can be pretty complex, and we'd need its inverse as well

            var lastJ = 0
            for (i in 0 until iLutRes) {
                val f = (i * 255 + 127) / iLutRes
                for (j in lastJ until 256) { // on avg only a few steps thanks to caching
                    if (f < lut[j]) {
                        lutData[i] = j.toByte()
                        lastJ = j
                        break
                    }
                }
            }

            // add forward lut
            for (i in 0 until iLutRes) {
                lutData[i + iLutRes] = lut[i * 256 / iLutRes].toByte()
            }

            return lutData

        }
    }

}