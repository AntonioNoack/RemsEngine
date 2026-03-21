package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texOrNull

class ToneMappingNode : TimedRenderingNode(
    "ToneMapping",
    listOf(
        "Texture", "Illuminated",
        "Float", EXPOSURE_NAME, // set to a value of 0 to disable tone-mapping
    ),
    listOf("Texture", "Illuminated")
) {

    init {
        setInput(2, 1f)
    }

    override fun executeAction() {
        val color = getInput(1) as? Texture
        val exposure = getFloatInput(2)
        val result = if (exposure > 0f) {
            val source = color.texOrNull ?: return
            Texture(applyToneMapping(source, exposure, name, timer))
        } else color
        setOutput(1, result)
    }

    companion object {

        @Suppress("MayBeConstant")
        val EXPOSURE_NAME = "Tone Mapping Exposure"

        fun applyToneMapping(
            source: ITexture2D, exposure: Float,
            name: String, timer: GPUClockNanos
        ): ITexture2D {
            val result = FBStack[name, source.width, source.height, 4, false, 1, DepthBufferType.NONE]
            timeRendering(name, timer) {
                GFXState.useFrame(result) {
                    val shader = toneMappingShader
                    shader.use()
                    shader.v1f("exposure", exposure)
                    source.bindTrulyNearest(0)
                    SimpleBuffer.flat01.draw(shader)
                }
            }
            return result.getTexture0()
        }

        private val toneMappingShader = Shader(
            "tonemapping", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "exposure"),
                Variable(GLSLType.S2D, "source"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ),
            tonemapGLSL +
                    "void main(){\n" +
                    "   vec4 color = texture(source, uv);\n" +
                    "   result = vec4(tonemap(exposure * color.rgb), color.a);\n" +
                    "}\n"
        )
    }
}