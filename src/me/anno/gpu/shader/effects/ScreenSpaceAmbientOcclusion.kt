package me.anno.gpu.shader.effects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredSettings
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
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.iff
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.iff
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

// todo this is too dark on some regions on curved region on flat angles
//  - maybe the normals are close to being backwards?

// todo bilateral filtering for MSAA data
object ScreenSpaceAmbientOcclusion {

    class SSGIData(
        val illuminated: ITexture2D, val color: ITexture2D,
        val roughness: ITexture2D, val roughnessMask: Vector4f,
    )

    // todo this can become extremely with complex geometry
    // (40 fps on a RTX 3070 🤯, where a pure-color scene has 600 fps)
    // todo why is pure color soo slow? 600 fps instead of 1200 fps in mode "without post-processing")
    // why is this soo expensive on my RTX3070?
    // memory limited...

    // could be set lower for older hardware, would need restart
    private val MAX_SAMPLES = Maths.max(4, DefaultConfig["gpu.ssao.maxSamples", 512])
    private val sampleKernel = LazyMap { numSamples: Int ->
        generateSampleKernel(numSamples)
    }

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
        val data = ByteArray(64)
        for (i in 0 until 16) {
            val nx = random.nextFloat() * 2 - 1
            val ny = random.nextFloat() * 2 - 1
            val nz = random.nextFloat() * 2 - 1
            val nf = 127.5f / sqrt(nx * nx + ny * ny + nz * nz)
            val x = Maths.clamp(nx * nf + 127.5f, 0f, 255f)
            val y = Maths.clamp(ny * nf + 127.5f, 0f, 255f)
            val z = Maths.clamp(nz * nf + 127.5f, 0f, 255f)
            data[j++] = (x.toInt().toByte())
            data[j++] = (y.toInt().toByte())
            data[j++] = (z.toInt().toByte())
            data[j++] = (-1)
        }
        return IndestructibleTexture2D("ssao-noise", 4, 4, data)
    }

    private val random4x4 = generateRandomTexture(Random(1234L))

    private val occlusionShaders = Array(4) {
        val multisampling = it.hasFlag(1)
        val ssgi = it.hasFlag(2)
        val srcType = if (multisampling) GLSLType.S2DMS else GLSLType.S2D
        Shader(
            if (ssgi) "ssgi" else "ssao",
            ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.V1F, "strength"),
                Variable(GLSLType.V1F, "radiusScale"),
                Variable(GLSLType.V1I, "numSamples"),
                Variable(GLSLType.V1I, "mask"),
                Variable(GLSLType.M4x4, "transform"),
                Variable(srcType, "finalDepth"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(srcType, "finalNormal"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.S2D, "sampleKernel"),
                Variable(GLSLType.S2D, "random4x4"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + listOf(
                Variable(GLSLType.S2D, "illuminatedTex"),
                Variable(GLSLType.S2D, "roughnessTex"),
                Variable(GLSLType.V4F, "roughnessMask"),
            ).iff(ssgi) + DepthTransforms.depthVars, "" +
                    "float dot2(vec3 p){ return dot(p,p); }\n" +
                    ShaderLib.quatRot +
                    DepthTransforms.rawToDepth +
                    DepthTransforms.depthToPosition +
                    ShaderLib.octNormalPacking +
                    "void main(){\n" +
                    (if (multisampling) "" +
                            "   vec2 texSizeI = vec2(textureSize(finalDepth));\n" +
                            "   #define getPixel(tex,uv) texelFetch(tex,ivec2(clamp(uv,vec2(0.0),vec2(0.99999))*texSizeI),0)\n"
                    else "#define getPixel(tex,uv) textureLod(tex,uv,0.0)\n") +
                    "   float depth0 = dot(getPixel(finalDepth, uv), depthMask);\n" +
                    "   vec3 origin = rawDepthToPosition(uv, depth0);\n" +
                    "   float radius = length(origin);\n" +
                    "   if(radius < 1e18){\n" + // sky and such can be skipped automatically
                    "       radius *= radiusScale;\n" +
                    "       vec4 normalData = getPixel(finalNormal, uv);\n" +
                    "       vec3 normal = UnpackNormal(normalZW ? normalData.zw : normalData.xy);\n" +
                    // reverse back sides, e.g., for plants
                    // could be done by the material as well...
                    "       if(dot(origin,normal) > 0.0) normal = -normal;\n" +
                    "       vec3 randomVector = texelFetch(random4x4, ivec2(gl_FragCoord.xy) & mask, 0).xyz * 2.0 - 1.0;\n" +
                    "       vec3 tangent = normalize(randomVector - normal * dot(normal, randomVector));\n" +
                    "       vec3 bitangent = cross(normal, tangent);\n" +
                    (if (ssgi) { // reduce blurriness on smooth surfaces
                        "" +
                                "float roughness = 0.1 + 0.9 * dot(getPixel(roughnessTex,uv),roughnessMask);\n" +
                                "tangent *= roughness;\n" +
                                "bitangent *= roughness;\n"
                    } else "") +
                    "       mat3 tbn = mat3(tangent, bitangent, normal);\n" +
                    (if (ssgi) {
                        "      vec3 lightSum = vec3(0.0);\n"
                    } else {
                        "       float occlusion = 0.0;\n"
                    }) +
                    "       // [loop]\n" + // hlsl instruction
                    "       for(int i=0;i<numSamples;i++){\n" +
                    // "sample" seems to be a reserved keyword for the emulator
                    "           vec3 dir0 = texelFetch(sampleKernel, ivec2(i,0), 0).xyz;\n" +
                    "           vec3 dir1 = matMul(tbn, dir0);\n" +
                    "           vec3 position = dir1 * radius + origin;\n" +
                    // project sample position... mmmh...
                    "           vec4 offset = matMul(transform, vec4(position, 1.0));\n" +
                    "           offset.xy /= offset.w;\n" +
                    "           offset.xy = offset.xy * 0.5 + 0.5;\n" +
                    "           bool isInside = offset.x >= 0.0 && offset.x <= 1.0 && offset.y >= 0.0 && offset.y <= 1.0;\n" +
                    // theoretically, the tutorial also contained this condition, but somehow it
                    // introduces a radius (when radius = 1), where occlusion appears exclusively
                    // && abs(originDepth - sampleDepth) < radius
                    // without it, the result looks approx. the same :)
                    "           if(isInside){\n" +
                    "               float depth1 = dot(getPixel(finalDepth, offset.xy), depthMask);\n" +
                    "               float sampleDepthSq = dot2(rawDepthToPosition(offset.xy, depth1));\n" +
                    (if (ssgi) {
                        "" +
                                // add light from that surface
                                "   vec4 normalData1 = getPixel(finalNormal, offset.xy);\n" +
                                "   vec3 normal1 = UnpackNormal(normalZW ? normalData1.zw : normalData1.xy);\n" +
                                // aligned normals have higher contribution -> correct?? kind of a guess...
                                "   float alignmentStrength = 0.6 - 0.4 * dot(normal,normal1);\n" +
                                "   vec3 color1 = getPixel(illuminatedTex,offset.xy).xyz;\n" +
                                "   lightSum += alignmentStrength *  color1;\n"
                    } else {
                        "" +
                                "   float sampleTheoDepth = dot2(position);\n" +
                                "   occlusion += step(sampleDepthSq, sampleTheoDepth);\n"
                    }) +
                    "           }\n" +
                    "       }\n" +
                    (if (ssgi) "result = vec4(lightSum * strength, 1.0);\n"
                    else "result = vec4(clamp(strength * occlusion, 0.0, 1.0));\n") +
                    "   } else {\n" +
                    "       result = vec4(0.0);\n" +
                    "   }\n" +
                    "}"
        ).apply {
            glslVersion = 330
        }
    }

    private val blurShaders = Array(3) {
        val blur = (it + 1).hasFlag(1)
        val ssgi = (it + 1).hasFlag(2)
        Shader(
            if (ssgi) if (blur) "ssgi-blur" else "ssgi-apply"
            else "ssao-blur", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V4F, "glFragColor", VariableMode.OUT),
                Variable(GLSLType.S2D, "ssaoTex"),
            ) + listOf(
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.V2F, "duv"),
            ).iff(blur) + listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "illumTex")
            ).iff(ssgi), "" +
                    ("" +
                            ShaderLib.octNormalPacking +
                            "float getDepth(vec2 uv){\n" +
                            "   float depth = dot(texture(depthTex,uv), depthMask);\n" +
                            "   return log2(max(depth,1e-38));\n" +
                            "}\n" +
                            "vec3 getNormal(vec2 uv){\n" +
                            "   vec4 raw = texture(normalTex,uv);\n" +
                            "   return UnpackNormal(normalZW ? raw.zw : raw.xy);\n" +
                            "}\n").iff(blur) +
                    "void main(){\n" +
                    // bilateral blur (use normal and depth as weights)
                    (if (ssgi) "vec3 valueSum = vec3(0.0);\n"
                    else "float valueSum = 0.0;\n") +
                    (if (blur) {
                        "" +
                                "   float weightSum = 0.0;\n" +
                                "   float d0 = getDepth(uv);\n" +
                                "   vec3  n0 = getNormal(uv);\n" +
                                "   for(int j=-2;j<=2;j++){\n" +
                                "       for(int i=-2;i<=2;i++){\n" +
                                "           vec2 ij = vec2(i,j);\n" +
                                "           vec2 uvi = uv+ij*duv;\n" +
                                "           float di = getDepth(uvi);\n" +
                                "           vec3  ni = getNormal(uvi);\n" +
                                "           float weight = max(1.0-abs(di-d0),0.0) * max(dot(n0,ni),0.0) / (1.0 + dot(ij,ij));\n" +
                                "            valueSum += weight * texture(ssaoTex,uvi)" + (if (ssgi) ".xyz" else ".x") + ";\n" +
                                "           weightSum += weight;\n" +
                                "       }\n" +
                                "   }\n"
                    } else "valueSum = texture(ssaoTex,uv).xyz;\n") +
                    (if (ssgi) {
                        "" + (if (blur) "valueSum *= 1.0 / weightSum;\n" else "") +
                                "vec4 base = texture(illumTex,uv);\n" +
                                "base.rgb += valueSum * texture(colorTex,uv).xyz;\n" +
                                "glFragColor = base;\n"
                    } else
                        "glFragColor = vec4(valueSum / weightSum);\n") +
                    "}"
        )
    }

    private fun calculate(
        ssgi: SSGIData?,
        depth: ITexture2D,
        depthMask: String,
        normal: ITexture2D,
        normalZW: Boolean,
        transform: Matrix4f,
        strength: Float,
        radiusScale: Float,
        samples: Int,
        enableBlur: Boolean
    ): IFramebuffer {

        // resolution can be halved to improve performance
        val scale = DefaultConfig["gpu.ssao.scale", 1f]
        val fw = (depth.width * scale).roundToInt()
        val fh = (depth.height * scale).roundToInt()

        val isSSGI = ssgi != null
        val dst = FBStack["ssao-1st", fw, fh, if (isSSGI) 3 else 1, isSSGI, 1, DepthBufferType.NONE]
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            val msaa = (depth is Texture2D && depth.samples > 1)
            val shader = occlusionShaders[msaa.toInt() + isSSGI.toInt(2)]
            shader.use()
            DepthTransforms.bindDepthUniforms(shader)
            // bind all textures
            sampleKernel[samples].bindTrulyNearest(shader, "sampleKernel")
            random4x4.bindTrulyNearest(shader, "random4x4")
            if (ssgi != null) {
                ssgi.illuminated.bindTrulyNearest(shader, "illuminatedTex")
                ssgi.roughness.bindTrulyNearest(shader, "roughnessTex")
                shader.v4f("roughnessMask", ssgi.roughnessMask)
            }
            normal.bindTrulyNearest(shader, "finalNormal")
            depth.bindTrulyNearest(shader, "finalDepth")
            // define all uniforms
            shader.m4x4("transform", transform)
            shader.v1i("numSamples", samples)
            shader.v1f("strength", strength / samples)
            shader.v1i("mask", if (enableBlur) 3 else 0)
            shader.v1b("normalZW", normalZW)
            shader.v4f("depthMask", DeferredSettings.singleToVector[depthMask]!!)
            shader.v1f("radiusScale", radiusScale)
            // draw
            flat01.draw(shader)
            GFX.check()
        }

        return dst
    }

    private fun average(
        ssaoTex: IFramebuffer,
        normals: ITexture2D, normalZW: Boolean,
        depth: ITexture2D, depthMask: String,
        enableBlur: Boolean, ssgi: SSGIData?
    ): IFramebuffer {
        val w = ssaoTex.width
        val h = ssaoTex.height
        val isSSGI = ssgi != null
        val dst = FBStack["ssao-2nd", w, h, if (isSSGI) 3 else 1, isSSGI, 1, DepthBufferType.NONE]
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            val shader = blurShaders[enableBlur.toInt() + isSSGI.toInt(2) - 1]
            shader.use()
            shader.v1b("normalZW", normalZW)
            shader.v4f("depthMask", DeferredSettings.singleToVector[depthMask]!!)
            ssaoTex.getTexture0().bindTrulyNearest(shader, "ssaoTex")
            if (ssgi != null) {
                ssgi.color.bindTrulyNearest(shader, "colorTex")
                ssgi.illuminated.bindTrulyNearest(shader, "illumTex")
            }
            normals.bindTrulyNearest(shader, "normalTex")
            depth.bindTrulyNearest(shader, "depthTex")
            shader.v2f("duv", 1f / w, 1f / h)
            flat01.draw(shader)
            GFX.check()
        }
        return dst
    }

    fun compute(
        ssgi: SSGIData?,
        depth: ITexture2D,
        depthMask: String,
        normal: ITexture2D,
        normalZW: Boolean,
        transform: Matrix4f,
        strength: Float,
        radiusScale: Float,
        samples: Int,
        enableBlur: Boolean
    ): IFramebuffer {
        return GFXState.renderPurely {
            val ssao = calculate(
                ssgi, depth, depthMask, normal, normalZW,
                transform, strength, radiusScale,
                Maths.min(samples, MAX_SAMPLES), enableBlur,
            )
            if (enableBlur || ssgi != null) {
                average(
                    ssao, normal, normalZW,
                    depth, depthMask,
                    enableBlur, ssgi
                )
            } else ssao
        }
    }
}