package me.anno.gpu.shader.effects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.IndestructibleTexture2D
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.structures.lists.Lists.iff
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.iff
import org.joml.Matrix4f
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// todo use screen-space shadows in RenderLights-pass: if shadow map is unavailable, use SSR instead


// todo this is too dark on some regions on curved region on flat angles
//  - maybe the normals are close to being backwards?
object ScreenSpaceAmbientOcclusion {

    // to do this can become extremely slow with complex geometry
    // (40 fps on a RTX 3070 🤯, where a pure-color scene has 600 fps)
    // why is this soo expensive on my RTX3070?
    // memory limited...

    // could be set lower for older hardware, would need restart
    private val MAX_SAMPLES = max(4, DefaultConfig["gpu.ssao.maxSamples", 512])
    private val sampleKernel = LazyMap(::generateSampleKernel)

    @JvmStatic
    private fun generateSampleKernel(numSamples: Int): IndestructibleTexture2D {
        assertTrue(numSamples > 0)
        val seed = 1234L
        val random = Random(seed)
        var j = 0
        val data = FloatArray(numSamples * 4)
        for (i in 0 until numSamples) {
            val f01 = (i + 1f) / numSamples
            var x: Float
            var y: Float
            var z: Float
            do {
                x = random.nextFloat() * 2f - 1f
                y = random.nextFloat() * 2f - 1f
                z = random.nextFloat() // half sphere
                val dot2 = x * x + y * y + z * z
            } while (dot2 < 0.01f || dot2 > 1f)
            val scale = f01 * f01
            val f = scale / Maths.length(x, y, z)
            data[j++] = x * f
            data[j++] = y * f
            data[j++] = z * f
            j++
        }
        return IndestructibleTexture2D("sampleKernel", numSamples, 1, data)
    }

    // tiled across the screen; e.g., 4x4 texture size
    // because of that, we probably could store it in the shader itself
    // 2*16 values ~ just two matrices
    @Suppress("SameParameterValue")
    private fun generateRandomTexture(random: Random): IndestructibleTexture2D {
        var j = 0
        val numPixels = 16
        val data = ByteArray(numPixels * 4)
        repeat(numPixels) {
            val nx = random.nextFloat() * 2 - 1
            val ny = random.nextFloat() * 2 - 1
            val nz = random.nextFloat() * 2 - 1
            val nf = 127.5f / sqrt(nx * nx + ny * ny + nz * nz)
            val x = clamp(nx * nf + 127.5f, 0f, 255f)
            val y = clamp(ny * nf + 127.5f, 0f, 255f)
            val z = clamp(nz * nf + 127.5f, 0f, 255f)
            data[j++] = (x.toInt().toByte())
            data[j++] = (y.toInt().toByte())
            data[j++] = (z.toInt().toByte())
            data[j++] = (-1)
        }
        return IndestructibleTexture2D("ssao-noise", 4, 4, data)
    }

    private val random4x4 = generateRandomTexture(Random(1234L))

    private val occlusionShaders = LazyList(3 * 8) {
        val base = it.shr(3)
        val normalZW = if (it.hasFlag(1)) "zw" else "xy"
        val depthMask = "xyzw"[it.shr(1).and(3)]
        val multisampling = base.hasFlag(4)
        val ssgi = base.hasFlag(8)
        val srcType = if (multisampling) GLSLType.S2DMS else GLSLType.S2D
        val reflectivityMask = "xyzw"[base.and(3)]
        Shader(
            if (ssgi) "ssgi" else "ssao",
            emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.V1F, "strength"),
                Variable(GLSLType.V1F, "radiusScale"),
                Variable(GLSLType.V1I, "numSamples"),
                Variable(GLSLType.V1I, "mask"),
                Variable(GLSLType.M4x4, "cameraMatrix"),
                Variable(srcType, "finalDepth"),
                Variable(srcType, "finalNormal"),
                Variable(GLSLType.S2D, "sampleKernel"),
                Variable(GLSLType.S2D, "random4x4"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + listOf(
                Variable(GLSLType.S2D, "illuminatedTex"),
                Variable(GLSLType.S2D, "reflectivityTex"),
            ).iff(ssgi) + DepthTransforms.depthVars + listOf(
                // sun uniforms
                Variable(GLSLType.V3F, "sunDirection"),
                Variable(GLSLType.V1F, "sunStrength")
            ), "" +
                    "float dot2(vec3 p){ return dot(p,p); }\n" +
                    ShaderLib.quatRot +
                    DepthTransforms.rawToDepth +
                    DepthTransforms.depthToPosition +
                    ShaderLib.octNormalPacking +
                    "void main() {\n" +
                    (if (multisampling) "" +
                            "   ivec2 texSizeI = textureSize(finalDepth);\n" +
                            "   #define getPixel(tex,uv) texelFetch(tex,clamp(ivec2(uv*vec2(texSizeI)),ivec2(0),texSizeI-1),0)\n"
                    else "#define getPixel(tex,uv) textureLod(tex,uv,0.0)\n") +
                    "   float depth0 = getPixel(finalDepth, uv).$depthMask;\n" +
                    "   vec3 origin = rawDepthToPosition(uv, depth0);\n" +
                    "   float radius = length(origin);\n" +
                    "   if (radius < 1e18) {\n" + // sky and such can be skipped automatically
                    "       radius *= radiusScale;\n" +
                    "       vec3 normal = UnpackNormal(getPixel(finalNormal, uv).$normalZW);\n" +
                    // reverse back sides, e.g., for plants
                    // could be done by the material as well...
                    "       if (dot(origin,normal) > 0.0) normal = -normal;\n" +
                    "       vec3 randomVector = texelFetch(random4x4, ivec2(gl_FragCoord.xy) & mask, 0).xyz * 2.0 - 1.0;\n" +
                    "       vec3 tangent = normalize(randomVector - normal * dot(normal, randomVector));\n" +
                    "       vec3 bitangent = cross(normal, tangent);\n" +
                    (if (ssgi) { // reduce blurriness on smooth surfaces
                        "" +
                                "float roughness = 1.0 - 0.9 * getPixel(reflectivityTex,uv).$reflectivityMask;\n" +
                                "tangent *= roughness;\n" +
                                "bitangent *= roughness;\n"
                    } else "") +
                    "mat3 tbn = mat3(tangent, bitangent, normal);\n" +
                    (if (ssgi) {
                        "vec3 lightSum = vec3(0.0);\n"
                    } else {
                        "" +
                                "float ambientOcclusion = 0.0, sunOcclusion = 0.0;\n" +
                                "bool collectSunOcclusion = sunStrength > 0.0 && dot(normal, sunDirection) > 0.0;\n"
                    }) +
                    "       // [loop]\n" + // hlsl instruction
                    "       for (int i=0;i<numSamples;i++) {\n" +
                    "           vec3 dir0 = texelFetch(sampleKernel, ivec2(i,0), 0).xyz;\n" +
                    "           vec3 dir1 = matMul(tbn, dir0);\n" +
                    "           vec3 position = dir1 * radius + origin;\n" +
                    // project sample position
                    "           vec4 offset = matMul(cameraMatrix, vec4(position, 0.0));\n" +
                    "           offset.xy = offset.xy * 0.5 / offset.w + 0.5;\n" +
                    "           bool isInside = offset.x >= 0.0 && offset.x <= 1.0 && offset.y >= 0.0 && offset.y <= 1.0;\n" +
                    // theoretically, the tutorial also contained this condition, but somehow it
                    // introduces a radius (when radius = 1), where occlusion appears exclusively
                    // && abs(originDepth - sampleDepth) < radius
                    // without it, the result looks approx. the same :)
                    "           if (isInside){\n" +
                    "               float depth1 = getPixel(finalDepth, offset.xy).$depthMask;\n" +
                    "               float sampleDepthSq = dot2(rawDepthToPosition(offset.xy, depth1));\n" +
                    (if (ssgi) {
                        "" +
                                // add light from that surface
                                "   vec3 normal1 = UnpackNormal(getPixel(finalNormal, offset.xy).$normalZW);\n" +
                                // aligned normals have higher contribution -> correct?? kind of a guess...
                                "   float alignmentStrength = 0.6 - 0.4 * dot(normal,normal1);\n" +
                                "   vec3 color1 = getPixel(illuminatedTex,offset.xy).xyz;\n" +
                                "   lightSum += alignmentStrength * color1;\n"
                    } else {
                        "" +
                                "   float sampleTheoDepthSq = dot2(position);\n" +
                                "   ambientOcclusion += step(sampleDepthSq, sampleTheoDepthSq);\n" +

                                // directional sun occlusion approximation:
                                // project a small step from the sample toward the sun and check scene depth there
                                "   if (collectSunOcclusion) {\n" +
                                // move from the sample a bit towards the sun; scale chosen empirically
                                "       vec3 sunProbePos = position + sunDirection * (radius * pow(float(i + 1) / float(numSamples), 2.0));\n" +
                                "       vec4 sunOffset = matMul(cameraMatrix, vec4(sunProbePos, 1.0));\n" +
                                "       sunOffset.xy = sunOffset.xy * 0.5 / sunOffset.w + 0.5;\n" +
                                "       if (sunOffset.x >= 0.0 && sunOffset.x <= 1.0 && sunOffset.y >= 0.0 && sunOffset.y <= 1.0) {\n" +
                                "           float depthSun = getPixel(finalDepth, sunOffset.xy).$depthMask;\n" +
                                "           float sunDepthSq = dot2(rawDepthToPosition(sunOffset.xy, depthSun));\n" +
                                // if the scene depth at the probe is closer than the probe point, there is geometry between
                                // the sample and the sun direction -> sample contributes to sun-occlusion
                                "           sunOcclusion += step(sunDepthSq, dot2(sunProbePos));\n" +
                                "       }\n" +
                                "   }\n"
                    }) +
                    "           }\n" +
                    "       }\n" +
                    if (ssgi) {
                        "result = vec4(lightSum * strength, 1.0);\n"
                    } else {
                        "" +
                                "ambientOcclusion *= strength;\n" +
                                "sunOcclusion = sunStrength * (collectSunOcclusion ? sunOcclusion / float(numSamples) : 1.0);\n" +
                                "result = vec4(ambientOcclusion, sunOcclusion, 0.0, 1.0);\n"
                    } +
                    "   } else {\n" +
                    "       result = vec4(0.0);\n" +
                    "   }\n" +
                    "}"
        ).apply {
            glslVersion = 330
        }
    }

    private const val BLUR_FLAG_DEPTH_MASK = 3
    private const val BLUR_FLAG_HAS_BLUR = 4
    private const val BLUR_FLAG_IS_SSGI = 8
    private const val BLUR_FLAG_ZW_NORMALS = 16
    private const val BLUR_FLAG_WITH_SSR = 32

    private val blurShaders = LazyList(64) { flags ->
        val depthMask = "xyzw"[flags.and(BLUR_FLAG_DEPTH_MASK)]
        val normalZW = if (flags.hasFlag(BLUR_FLAG_ZW_NORMALS)) "zw" else "xy"
        val blur = flags.hasFlag(BLUR_FLAG_HAS_BLUR)
        val ssgi = flags.hasFlag(BLUR_FLAG_IS_SSGI)
        val ssr = flags.hasFlag(BLUR_FLAG_WITH_SSR)
        val sumType = when {
            ssgi -> "vec3"
            ssr -> "vec2"
            else -> "float"
        }
        val sumMask = when {
            ssgi -> "xyz"
            ssr -> "xy"
            else -> "x"
        }
        Shader(
            if (ssgi) if (blur) "ssgi-blur" else "ssgi-apply"
            else "ssao-blur", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1B, "inverseResult"),
                Variable(GLSLType.V4F, "glFragColor", VariableMode.OUT),
                Variable(GLSLType.S2D, "ssaoTex"),
            ) + listOf(
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.V2F, "duv"),
            ).iff(blur) + listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "illumTex")
            ).iff(ssgi), "" +
                    ("" +
                            ShaderLib.octNormalPacking +
                            "float getDepth(vec2 uv){\n" +
                            "   float depth = texture(depthTex,uv).$depthMask;\n" +
                            "   return log2(max(depth,1e-38));\n" +
                            "}\n" +
                            "vec3 getNormal(vec2 uv){\n" +
                            "   vec2 raw = texture(normalTex,uv).$normalZW;\n" +
                            "   return UnpackNormal(raw);\n" +
                            "}\n").iff(blur) +
                    "void main(){\n" +
                    // bilateral blur (use normal and depth as weights)
                    "$sumType valueSum = $sumType(0.0);\n" +
                    if (blur) {
                        "" +
                                "float weightSum = 0.0;\n" +
                                "float d0 = getDepth(uv);\n" +
                                "bool isNotSky = d0 < 60.0;\n" +
                                "if(isNotSky){\n" +
                                "   vec3 n0 = getNormal(uv);\n" +
                                "   for(int j=-2;j<=2;j++){\n" +
                                "       for(int i=-2;i<=2;i++){\n" +
                                "           vec2 ij = vec2(i,j);\n" +
                                "           vec2 uvi = uv+ij*duv;\n" +
                                "           float di = getDepth(uvi);\n" +
                                "           vec3  ni = getNormal(uvi);\n" +
                                "           float weight = max(1.0-abs(di-d0),0.0) * max(dot(n0,ni),0.0) / (1.0 + dot(ij,ij));\n" +
                                "            valueSum += weight * texture(ssaoTex,uvi).$sumMask;\n" +
                                "           weightSum += weight;\n" +
                                "       }\n" +
                                "   }\n" +
                                "}\n"
                    } else {
                        "valueSum = texture(ssaoTex,uv).$sumMask;\n"
                    } +
                    "valueSum *= 1.0 / weightSum;\n".iff(blur) +
                    when {
                        ssgi -> {
                            "" +
                                    "vec4 base = texture(illumTex,uv);\n" +
                                    "base.rgb += valueSum * texture(colorTex,uv).xyz;\n" +
                                    "glFragColor = base;\n"
                        }
                        ssr -> {
                            "glFragColor = isNotSky ? vec4(valueSum, 0.0, 1.0) : vec4(0.0);\n" +
                                    "if (inverseResult) { glFragColor.r = 1.0 - glFragColor.r; }\n"
                        }
                        else -> {
                            "glFragColor = isNotSky ? vec4(valueSum) : vec4(0.0);\n" +
                                    "if (inverseResult) { glFragColor.r = 1.0 - glFragColor.r; }\n"
                        }
                    } + "}"
        )
    }

    private fun getNumChannels(ssgi: ScreenSpaceGlobalIlluminationData?, ssr: ScreenSpaceShadowData?): Int {
        return when {
            ssgi != null -> 3
            ssr != null -> 2
            else -> 1
        }
    }

    private fun calculate(
        ssgi: ScreenSpaceGlobalIlluminationData?,
        ssr: ScreenSpaceShadowData?,
        depthSS: ITexture2D,
        depthMask: Int,
        normalSS: ITexture2D,
        normalZW: Boolean,
        cameraMatrix: Matrix4f,
        strength: Float,
        radiusScale: Float,
        samples: Int,
        enableBlur: Boolean,
    ): IFramebuffer {

        // resolution can be halved to improve performance
        val scale = DefaultConfig["gpu.ssao.scale", 1f]
        val fw = (depthSS.width * scale).roundToIntOr()
        val fh = (depthSS.height * scale).roundToIntOr()

        val isSSGI = ssgi != null
        val channels = getNumChannels(ssgi, ssr)
        val dst = FBStack["ssao-1st", fw, fh, channels, isSSGI, 1, DepthBufferType.NONE]
        useFrame(dst, Renderer.copyRenderer) {
            dst.clearColor(0)
            GFX.check()
            val msaa = depthSS.samples > 1
            val roughnessMask = ssgi?.roughnessMask ?: 0
            val base = ((msaa.toInt() + isSSGI.toInt(2)).shl(2) + roughnessMask).shl(3)
            val shader = occlusionShaders[base + normalZW.toInt() + depthMask.shl(1)]
            shader.use()

            // todo bug: "sampleKernel"-slot is filled by random4x4-texture

            DepthTransforms.bindDepthUniforms(shader)
            // bind all textures
            sampleKernel[samples].bindTrulyNearest(shader, "sampleKernel")
            random4x4.bindTrulyNearest(shader, "random4x4")
            if (ssgi != null) {
                ssgi.illuminated.bindTrulyNearest(shader, "illuminatedTex")
                ssgi.roughness.bindTrulyNearest(shader, "roughnessTex")
            }
            normalSS.bindTrulyNearest(shader, "finalNormal")
            depthSS.bindTrulyNearest(shader, "finalDepth")
            // define all uniforms
            shader.m4x4("cameraMatrix", cameraMatrix)
            shader.v1i("numSamples", samples)
            shader.v1f("strength", strength / samples)
            shader.v1i("mask", if (enableBlur) 3 else 0)
            shader.v1f("radiusScale", radiusScale)
            // sun uniforms
            if (ssr != null) {
                shader.v3f("sunDirection", ssr.direction)
                shader.v1f("sunStrength", ssr.strength)
            } else {
                shader.v1f("sunStrength", 0f)
            }
            // draw
            flat01.draw(shader)
            GFX.check()
        }

        return dst
    }

    private fun applyBlur(
        ssaoTex: IFramebuffer,
        normals: ITexture2D, normalZW: Boolean,
        depth: ITexture2D, depthMaskI: Int,
        enableBlur: Boolean,
        ssgi: ScreenSpaceGlobalIlluminationData?,
        ssr: ScreenSpaceShadowData?,
        inverse: Boolean
    ): IFramebuffer {
        val w = ssaoTex.width
        val h = ssaoTex.height
        val isSSGI = ssgi != null
        val numChannels = getNumChannels(ssgi, ssr)
        val dst = FBStack["ssao-2nd", w, h, numChannels, isSSGI, 1, DepthBufferType.NONE]
        useFrame(dst, Renderer.copyRenderer) {
            dst.clearColor(0)
            GFX.check()
            val withSSR = !isSSGI && ssr != null
            val shaderId = depthMaskI +
                    enableBlur.toInt(BLUR_FLAG_HAS_BLUR) +
                    isSSGI.toInt(BLUR_FLAG_IS_SSGI) +
                    normalZW.toInt(BLUR_FLAG_ZW_NORMALS) +
                    withSSR.toInt(BLUR_FLAG_WITH_SSR)
            val shader = blurShaders[shaderId]
            shader.use()
            ssaoTex.getTexture0().bindTrulyNearest(shader, "ssaoTex")
            if (ssgi != null) {
                ssgi.color.bindTrulyNearest(shader, "colorTex")
                ssgi.illuminated.bindTrulyNearest(shader, "illumTex")
            }
            normals.bindTrulyNearest(shader, "normalTex")
            depth.bindTrulyNearest(shader, "depthTex")
            shader.v1b("inverseResult", inverse)
            shader.v2f("duv", 1f / w, 1f / h)
            flat01.draw(shader)
            GFX.check()
        }
        return dst
    }

    fun compute(
        ssgi: ScreenSpaceGlobalIlluminationData?,
        ssr: ScreenSpaceShadowData?,
        depth: ITexture2D,
        depthMaskI: Int,
        normal: ITexture2D,
        normalZW: Boolean,
        cameraMatrix: Matrix4f,
        strength: Float,
        radiusScale: Float,
        samples: Int,
        enableBlur: Boolean,
        inverse: Boolean,
    ): IFramebuffer {
        return GFXState.renderPurely {
            val ssao = calculate(
                ssgi, ssr, depth, depthMaskI, normal, normalZW,
                cameraMatrix, strength, radiusScale,
                min(samples, MAX_SAMPLES), enableBlur,
            )
            if (enableBlur || ssgi != null) {
                applyBlur(
                    ssao, normal, normalZW,
                    depth, depthMaskI,
                    enableBlur, ssgi, ssr, inverse
                )
            } else ssao
        }
    }
}
