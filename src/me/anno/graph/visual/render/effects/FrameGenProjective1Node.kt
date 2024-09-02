package me.anno.graph.visual.render.effects

import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

class FrameGenProjective1Node : FrameGenProjective0Node("FrameGenProjective1") {

    override fun renderInterpolated(view: PerViewProjData, width: Int, height: Int, fraction: Float) {
        val result = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.NONE]
        useFrame(result) {
            GFXState.depthMode.use(DepthMode.CLOSER) {
                val shader = predictiveShader
                bind(shader, view, width, height)
                flat01.draw(shader)
            }
        }
        showOutput(result.getTexture0())
    }

    companion object {
        val predictiveShader = Shader(
            "projective1", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.M4x4, "cameraMatrixI"),
                Variable(GLSLType.V3F, "cameraPositionI"),
                Variable(GLSLType.V3F, "cameraPosition0"),
                Variable(GLSLType.V1F, "worldScaleI"),
                Variable(GLSLType.V1F, "worldScale0"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + DepthTransforms.depthVars, "" +
                    DepthTransforms.rawToDepth +
                    DepthTransforms.depthToPosition +
                    "void main() {\n" +
                    "   float depth = clamp(texture(depthTex,uv,0).x, 1e-3, 1e15);\n" + // must be clamped to avoid Inf/NaN
                    "   vec3 pos = depthToPosition(uv,depth);\n" +
                    // could be simplified, is just 1x scale, 1x translation
                    "   pos = (pos/worldScale0)+cameraPosition0;\n" +
                    "   pos = (pos-cameraPositionI)*worldScaleI;\n" +
                    "   vec4 proj = matMul(cameraMatrixI,vec4(pos,1.0));\n" +
                    "   vec2 uv0 = 2.0 * uv - (proj.xy/proj.w*0.5+0.5);\n" +
                    "   result = texture(colorTex,uv0,0);\n" +
                    "}\n"
        )
    }
}