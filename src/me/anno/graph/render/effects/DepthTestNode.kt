package me.anno.graph.render.effects

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
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

class DepthTestNode : ActionNode(
    "DepthTest",
    listOf("Texture", "Depth"),
    listOf("Texture", "Illuminated")
) {
    override fun executeAction() {
        val depth = ((getInput(1) as? Texture)?.tex as? Texture2D) ?: return
        val result = FBStack[name, depth.width, depth.height, 4, true, 1, DepthBufferType.NONE]
        GFXState.useFrame(result) {
            val shader = shader
            shader.use()
            shader.v1f("worldScale", RenderState.worldScale)
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            DepthTransforms.bindDepthToPosition(shader)
            depth.bindTrulyNearest(shader, "depthTex")
            SimpleBuffer.flat01.draw(shader)
        }
        setOutput(1, Texture(result.getTexture0()))
    }

    companion object {
        val shader = Shader(
            "dof", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
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