package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.RenderMode.Companion.createHDRPostProcessGraph
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texOrNull

class ColorBlindnessNode(var mode: ColorBlindnessMode) : TimedRenderingNode(
    "ColorBlindness",
    listOf("Texture", "Illuminated", "Float", "Strength"),
    listOf("Texture", "Illuminated")
) {

    init {
        setInput(2, 1f) // default strength
    }

    override fun executeAction() {
        val color = getInput(1) as? Texture
        val strength = getFloatInput(2)
        val result = if (strength != 0f) {
            val source = color.texOrNull ?: return
            val result = FBStack[name, source.width, source.height, 4, true, 1, DepthBufferType.NONE]
            timeRendering(name, timer) {
                GFXState.useFrame(result) {
                    val shader = shader
                    shader.use()
                    shader.v1i("mode", mode.id)
                    shader.v1f("strength", strength)
                    source.bindTrulyNearest(0)
                    SimpleBuffer.flat01.draw(shader)
                }
            }
            Texture(result.getTexture0())
        } else color
        setOutput(1, result)
    }

    companion object {

        /**
         * from https://gist.github.com/jcdickinson/580b7fb5cc145cee8740, http://www.daltonize.org/search/label/Daltonize
         * */
        private val shader = Shader(
            "colorblindness", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1I, "mode"),
                Variable(GLSLType.V1F, "strength"),
                Variable(GLSLType.S2D, "source")
            ), ShaderLib.brightness +
                    "void main(){\n" +
                    "   vec4 color = texture(source, uv);\n" +
                    "   if(mode == ${ColorBlindnessMode.GRAYSCALE.id}){\n" +
                    "       gl_FragColor = mix(color, vec4(vec3(brightness(color.rgb)), color.a), strength);\n" +
                    "   } else {\n" +
                    // RGB to LMS matrix conversion
                    "       float L = dot(vec3(17.8824,   43.5161,   4.11935), color.rgb);\n" +
                    "       float M = dot(vec3( 3.45565,  27.1554,   3.86714), color.rgb);\n" +
                    "       float S = dot(vec3( 0.0299566, 0.184309, 1.46709), color.rgb);\n" +
                    // Simulate color blindness
                    // Protanopia - reds are greatly reduced (1% men)
                    "       vec3 lms;\n" +
                    "       switch(mode){\n" +
                    "       case ${ColorBlindnessMode.PROTANOPIA.id}:\n" +
                    "           lms = vec3(2.02344 * M - 2.52581 * S, M, S);\n" +
                    "           break;\n" +
                    // Deuteranopia - greens are greatly reduced (1% men)
                    "       case ${ColorBlindnessMode.DEUTERANOPIA.id}:\n" +
                    "           lms = vec3(L, 0.494207 * L + 1.24827 * S, S);\n" +
                    "           break;\n" +
                    // Tritanopia - blues are greatly reduced (0.003% population)
                    "       default:\n" +
                    "           lms = vec3(L, M, -0.395913 * L + 0.801109 * M);\n" +
                    "           break;\n" +
                    "       }\n" +
                    // LMS to RGB matrix conversion
                    "       gl_FragColor = vec4(mix(color.rgb, vec3(\n" +
                    "           dot(vec3( 0.0809444479,   -0.130504409,   0.116721066), lms),\n" +
                    "           dot(vec3(-0.0102485335,    0.0540193266, -0.113614708), lms),\n" +
                    "           dot(vec3(-0.000365296938, -0.00412161469, 0.693511405), lms)\n" +
                    "       ), strength), color.a);\n" +
                    "   }" +
                    "}\n"
        )

        fun createRenderGraph(mode: ColorBlindnessMode): FlowGraph {
            return createHDRPostProcessGraph(ColorBlindnessNode(mode))
        }
    }
}