package me.anno.ecs.components.shaders.effects

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.FlatShaders.copyShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.maths.Maths.log
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
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
    private const val minSize = 8

    // temporary buffer for this object
    private val tmpForward = arrayOfNulls<Framebuffer>(maxSteps)

    private fun forwardPass(source: ITexture2D, strength: Float, offset: Float): Int {

        var wi = source.width
        var hi = source.height
        var previous = source

        var shaderX = forwardShader0
        val shaderY = forwardShaderY

        shaderX.use()
        shaderX.v1f("strength", strength)
        shaderX.v1f("offset", offset)

        val renderer = copyRenderer

        for (i in 0 until maxSteps) {

            if (i > 0 && (wi < minSize || hi < minSize)) return i

            // x blur pass
            wi = max((wi + 1) shr 1, 1)
            shaderX.use()
            val bufferX = FBStack["bloomX", wi, hi, 4, BufferQuality.HIGH_16, 1, false]
            useFrame(bufferX, renderer) {
                previous.bindTrulyNearest(0)
                flat01.draw(shaderX)
                previous = bufferX.getTexture0()
            }

            // y blur pass
            hi = max((hi + 1) shr 1, 1)
            shaderY.use()
            val bufferY = FBStack["bloomY", wi, hi, 4, BufferQuality.HIGH_16, 1, false]
            useFrame(bufferY, renderer) {
                previous.bindTrulyNearest(0)
                flat01.draw(shaderY)
                previous = bufferY.getTexture0()
            }

            tmpForward[i] = bufferY

            // in the next steps with x use the actual shader,
            // which does not have the costly offset calculation
            shaderX = forwardShaderX
        }
        return maxSteps
    }

    private fun backwardPass(steps: Int): ITexture2D {
        val shader = copyShader
        shader.use()
        shader.v1f("alpha", 1f)
        var previous = tmpForward[steps - 1]!!
        GFXState.blendMode.use(BlendMode.PURE_ADD) {
            for (i in steps - 2 downTo 0) {// render onto that layer
                val nextSrc = tmpForward[i]!! // large
                useFrame(nextSrc, copyRenderer) {
                    previous.bindTexture0(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                    flat01.draw(shader)
                    previous = nextSrc
                }
            }
        }
        return previous.getTexture0()
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
        if (offset) blur.append("*loadColor(texelFetch(tex,p,0).rgb)")
        else blur.append("*texelFetch(tex,p,0).rgb")
        blur.append('\n')
        for (i in 1..halfSegments) {
            val factor = factors[i] / inv
            val dxi = dx * i
            val dyi = dy * i
            blur.append('+')
            blur.append(factor)
            if (offset) blur.append("*(loadColor(texelFetch(tex,p+ivec2(")
            else blur.append("*(texelFetch(tex,p+ivec2(")
            blur.append(dxi)
            blur.append(",")
            blur.append(dyi)
            if (offset) blur.append("),0).rgb)+loadColor(texelFetch(tex,p-ivec2(")
            else blur.append("),0).rgb+texelFetch(tex,p-ivec2(")
            blur.append(dxi)
            blur.append(",")
            blur.append(dyi)
            if (offset) blur.append("),0).rgb))\n")
            else blur.append("),0).rgb)\n")
        }
        return Shader(
            "bloom0", coordsList, coordsVShader, uvList,
            listOf(
                Variable(GLSLType.V4F, "fragColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "offset"),
                Variable(GLSLType.V1F, "strength"),
                Variable(GLSLType.S2D, "tex")
            ), "" +
                    brightness +
                    "vec3 minusOffset(vec3 col){\n" +
                    "   float length = dot(col, vec3(0.299, 0.587, 0.114));\n" +
                    "   return length > offset ? col * (length-offset) / length : vec3(0);\n" +
                    "}\n" +
                    "vec3 loadColor(vec3 srgb){\n" +
                    "   return pow(srgb,vec3(2.2));\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec2 p = ivec2(gl_FragCoord.x${if (dx == 0) "" else "*2.0"},gl_FragCoord.y${if (dy == 0) "" else "*2.0"});\n" +
                    (if (offset) "fragColor = vec4(minusOffset($blur) * strength, 1.0);\n" else
                        "fragColor = vec4($blur, 1.0);\n") +
                    "}\n"
        )
    }

    private fun addBloom(source: ITexture2D, bloom: ITexture2D, applyToneMapping: Boolean) {
        val shader = compositionShader.value
        shader.use()
        shader.v1b("applyToneMapping", applyToneMapping)
        source.bindTrulyNearest(0)
        bloom.bind(1, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
        flat01.draw(shader)
    }

    private val forwardShaderX = createForwardShader(1, 0, false)
    private val forwardShaderY = createForwardShader(0, 1, false)
    private val forwardShader0 = createForwardShader(1, 0, true)

    private val compositionShader = lazy {
        Shader(
            "bloom", coordsList, coordsVShader, uvList,
            listOf(
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
                Variable(GLSLType.V1B, "applyToneMapping"),
                Variable(GLSLType.S2D, "base"),
                Variable(GLSLType.S2D, "bloom")
            ), "" +
                    noiseFunc +
                    tonemapGLSL +
                    "void main(){\n" +
                    "   vec3 color0 = pow(texture(base,uv).rgb,vec3(2.2));\n" +
                    "   vec3 color1 = color0 + texture(bloom, uv).rgb;\n" +
                    // todo noise should depend on used destination
                    // - random(uv) * ${1.0 / 255.0}
                    "   if(applyToneMapping) color1 = tonemapLinear(color1);\n" +
                    "   color1 = pow(color1,vec3(1.0/2.2));\n" +
                    "   result = vec4(color1.rgb, 1.0);\n" +
                    "}\n"
        ).apply { setTextureIndices("base", "bloom") }
    }

    fun bloom(source: ITexture2D, offset: Float, strength: Float, applyToneMapping: Boolean) {
        renderPurely {
            val steps = forwardPass(source, strength, offset)
            val bloom = backwardPass(steps)
            addBloom(source, bloom, applyToneMapping)
        }
    }
}