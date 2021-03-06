package me.anno.ecs.components.shaders.effects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.renderPurely
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
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
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.ByteBufferPool
import org.joml.Matrix4f
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ScreenSpaceAmbientOcclusion {

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
    private val occlusionShader = lazy {
        Shader(
            "ssao-occlusion",
            coordsList, coordsVShader, uvList, emptyList(), "" +
                    "layout(location=0) out vec4 glFragColor;\n" +
                    "uniform float radius, strength, skipRadiusSq;\n" +
                    "uniform ivec2 size;\n" +
                    "uniform int numSamples;\n" +
                    "uniform mat4 transform;\n" +
                    "uniform sampler2D sampleKernel;\n" +
                    "uniform sampler2D finalPosition, finalNormal, random4x4;\n" +
                    "float dot2(vec3 p){ return dot(p,p); }\n" +
                    "void main(){\n" +
                    "   vec3 origin = texture(finalPosition, uv).xyz;\n" +
                    "   if(dot2(origin) > skipRadiusSq){\n" + // sky and such can be skipped automatically
                    "       glFragColor = vec4(0.0);\n" +
                    "   } else {\n" +
                    // "  float originDepth = length(origin);\n" +
                    "       vec3 normal = texture(finalNormal, uv).xyz * 2.0 - 1.0;\n" +
                    // reverse back sides, e.g., for plants
                    // could be done by the material as well...
                    "       if(dot(origin,normal) > 0.0) normal = -normal;\n" +
                    "       vec3 randomVector = texelFetch(random4x4, ivec2(gl_FragCoord.xy) & 3, 0).xyz * 2.0 - 1.0;\n" +
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
                    "               float sampleDepth = dot2(texture(finalPosition, offset.xy).xyz);\n" +
                    "               occlusion += step(0.0, sampleTheoDepth-sampleDepth);\n" +
                    "           }\n" +
                    "       }\n" +
                    "       glFragColor = vec4(clamp(strength * occlusion/float(numSamples), 0.0, 1.0));\n" +
                    "   }" +
                    "}"
        ).apply {
            glslVersion = 330
            setTextureIndices(listOf("finalPosition", "finalNormal", "random4x4", "sampleKernel"))
        }
    }

    private val blurShader = lazy {
        Shader(
            "ssao-blur", coordsList, coordsVShader, uvList,
            listOf(
                Variable(GLSLType.V4F, "glFragColor", VariableMode.OUT),
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
                    "   glFragColor = vec4((sum.x+sum.y+sum.z+sum.w) * ${1.0 / 16.0});\n" +
                    "}"
        ).apply { glslVersion = 400 } // 400 for textureGather
    }

    private var lastSamples = 0

    private fun calculate(
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
        return calculate(position, normal, transform, radius, strength, samples)
    }

    private fun copy(src: ITexture2D, dst: Framebuffer): ITexture2D {
        useFrame(dst) {
            GFX.copyNoAlpha(src)
        }
        return dst.getTexture0()
    }

    private fun calculate(
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
        val scale = DefaultConfig["gpu.ssao.scale", 1f]
        val fw = (position.w * scale).roundToInt()
        val fh = (position.h * scale).roundToInt()
        val tw = min(fw, position.w)
        val th = min(fh, position.h)

        // todo given depth, calculate position
        // costs compute, but saves bandwidth

        // scale down source to reduce vram bandwidth
        val pos = copy(position, FBStack["ssao-pos", tw, th, 4, BufferQuality.HIGH_16, 1, false])
        val nor = copy(normal, FBStack["ssao-nor", tw, th, 4, BufferQuality.LOW_8, 1, false])

        val dst = FBStack["ssao-1st", fw, fh, 1, false, 1, false]
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            val shader = occlusionShader.value
            shader.use()
            // bind all textures
            if (lastSamples != samples) { // || Input.isShiftDown
                // generate random kernel
                generateSampleKernel(samples)
                lastSamples = samples
            }
            sampleKernel.bindTrulyNearest(3)
            random4x4.bindTrulyNearest(2)
            nor.bindTrulyNearest(1)
            pos.bindTrulyNearest(0)
            // define all uniforms
            shader.v2i("size", fw, fh)
            shader.v1f("radius", radius)
            shader.v1f("skipRadiusSq", sq(radius * 20f))
            shader.m4x4("transform", transform)
            shader.v1i("numSamples", samples)
            shader.v1f("strength", strength)
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
        lateinit var result: ITexture2D
        renderPurely {
            val tmp = calculate(data, settingsV2, transform, radius, strength, min(samples, MAX_SAMPLES)) ?: return null
            result = average(tmp).getTexture0()
        }
        return result
    }

    fun compute(
        position: ITexture2D,
        normal: ITexture2D,
        transform: Matrix4f,
        radius: Float,
        strength: Float,
        samples: Int
    ): IFramebuffer {
        lateinit var result: IFramebuffer
        renderPurely {
            val tmp = calculate(position, normal, transform, radius, strength, min(samples, MAX_SAMPLES))
            result = average(tmp)
        }
        return result
    }

}