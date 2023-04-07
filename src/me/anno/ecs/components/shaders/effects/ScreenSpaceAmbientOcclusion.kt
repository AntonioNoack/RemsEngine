package me.anno.ecs.components.shaders.effects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ReverseDepth.bindDepthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPositionList
import me.anno.gpu.shader.ReverseDepth.rawToDepth
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import org.joml.Matrix4f
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ScreenSpaceAmbientOcclusion {

    // todo option to calculate normals from depth for forward-ssao

    // could be set lower for older hardware, would need restart
    private val MAX_SAMPLES = max(4, DefaultConfig["gpu.ssao.maxSamples", 512])

    private val sampleKernel = Texture2D("sampleKernel", MAX_SAMPLES, 1, 1)
    private val sampleKernel0 = FloatArray(MAX_SAMPLES * 4)

    private fun generateSampleKernel(samples: Int, seed: Long = 1234L) {
        val random = Random(seed)
        var j = 0
        val data = sampleKernel0
        for (i in 0 until samples) {
            val f01 = i / (samples - 1f)
            val x = random.nextFloat() * 2f - 1f
            val y = random.nextFloat() * 2f - 1f
            val z = random.nextFloat() // half sphere
            val scale = f01 * f01 * 0.9f + 0.1f
            val f = scale / sqrt(x * x + y * y + z * z)
            data[j++] = x * f
            data[j++] = y * f
            data[j++] = z * f
            j++
        }
        sampleKernel.createRGBA(data, false)
    }

    // tiled across the screen; e.g., 4x4 texture size
    // because of that, we probably could store it in the shader itself
    // 2*16 values ~ just two matrices
    private fun generateRandomTexture(random: Random, w: Int, h: Int = w): Texture2D {
        val data = ByteBufferPool.allocateDirect(3 * w * h)
        for (i in 0 until w * h) {
            val nx = random.nextFloat() * 2 - 1
            val ny = random.nextFloat() * 2 - 1
            val nz = random.nextFloat() * 2 - 1
            val nf = 127.5f / sqrt(nx * nx + ny * ny + nz * nz)
            val x = clamp(nx * nf + 127.5f, 0f, 255f)
            val y = clamp(ny * nf + 127.5f, 0f, 255f)
            val z = clamp(nz * nf + 127.5f, 0f, 255f)
            data.put(x.toInt().toByte())
            data.put(y.toInt().toByte())
            data.put(z.toInt().toByte())
        }
        data.flip()
        val tex = Texture2D("ssao-noise", w, h, 1)
        tex.createRGB(data, false)
        return tex
    }

    private var random4x4: Texture2D? = null

    // 2 passes: occlusion factor, then blurring
    private val occlusionShader = Shader(
        "ssao",
        coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V1F, "radius"),
            Variable(GLSLType.V1F, "strength"),
            Variable(GLSLType.V1F, "skipRadiusSq"),
            Variable(GLSLType.V1I, "numSamples"),
            Variable(GLSLType.V1I, "mask"),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.S2D, "sampleKernel"),
            Variable(GLSLType.S2D, "finalDepth"),
            Variable(GLSLType.S2D, "finalNormal"),
            Variable(GLSLType.S2D, "random4x4"),
            Variable(GLSLType.V1B, "normalZW"),
            Variable(GLSLType.V4F, "glFragColor", VariableMode.OUT)
        ) + depthToPositionList, "" +
                "float dot2(vec3 p){ return dot(p,p); }\n" +
                quatRot +
                rawToDepth + depthToPosition +
                octNormalPacking +
                "void main(){\n" +
                "   vec3 origin = rawDepthToPosition(uv, texture(finalDepth, uv).x);\n" +
                "   if(dot2(origin) > skipRadiusSq){\n" + // sky and such can be skipped automatically
                "       glFragColor = vec4(0.0);\n" +
                "   } else {\n" +
                "       vec4 normalData = texture(finalNormal, uv);\n" +
                "       vec3 normal = UnpackNormal(normalZW ? normalData.zw : normalData.xy);\n" +
                // reverse back sides, e.g., for plants
                // could be done by the material as well...
                "       if(dot(origin,normal) > 0.0) normal = -normal;\n" +
                "       vec3 randomVector = texelFetch(random4x4, ivec2(gl_FragCoord.xy) & mask, 0).xyz * 2.0 - 1.0;\n" +
                "       vec3 tangent = normalize(randomVector - normal * dot(normal, randomVector));\n" +
                "       vec3 bitangent = cross(normal, tangent);\n" +
                "       mat3 tbn = mat3(tangent, bitangent, normal);\n" +
                "       float occlusion = 0.0;\n" +
                "       for(int i=0;i<numSamples;i++){\n" +
                // "sample" seems to be a reserved keyword for the emulator
                "           vec3 position = tbn * texelFetch(sampleKernel, ivec2(i,0), 0).xyz * radius + origin;\n" +
                "           float sampleTheoDepth = dot2(position);\n" +
                // project sample position... mmmh...
                "           vec4 offset = transform * vec4(position, 1.0);\n" +
                "           offset.xy /= offset.w;\n" +
                "           offset.xy = offset.xy * 0.5 + 0.5;\n" +
                "           bool isInside = offset.x >= 0.0 && offset.x <= 1.0 && offset.y >= 0.0 && offset.y <= 1.0;\n" +
                // theoretically, the tutorial also contained this condition, but somehow it
                // introduces a radius (when radius = 1), where occlusion appears exclusively
                // && abs(originDepth - sampleDepth) < radius
                // without it, the result looks approx. the same :)
                "           if(isInside){\n" +
                "               float sampleDepth = dot2(rawDepthToPosition(offset.xy, texture(finalDepth, offset.xy).x));\n" +
                "               occlusion += step(0.0, sampleTheoDepth-sampleDepth);\n" +
                "           }\n" +
                "       }\n" +
                "       glFragColor = vec4(clamp(strength * occlusion/float(numSamples), 0.0, 1.0));\n" +
                "   }" +
                "}"
    ).apply {
        glslVersion = 330
        setTextureIndices("finalDepth", "finalNormal", "random4x4", "sampleKernel")
    }

    private val blurShader = Shader(
        "ssao-blur", coordsList, coordsVShader, uvList,
        listOf(
            Variable(GLSLType.V4F, "glFragColor", VariableMode.OUT),
            Variable(GLSLType.S2D, "source"),
            Variable(GLSLType.V2F, "delta")
        ), "" +
                "void main(){\n" +
                "   float sum1;\n" +
                if (OS.isWeb) {
                    "" +
                            "   sum1 = 0.0;\n" +
                            "   for(int j=-1;j<3;j++){\n" +
                            "       for(int i=-1;i<3;i++){\n" +
                            "           sum1 += texture(source,uv+vec2(i,j)*delta).x;\n" +
                            "       }\n" +
                            "   }\n"
                } else {
                    "" +
                            "   vec2 uv0 = uv-delta;\n" +
                            "   vec2 uv1 = uv+delta;\n" +
                            "   vec4 sum = \n" + // avg over 4x4 area
                            "       textureGather(source,vec2(uv0.x,uv0.y),0) +\n" +
                            "       textureGather(source,vec2(uv1.x,uv0.y),0) +\n" +
                            "       textureGather(source,vec2(uv0.x,uv1.y),0) +\n" +
                            "       textureGather(source,vec2(uv1.x,uv1.y),0);\n" +
                            "   sum1 = (sum.x+sum.y+sum.z+sum.w);\n"
                } +
                "   glFragColor = vec4(sum1 * ${1.0 / 16.0});\n" +
                "}"
    ).apply { glslVersion = 400 } // 400 for textureGather

    private var lastSamples = 0

    private fun calculate(
        data: IFramebuffer,
        settingsV2: DeferredSettingsV2,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int,
        enableBlur: Boolean,
    ): Framebuffer? {
        // ensure we can find the required inputs
        val depth = data.depthTexture ?: return null
        val normalLayer = settingsV2.findLayer(DeferredLayerType.NORMAL) ?: return null
        val normal = settingsV2.findTextureMS(data, normalLayer)
        return calculate(
            depth, normal, normalLayer.mapping == "zw",
            transform, radius, strength, samples, enableBlur
        )
    }

    private fun calculate(
        depth: ITexture2D,
        normal: ITexture2D,
        normalZW: Boolean,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int,
        enableBlur: Boolean,
    ): Framebuffer {

        var random4x4 = random4x4
        random4x4?.checkSession()
        if (random4x4 == null || !random4x4.isCreated) {
            random4x4 = generateRandomTexture(Random(1234L), 4)
            this.random4x4 = random4x4
        }

        // resolution can be halved to improve performance
        val scale = DefaultConfig["gpu.ssao.scale", 1f]
        val fw = (depth.w * scale).roundToInt()
        val fh = (depth.h * scale).roundToInt()

        val dst = FBStack["ssao-1st", fw, fh, 1, false, 1, false]
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            val shader = occlusionShader
            shader.use()
            bindDepthToPosition(shader)
            // bind all textures
            if (lastSamples != samples) { // || Input.isShiftDown
                // generate random kernel
                generateSampleKernel(samples)
                lastSamples = samples
            }
            sampleKernel.bindTrulyNearest(3)
            random4x4.bindTrulyNearest(2)
            normal.bindTrulyNearest(1)
            depth.bindTrulyNearest(0)
            // define all uniforms
            shader.v1f("radius", radius)
            shader.v1f("skipRadiusSq", sq(radius * 20f))
            shader.m4x4("transform", transform)
            shader.v1i("numSamples", samples)
            shader.v1f("strength", strength)
            shader.v1i("mask", if (enableBlur) 3 else 0)
            shader.v1b("normalZW", normalZW)
            // draw
            GFX.flat01.draw(shader)
            GFX.check()
        }

        return dst
    }

    private fun average(data: Framebuffer): Framebuffer {
        if (!DefaultConfig["gpu.ssao.blur", true]) return data
        val w = data.w
        val h = data.h
        val dst = FBStack["ssao-2nd", w, h, 1, false, 1, false]
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            val shader = blurShader
            shader.use()
            data.bindTrulyNearest(0)
            shader.v2f("delta", 1f / w, 1f / h)
            GFX.flat01.draw(shader)
            GFX.check()
        }
        return dst
    }

    fun compute(
        data: IFramebuffer,
        settingsV2: DeferredSettingsV2,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int,
        enableBlur: Boolean,
    ): ITexture2D? {
        if (strength <= 0f) return whiteTexture
        return renderPurely {
            val samples1 = min(samples, MAX_SAMPLES)
            val tmp = calculate(data, settingsV2, transform, radius, strength, samples1, enableBlur)
            if (tmp == null) null else if (enableBlur) average(tmp).getTexture0() else tmp.getTexture0()
        }
    }

    fun compute(
        depth: ITexture2D,
        normal: ITexture2D,
        normalZW: Boolean,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int,
        enableBlur: Boolean,
    ): IFramebuffer {
        return renderPurely {
            val tmp = calculate(
                depth, normal, normalZW, transform, radius,
                strength, min(samples, MAX_SAMPLES), enableBlur
            )
            if (enableBlur) average(tmp) else tmp
        }
    }

}