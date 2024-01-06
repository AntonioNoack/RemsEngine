package me.anno.graph.render.effects

import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms.bindDepthToPosition
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

class DepthToNormalNode : ActionNode(
    "Depth To Normal",
    listOf(
        "Texture", "Depth",
        "Int", "Precision"
    ),
    listOf("Texture", "Normal")
) {

    override fun executeAction() {
        val depth = getInput(1) as? Texture ?: return
        val target = when (getInput(2) as Int) {
            1 -> TargetType.Float16x2
            2 -> TargetType.Float32x2
            else -> TargetType.UInt8x2
        }
        val depthTex = depth.tex
        val result = FBStack[name, depthTex.width, depthTex.height, target, 1, DepthBufferType.NONE]
        GFXState.useFrame(result) {
            val shader = shader
            shader.use()
            shader.v4f("depthMask", depth.mask!!)
            depthTex.bindTrulyNearest(0)
            bindDepthToPosition(shader)
            flat01.draw(shader)
        }
        setOutput(1, Texture.texture(result, 0, "rg", DeferredLayerType.NORMAL))
    }

    companion object {
        val shader = Shader(
            "depthToNormal", coordsList, coordsUVVertexShader, uvList,
            listOf(
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthVars, "" +
                    quatRot +
                    rawToDepth +
                    depthToPosition +
                    octNormalPacking +
                    "void main(){\n" +
                    "   float rawDepth = dot(texture(depthTex,uv), depthMask);\n" +
                    "   vec3 pos = rawDepthToPosition(uv, max(rawDepth, 1e-10));\n" +
                    // sky doesn't have depth information / is at infinity -> different algorithm for it
                    "   vec3 normal = rawDepth < 1e-10 ? -rawCameraDirection(uv) : cross(dFdx(pos),dFdy(pos));\n" +
                    "   result = vec4(PackNormal(normal),0.0,1.0);\n" +
                    "}\n"
        )
    }
}