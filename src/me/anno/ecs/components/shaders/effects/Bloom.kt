package me.anno.ecs.components.shaders.effects

import me.anno.gpu.GFX.flat01
import me.anno.gpu.OpenGL
import me.anno.gpu.OpenGL.renderPurely
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.simplestVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.studio.rems.Scene.noiseFunc
import me.anno.utils.maths.Maths.log
import me.anno.utils.maths.Maths.sq
import kotlin.math.exp

object Bloom {

    // bloom like in
    //  - https://www.youtube.com/watch?v=tI70-HIc5ro&ab_channel=TheCherno
    //  - https://github.com/Unity-Technologies/Graphics/blob/master/com.unity.postprocessing/PostProcessing/Shaders/Builtins/Bloom.shader
    //  - http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
    // todo use better hdr format to save on bandwidth

    // how many iterations are executed at maximum
    private const val maxSteps = 16 // equal to (65k)² pixels

    // minimum buffer size
    private const val minSize = 4

    // temporary buffer for this object
    private val tmpForward = arrayOfNulls<Framebuffer>(maxSteps)

    private fun forwardPass(source: ITexture2D, offset: Float): Int {

        var wi = source.w
        var hi = source.h
        var previous = source

        var shaderX = forwardShader0
        val shaderY = forwardShaderY

        shaderX.use()
        shaderX.v1("offset", offset)

        val renderer = copyRenderer

        for (i in 0 until maxSteps) {

            if (wi shr 1 < minSize || hi shr 1 < minSize) return i

            // x blur pass
            wi = wi shr 1
            shaderX.use()
            val bufferX = FBStack["bloomX", wi, hi, 4, true, 1, false]
            useFrame(bufferX, renderer) {
                previous.bindTrulyNearest(0)
                flat01.draw(shaderX)
                previous = bufferX.getColor0()
            }

            // y blur pass
            hi = hi shr 1
            shaderY.use()
            val bufferY = FBStack["bloomY", wi, hi, 4, true, 1, false]
            useFrame(bufferY, renderer) {
                previous.bindTrulyNearest(0)
                flat01.draw(shaderY)
                previous = bufferY.getColor0()
            }

            tmpForward[i] = bufferY

            // in the next steps with x use the actual shader,
            // which does not have the costly offset calculation
            shaderX = forwardShaderX

        }
        return maxSteps
    }

    private fun backwardPass(steps: Int): ITexture2D {
        val shader = ShaderLib.copyShader.value
        shader.use()
        shader.v1("am1", 0f)
        var previous = tmpForward[steps - 1]!!
        OpenGL.blendMode.use(BlendMode.PURE_ADD) {
            for (i in steps - 2 downTo 0) {// render onto that layer
                val nextSrc = tmpForward[i]!! // large
                useFrame(nextSrc, copyRenderer) {
                    previous.bindTexture0(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                    flat01.draw(shader)
                    previous = nextSrc
                }
            }
        }
        return previous.getColor0()
    }

    private fun createForwardShader(dx: Int, dy: Int, offset: Boolean): Shader {
        val halfSegments = 7
        val f = 1f / halfSegments
        val minInfluence = 0.001f
        val factor0 = log(minInfluence * 6f)
        val blur = StringBuilder(halfSegments * 2 * 40)
        val factors = FloatArray(halfSegments + 1) { exp(factor0 * sq(it * f)) }
        val inv = factors.sum() * 2f - factors[0]
        blur.append(factors[0] / inv)
        if (offset) blur.append("*minusOffset(texelFetch(tex,p,0).rgb)")
        else blur.append("*texelFetch(tex,p,0).rgb")
        blur.append('\n')
        for (i in 1..halfSegments) {
            val factor = factors[i] / inv
            val dxi = dx * i
            val dyi = dy * i
            blur.append('+')
            blur.append("")
            blur.append(factor)
            if (offset) blur.append("*(minusOffset(texelFetch(tex,p+ivec2(")
            else blur.append("*(texelFetch(tex,p+ivec2(")
            blur.append(dxi)
            blur.append(",")
            blur.append(dyi)
            if (offset) blur.append("),0).rgb)+minusOffset(texelFetch(tex,p-ivec2(")
            else blur.append("),0).rgb+texelFetch(tex,p-ivec2(")
            blur.append(dxi)
            blur.append(",")
            blur.append(dyi)
            if (offset) blur.append("),0).rgb))\n")
            else blur.append("),0).rgb)\n")
        }
        return Shader(
            "bloom0", null, simplestVertexShader, uvList, "" +
                    "out vec4 fragColor;\n" +
                    "uniform float offset;\n" +
                    "uniform sampler2D tex;\n" +
                    brightness +
                    "vec3 minusOffset(vec3 col){\n" +
                    "   float length = brightness(col);\n" +
                    "   return length > offset ? col * (length-offset) / length : vec3(0);\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec2 p = ivec2(gl_FragCoord.x${if (dx == 0) "" else "*2.0"},gl_FragCoord.y${if (dy == 0) "" else "*2.0"});\n" +
                    "   fragColor = vec4($blur, 1.0);\n" +
                    "}\n"
        )
    }

    private fun addBloom(source: ITexture2D, bloom: ITexture2D, strength: Float, applyToneMapping: Boolean) {
        val shader = compositionShader.value
        shader.use()
        shader.v1("applyToneMapping", applyToneMapping)
        shader.v1("strength", strength)
        source.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        bloom.bind(1, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
        flat01.draw(shader)
    }

    private val forwardShaderX = createForwardShader(1, 0, false)
    private val forwardShaderY = createForwardShader(0, 1, false)
    private val forwardShader0 = createForwardShader(1, 0, true)

    private val compositionShader = lazy {
        Shader(
            "bloom", null, simplestVertexShader, uvList, "" +
                    "out vec4 fragColor;\n" +
                    "uniform float strength;\n" +
                    "uniform bool applyToneMapping;\n" +
                    "uniform sampler2D base, bloom;\n" +
                    noiseFunc +
                    "void main(){\n" +
                    "   vec4 color0 = texture(base,uv);\n" +
                    "   vec3 color = color0.rgb + strength * texture(bloom, uv).rgb;\n" +
                    "   if(applyToneMapping){\n" +
                    "       color = color/(1.0+color) - random(uv) * ${1.0 / 255.0};\n" +
                    "   }\n" +
                    "   fragColor = vec4(color.rgb, 1.0);\n" +
                    "}\n"
        ).apply { setTextureIndices(listOf("base", "bloom")) }
    }

    fun bloom(source: ITexture2D, offset: Float, strength: Float, applyToneMapping: Boolean) {
        renderPurely {
            val steps = forwardPass(source, offset)
            val bloom = backwardPass(steps)
            addBloom(source, bloom, strength, applyToneMapping)
        }
    }

}