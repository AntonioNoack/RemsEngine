package me.anno.ecs.components.mesh.material.shaders

import me.anno.cache.CacheSection
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.YUVHelper.rgb2yuv
import me.anno.gpu.shader.YUVHelper.yuv2rgb
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileKey
import me.anno.maths.Maths
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import org.joml.Matrix2f
import kotlin.math.cos
import kotlin.math.sin

/**
 * implements https://benedikt-bitterli.me/histogram-tiling/
 * "Histogram-preserving Blending for Randomized Texture Tiling"
 * */
object AutoTileableShader : ECSMeshShader("auto-tileable") {

    // for triangle pattern
    val latToWorld = Matrix2f(cos(Maths.PIf / 3f), sin(Maths.PIf / 3f), 1f, 0f).scale(0.25f)
    val worldToLat = latToWorld.invert(Matrix2f())

    private const val SAMPLE_TILE_INIT = "" +
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
            "       float diff = length(vec4(dFdx(uv),dFdy(uv)))*float(textureSize(T,0).y);\n" +
            "       rgb = textureLod(T, pos, log2(diff));\n" +
            "   }\n"

    const val sampleTile = "" +
            "vec4 textureAnisotropic(sampler2D, vec2, vec2);\n" +
            "float random(vec2);\n" +
            "vec3 rgb2yuv(vec3);\n" +
            "vec4 sampleTile(sampler2D T, sampler2D invLUT, vec2 uv, vec2 seed, vec2 offset) {\n" +
            SAMPLE_TILE_INIT +
            "   vec3 yuv = rgb2yuv(rgb.xyz);\n" +
            "   yuv.x = textureLod(invLUT, vec2(yuv.x, 1.0), 0.0).x;\n" +
            "   return vec4(yuv, rgb.a);\n" +
            "}\n"

    private const val LATTICE_LOGIC = "" +
            "   vec2 lattice = worldToLat*pos;\n" +
            "   vec2 cell = floor(lattice);\n" +
            "   vec2 uv = lattice - cell;\n" +
            "   vec2 v0 = cell;\n" +
            "   if (uv.x + uv.y >= 1.0) {\n" +
            "       v0 += 1.0;\n" +
            "       uv = 1.0 - uv.yx;\n" +
            "   }\n" +
            "   vec2 v1 = cell + vec2(1, 0);\n" +
            "   vec2 v2 = cell + vec2(0, 1);\n"

    private const val UVW_LOGIC = "" +
            "   vec3 uvw = vec3(1.0 - uv.x - uv.y, uv.x, uv.y);\n" +
            "   uvw = uvw*uvw*uvw;\n" +
            "   uvw /= uvw.x + uvw.y + uvw.z;\n"

    const val getTexture = "" +
            "vec4 sampleTile(sampler2D, sampler2D, vec2, vec2, vec2);\n" +
            "vec3 yuv2rgb(vec3);\n" +
            "vec4 sampleAutoTileableTexture(sampler2D T, sampler2D invLUT, vec2 pos) {\n" +
            LATTICE_LOGIC +
            // to do test as tri-planar material
            "   vec4 color0 = sampleTile(T, invLUT, pos, v0, pos - latToWorld*v0);\n" +
            "   vec4 color1 = sampleTile(T, invLUT, pos, v1, pos - latToWorld*v1);\n" +
            "   vec4 color2 = sampleTile(T, invLUT, pos, v2, pos - latToWorld*v2);\n" +
            UVW_LOGIC +
            "   vec4 yuv = uvw.x*color0 + uvw.y*color1 + uvw.z*color2;\n" +
            "   yuv.x = textureLod(invLUT, vec2(yuv.x, 0.0), 0.0).x;\n" +
            "   return vec4(yuv2rgb(yuv.xyz), yuv.a);\n" +
            "}\n"

    const val sampleTileNoLUT = "" +
            "vec4 textureAnisotropic(sampler2D, vec2, vec2);\n" +
            "float random(vec2);\n" +
            "vec4 sampleTileNoLUT(sampler2D T, vec2 uv, vec2 seed, vec2 offset) {\n" +
            SAMPLE_TILE_INIT +
            "   return rgb;\n" +
            "}\n"

    const val getTextureNoLUT = "" +
            "vec4 sampleTileNoLUT(sampler2D, vec2, vec2, vec2);\n" +
            "vec4 sampleAutoTileableTextureNoLUT(sampler2D T, vec2 pos) {\n" +
            LATTICE_LOGIC +
            // to do test as tri-planar material
            "   vec4 color0 = sampleTileNoLUT(T, pos, v0, pos - latToWorld*v0);\n" +
            "   vec4 color1 = sampleTileNoLUT(T, pos, v1, pos - latToWorld*v1);\n" +
            "   vec4 color2 = sampleTileNoLUT(T, pos, v2, pos - latToWorld*v2);\n" +
            UVW_LOGIC +
            "   return uvw.x*color0 + uvw.y*color1 + uvw.z*color2;\n" +
            "}\n"

    val tilingVars = listOf(
        Variable(me.anno.gpu.shader.GLSLType.V1B, "anisotropic"),
        Variable(GLSLType.V3F, "tileOffset"),
        Variable(GLSLType.V3F, "tilingU"),
        Variable(GLSLType.V3F, "tilingV"),
        Variable(GLSLType.S2D, "invLUTTex"),
        Variable(GLSLType.M2x2, "worldToLat"),
        Variable(GLSLType.M2x2, "latToWorld"),
    )

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val vars = createFragmentVariables(key).filter { it.name != "uv" }.toMutableList()
        vars.add(Variable(GLSLType.V1B, "anisotropic"))
        vars.add(Variable(GLSLType.V3F, "tileOffset"))
        vars.add(Variable(GLSLType.V3F, "tilingU"))
        vars.add(Variable(GLSLType.V3F, "tilingV"))
        vars.add(Variable(GLSLType.S2D, "invLUTTex"))
        vars.add(Variable(GLSLType.M2x2, "worldToLat"))
        vars.add(Variable(GLSLType.M2x2, "latToWorld"))
        return listOf(
            ShaderStage(
                "material", vars,
                discardByCullingPlane +
                        // step by step define all material properties
                        "vec3 colorPos = finalPosition - tileOffset;\n" +
                        "vec2 uv = vec2(dot(colorPos, tilingU), sign(finalNormal.y) * dot(colorPos, tilingV));\n" +
                        jitterUVCorrection +
                        "vec4 texDiffuseMap = sampleAutoTileableTexture(diffuseMap, invLUTTex, uv);\n" +
                        "vec4 color = vec4(vertexColor0.rgb, 1.0) * diffuseBase * texDiffuseMap;\n" +
                        "if(color.a < ${1f / 255f}) discard;\n" +
                        "finalColor = color.rgb;\n" +
                        "finalAlpha = color.a;\n" +
                        normalTanBitanCalculation +
                        // todo the LUT depends on the texture ofc, we can't use the same lut everywhere
                        "mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                        /*"if(abs(normalStrength.x) > 0.0){\n" +
                        "   vec3 normalFromTex = sampleAutoTileableTexture(normalMap, invLUTTex, uv).rgb * 2.0 - 1.0;\n" +
                        "        normalFromTex = matMul(tbn, normalFromTex);\n" +
                        // normalize?
                        "   finalNormal = mix(finalNormal, normalFromTex, normalStrength.x);\n" +
                        "}\n" +*/
                        // todo the LUT depends on the texture ofc, we can't use the same lut everywhere
                        "finalEmissive  = emissiveBase;//sampleAutoTileableTexture(emissiveMap, invLUTTex, uv).rgb * emissiveBase;\n" +
                        "finalOcclusion = 0.0;//(1.0 - sampleAutoTileableTexture(occlusionMap, invLUTTex, uv).r) * occlusionStrength;\n" +
                        "#define HAS_ROUGHNESS\n" +
                        "finalMetallic  = metallicMinMax.y;//clamp(mix(metallicMinMax.x, metallicMinMax.y, sampleAutoTileableTexture(metallicMap, invLUTTex, uv).r), 0.0, 1.0);\n" +
                        "finalRoughness = roughnessMinMax.y;//clamp(mix(roughnessMinMax.x, roughnessMinMax.y, sampleAutoTileableTexture(roughnessMap, invLUTTex, uv).r), 0.0, 1.0);\n" +
                        reflectionCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        finalMotionCalculation
            ).add(rgb2yuv).add(yuv2rgb).add(ShaderLib.anisotropic16).add(ShaderFuncLib.randomGLSL)
                .add(getTexture).add(sampleTile).add(RendererLib.getReflectivity)
        )
    }

    object TileMath {


        fun C(sigma: Float) = 1f / Maths.erf(0.5f / (sigma * Maths.SQRT2f))
        fun truncCdfInv(x: Float, sigma: Float = 1f / 6f) =
            0.5f + Maths.SQRT2f * sigma * Maths.erfInv((2f * x - 1f) / C(sigma))

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
            val lutData = ByteArray(iLutRes * 2)
            lutData.fill(-1)

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