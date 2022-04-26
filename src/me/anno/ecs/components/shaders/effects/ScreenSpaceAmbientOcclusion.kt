package me.anno.ecs.components.shaders.effects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.attr0List
import me.anno.gpu.shader.ShaderLib.attr0VShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.roundDiv
import org.joml.Matrix4f
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.sqrt

object ScreenSpaceAmbientOcclusion {

    // could be set lower for older hardware, would need restart
    private val MAX_SAMPLES = max(4, DefaultConfig["gpu.ssao.maxSamples", 512])

    private val sampleKernel = ByteBuffer
        .allocateDirect(4 * MAX_SAMPLES * 3)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()


    private fun generateSampleKernel(samples: Int, seed: Long = 1234L) {
        val random = Random(seed)
        sampleKernel.position(0)
        for (i in 0 until samples) {
            val f01 = i / (samples - 1f)
            val x = random.nextFloat() * 2f - 1f
            val y = random.nextFloat() * 2f - 1f
            val z = random.nextFloat() // half sphere
            val scale = f01 * f01 * 0.9f + 0.1f
            val f = scale / sqrt(x * x + y * y + z * z)
            sampleKernel.put(x * f)
            sampleKernel.put(y * f)
            sampleKernel.put(z * f)
        }
    }

    // tiled across the screen; e.g. 4x4 texture size
    // because of that, we probably could store it in the shader itself
    // 2*16 values ~ just two matrices
    private fun generateRandomTexture(random: Random, w: Int, h: Int = w): Texture2D {
        val data = ByteBuffer.allocateDirect(3 * w * h)
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
    private val occlusionShader = lazy {
        Shader(
            "ssao-occlusion",
            attr0List, attr0VShader, uvList, emptyList(), "" +
                    "layout(location=0) out float glFragColor;\n" +
                    "uniform float radius, strength;\n" +
                    "uniform int numSamples;\n" +
                    "uniform mat4 transform;\n" +
                    "uniform vec3[$MAX_SAMPLES] sampleKernel;\n" +
                    "uniform sampler2D finalPosition, finalNormal, random4x4;\n" +
                    "float dot2(vec3 p){ return dot(p,p); }\n" +
                    "void main(){\n" +
                    "   vec3 origin = texture(finalPosition, uv).xyz;\n" +
                    "   if(dot2(origin) > 1e20){\n" + // sky and such can be skipped automatically
                    "       glFragColor = 1.0;\n" +
                    "   } else {;\n" +
                    // "  float originDepth = length(origin);\n" +
                    "       vec3 normal = texture(finalNormal, uv).xyz * 2.0 - 1.0;\n" +
                    // reverse back sides, e.g. for plants
                    // could be done by the material as well...
                    "       if(dot(origin,normal) > 0.0) normal = -normal;\n" +
                    "       vec3 randomVector = texelFetch(random4x4, ivec2(gl_FragCoord.xy) & 3, 0).xyz * 2.0 - 1.0;\n" +
                    "       vec3 tangent = normalize(randomVector - normal * dot(normal, randomVector));\n" +
                    "       vec3 bitangent = cross(normal, tangent);\n" +
                    "       mat3 tbn = mat3(tangent, bitangent, normal);\n" +
                    "       float occlusion = 0.0;\n" +
                    "       for(int i=0;i<numSamples;i++){\n" +
                    // "sample" seems to be a reserved keyword for the emulator
                    "          vec3 position = tbn * sampleKernel[i] * radius + origin;\n" +
                    "          float sampleTheoDepth = dot2(position);\n" +
                    // project sample position... mmmh...
                    "           vec4 offset = transform * vec4(position, 1.0);\n" +
                    "           offset.xy /= offset.w;\n" +
                    "           offset.xy = offset.xy * 0.5 + 0.5;\n" +
                    "           bool isInside = offset.x >= 0.0 && offset.x <= 1.0 && offset.y >= 0.0 && offset.y <= 1.0;\n" +
                    "           float sampleDepth = dot2(texture(finalPosition, offset.xy).xyz);\n" +
                    // theoretically, the tutorial also contained this condition, but somehow it
                    // introduces a radius (when radius = 1), where occlusion appears exclusively
                    // && abs(originDepth - sampleDepth) < radius
                    // without it, the result looks approx. the same :)
                    "           occlusion += isInside ? sampleDepth < sampleTheoDepth ? 1.0 : 0.0 : 0.5;\n" +
                    "       }\n" +
                    "       glFragColor = clamp(1.0 - strength * occlusion/float(numSamples), 0.0, 1.0);\n" +
                    "   }" +
                    "}"
        ).apply {
            glslVersion = 330
            setTextureIndices(listOf("finalPosition", "finalNormal", "random4x4"))
        }
    }

    private val blurShader = lazy {
        Shader(
            "ssao-blur", attr0List, attr0VShader, uvList,
            listOf(
                Variable(GLSLType.V1F, "glFragColor", VariableMode.OUT),
                Variable(GLSLType.S2D, "source"),
                Variable(GLSLType.V2F, "delta")
            ), "" +
                    "void main(){\n" +
                    "   vec2 uv0 = uv-delta;\n" +
                    "   vec2 uv1 = uv+delta;\n" +
                    "   vec4 sum = \n" +
                    "       textureGather(source,vec2(uv0.x,uv0.y),0) +\n" +
                    "       textureGather(source,vec2(uv1.x,uv0.y),0) +\n" +
                    "       textureGather(source,vec2(uv0.x,uv1.y),0) +\n" +
                    "       textureGather(source,vec2(uv1.x,uv1.y),0);\n" +
                    "   glFragColor = (sum.x+sum.y+sum.z+sum.w) * ${1.0 / 16.0};\n" +
                    "}"
        ).apply { glslVersion = 400 } // 400 for textureGather
    }

    private var lastSamples = 0

    private fun firstPass(
        data: IFramebuffer,
        settingsV2: DeferredSettingsV2,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int
    ): Framebuffer? {
        // ensure we can find the required inputs
        val position = settingsV2.findTexture(data, DeferredLayerType.POSITION) ?: return null
        val normal = settingsV2.findTexture(data, DeferredLayerType.NORMAL) ?: return null
        return firstPass(position, normal, transform, radius, strength, samples)
    }

    private fun firstPass(
        position: ITexture2D,
        normal: ITexture2D,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int
    ): Framebuffer {

        var random4x4 = random4x4
        random4x4?.checkSession()
        if (random4x4 == null || !random4x4.isCreated) {
            random4x4 = generateRandomTexture(Random(1234L), 4)
            this.random4x4 = random4x4
        }
        // resolution can be halved to improve performance
        val div = clamp(DefaultConfig["gpu.ssao.div10", 10], 10, 100)
        val dst =
            FBStack["ssao-1st", roundDiv(position.w * 10, div), roundDiv(position.h * 10, div), 1, false, 1, false]
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            val shader = occlusionShader.value
            shader.use()
            // bind all textures
            random4x4.bind(2)
            val f = GPUFiltering.TRULY_NEAREST
            val c = Clamping.CLAMP
            normal.bind(1, f, c)
            position.bind(0, f, c)
            if (lastSamples != samples) { // || Input.isShiftDown
                // generate random kernel
                sampleKernel.position(0)
                generateSampleKernel(samples)
                sampleKernel.flip()
                lastSamples = samples
                shader.v3Array("sampleKernel", sampleKernel)
            }
            // define all uniforms
            shader.v1f("radius", radius)
            shader.m4x4("transform", transform)
            shader.v1i("numSamples", samples)
            shader.v1f("strength", strength)
            // draw
            GFX.flat01.draw(shader)
            GFX.check()
        }
        return dst
    }

    private fun secondPass(data: Framebuffer): Framebuffer {
        if (!DefaultConfig["gpu.ssao.blur", true]) return data
        val w = data.w
        val h = data.h
        val dst = FBStack["ssao-2nd", w, h, 1, false, 1, false]
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            val shader = blurShader.value
            shader.use()
            data.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
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
        samples: Int
    ): ITexture2D? {
        if (strength <= 0f) return whiteTexture
        val tmp = firstPass(data, settingsV2, transform, radius, strength, min(samples, MAX_SAMPLES)) ?: return null
        return secondPass(tmp).getTexture0()
    }

    fun compute(
        position: ITexture2D,
        normal: ITexture2D,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int
    ): IFramebuffer {
        val tmp = firstPass(position, normal, transform, radius, strength, min(samples, MAX_SAMPLES))
        return secondPass(tmp)
    }

}