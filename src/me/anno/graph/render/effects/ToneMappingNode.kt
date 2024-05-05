package me.anno.graph.render.effects

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.types.flow.actions.ActionNode

class ToneMappingNode : ActionNode(
    "ToneMapping",
    listOf("Texture", "Illuminated", "Float", "Exposure", "Boolean", "Apply"),
    listOf("Texture", "Illuminated")
) {

    init {
        setInput(2, 1f)
        setInput(3, true)
    }

    override fun executeAction() {
        val color = getInput(1) as? Texture
        val result = if (getBoolInput(3)) {
            val exposure = getFloatInput(2)
            val source = color?.texOrNull ?: return
            val result = FBStack[name, source.width, source.height, 4, false, 1, DepthBufferType.NONE]
            GFXState.useFrame(result) {
                val shader = shader
                shader.use()
                shader.v1f("exposure", exposure)
                source.bindTrulyNearest(0)
                SimpleBuffer.flat01.draw(shader)
            }
            Texture(result.getTexture0())
        } else color
        setOutput(1, result)
    }

    companion object {
        val shader = Shader(
            "tonemapping", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
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