package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.actions.ActionNode

class DepthTestNode : ActionNode(
    "DepthTest",
    listOf("Texture", "Depth"),
    listOf("Texture", "Illuminated")
) {
    override fun executeAction() {
        val depth = (getInput(1) as? Texture)?.texOrNull ?: return
        val result = FBStack[name, depth.width, depth.height, 4, true, 1, DepthBufferType.NONE]
        GFXState.useFrame(result) {
            val shader = shader
            shader.use()
            shader.v1f("worldScale", RenderState.worldScale)
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            DepthTransforms.bindDepthUniforms(shader)
            depth.bindTrulyNearest(shader, "depthTex")
            SimpleBuffer.flat01.draw(shader)
        }
        setOutput(1, Texture(result.getTexture0()))
    }

    companion object {
        val shader = Shader(
            "dof", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "worldScale"),
                Variable(GLSLType.V3F, "cameraPosition"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + DepthTransforms.depthVars, "" +
                    ShaderLib.quatRot +
                    DepthTransforms.rawToDepth +
                    DepthTransforms.depthToPosition +
                    "void main() {\n" +
                    "   vec3 pos = cameraPosition + rawDepthToPosition(uv,texture(depthTex,uv).r) / worldScale;\n" +
                    "   result = vec4(fract(pos - 0.001),1.0);\n" +
                    "}\n"
        )
    }
}