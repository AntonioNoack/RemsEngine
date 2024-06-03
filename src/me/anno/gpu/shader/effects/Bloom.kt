package me.anno.gpu.shader.effects

import me.anno.engine.ui.render.Renderers
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.FlatShaders
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.gamma
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.maths.Maths
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.iff
import kotlin.math.exp

object Bloom {

    // bloom like in
    //  - https://www.youtube.com/watch?v=tI70-HIc5ro&ab_channel=TheCherno
    //  - https://github.com/Unity-Technologies/Graphics/blob/master/com.unity.postprocessing/PostProcessing/Shaders/Builtins/Bloom.shader
    //  - http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare

    // how many iterations are executed at maximum
    private const val maxSteps = 16 // equal to (65k)² pixels

    // minimum buffer size
    private const val minSize = 8

    // temporary buffer for this object
    private val tmpForward = arrayOfNulls<IFramebuffer>(maxSteps)

    private fun forwardPass(source: ITexture2D, strength: Float, offset: Float): Int {

        var wi = source.width
        var hi = source.height
        var previous = source

        var shaderX = forwardShader0
        val shaderY = forwardShaderY

        shaderX.use()
        shaderX.v1f("strength", strength)
        shaderX.v1f("offset", offset)

        val renderer = Renderer.copyRenderer

        for (i in 0 until maxSteps) {

            if (i > 0 && (wi < minSize || hi < minSize)) return i

            // x blur pass
            wi = Maths.max((wi + 1) shr 1, 1)
            shaderX.use()
            val bufferX = FBStack["bloomX", wi, hi, 4, BufferQuality.FP_16, 1, DepthBufferType.NONE]
            GFXState.useFrame(bufferX, renderer) {
                previous.bindTrulyNearest(0)
                flat01.draw(shaderX)
                previous = bufferX.getTexture0()
            }

            // y blur pass
            hi = Maths.max((hi + 1) shr 1, 1)
            shaderY.use()
            val bufferY = FBStack["bloomY", wi, hi, 4, BufferQuality.FP_16, 1, DepthBufferType.NONE]
            GFXState.useFrame(bufferY, renderer) {
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
        val shader = FlatShaders.copyShader
        shader.use()
        shader.v1f("alpha", 1f)
        var previous = tmpForward[steps - 1]!!
        GFXState.blendMode.use(BlendMode.PURE_ADD) {
            for (i in steps - 2 downTo 0) {// render onto that layer
                val nextSrc = tmpForward[i]!! // large
                GFXState.useFrame(nextSrc, Renderer.copyRenderer) {
                    previous.bindTexture0(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
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
        val factor0 = Maths.log(minInfluence * 6f)
        val blur = StringBuilder(halfSegments * 2 * 40)
        val factors = FloatArray(halfSegments + 1) { exp(factor0 * Maths.sq(it * f)) }
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
            "bloom0", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V4F, "fragColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "offset"),
                Variable(GLSLType.V1F, "strength"),
                Variable(GLSLType.S2D, "tex")
            ), "" +
                    ShaderLib.brightness +
                    "vec3 minusOffset(vec3 col){\n" +
                    "   float length = dot(col, vec3(0.299, 0.587, 0.114));\n" +
                    "   return length > offset ? col * (length-offset) / length : vec3(0);\n" +
                    "}\n" +
                    "vec3 loadColor(vec3 srgb){\n" +
                    "   return pow(srgb,vec3($gamma));\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec2 p = ivec2(gl_FragCoord.x${if (dx == 0) "" else "*2.0"},gl_FragCoord.y${if (dy == 0) "" else "*2.0"});\n" +
                    (if (offset) "fragColor = vec4(minusOffset($blur) * strength, 1.0);\n" else
                        "fragColor = vec4($blur, 1.0);\n") +
                    "}\n"
        )
    }

    private fun addBloom(source: ITexture2D, bloom: ITexture2D, applyToneMapping: Boolean) {
        val shader = compositionShader[(source.samples > 1).toInt()]
        shader.use()
        shader.v1b("applyToneMapping", applyToneMapping)
        shader.v1i("numSamples", source.samples)
        shader.v1f("invNumSamples", 1f / source.samples)
        source.bindTrulyNearest(0)
        bloom.bind(1, Filtering.TRULY_LINEAR, Clamping.CLAMP)
        flat01.draw(shader)
    }

    private val forwardShaderX = createForwardShader(1, 0, false)
    private val forwardShaderY = createForwardShader(0, 1, false)
    private val forwardShader0 = createForwardShader(1, 0, true)

    private val compositionShader = Array(2) {
        val msIn = it > 0
        val shader = Shader(
            "composeBloom", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
                Variable(GLSLType.V1B, "applyToneMapping"),
                Variable(if (msIn) GLSLType.S2DMS else GLSLType.S2D, "base"),
                Variable(GLSLType.S2D, "bloom"),
                Variable(GLSLType.V1I, "numSamples"),
                Variable(GLSLType.V1F, "invNumSamples")
            ), "" +
                    ShaderFuncLib.randomGLSL +
                    Renderers.tonemapGLSL +
                    "void main(){\n" +
                    "   vec3 sum = vec3(0.0), bloom = texture(bloom, uv).rgb;\n" +
                    "   ivec2 uvi = ivec2(gl_FragCoord.xy);\n".iff(msIn) +
                    "   for(int i=0;i<numSamples;i++){\n".iff(msIn) +
                    "       vec3 color = texture(base,uv).rgb;\n".iff(!msIn) +
                    "       vec3 color = texelFetch(base,uvi,i).rgb;\n".iff(msIn) +
                    "       color = pow(max(color,vec3(0.0)),vec3(2));\n" + // srgb -> linear
                    "       color += bloom;\n" +
                    "       if(applyToneMapping) {\n" +
                    "           color = tonemapLinear(color);\n" +
                    "       }\n" +
                    "       sum += color;\n" +
                    "   }\n".iff(msIn) +
                    "   sum *= invNumSamples;\n".iff(msIn) +
                    "   sum = sqrt(sum);\n" + // linear -> srgb
                    "   result = vec4(sum, 1.0);\n" +
                    "}\n"
        )
        shader.setTextureIndices("base", "bloom")
        shader.ignoreNameWarnings("numSamples,invNumSamples")
        shader
    }

    fun bloom(source: ITexture2D, sourceMS: ITexture2D, offset: Float, strength: Float, applyToneMapping: Boolean) {
        GFXState.renderPurely {
            val steps = forwardPass(source, strength, offset)
            val bloom = backwardPass(steps)
            addBloom(sourceMS, bloom, applyToneMapping)
        }
    }
}