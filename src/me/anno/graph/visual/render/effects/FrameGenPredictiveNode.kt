package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.effects.FrameGenInitNode.Companion.interFrames

// todo test this in VR
class FrameGenPredictiveNode : FrameGenOutputNode<FrameGenPredictiveNode.PerViewData2>(
    "FrameGenPredictive", listOf(
        "Int", "Width",
        "Int", "Height",
        "Texture", "Illuminated",
        "Texture", "Motion",
    ), listOf("Texture", "Illuminated")
) {

    class PerViewData2 : PerViewData() {
        val color = Texture2D("frameGenC", 1, 1, 1)
        val motion = Texture2D("frameGenM", 1, 1, 1)
        override fun destroy() {
            color.destroy()
            motion.destroy()
        }
    }

    override fun createPerViewData(): PerViewData2 {
        return PerViewData2()
    }

    override fun renderOriginal(view: PerViewData2, width: Int, height: Int) {
        // copy input onto data0
        fill(width, height, view.color, 3, TargetType.UInt8x3, whiteTexture)
        fill(width, height, view.motion, 4, TargetType.Float16x2, blackTexture)
        showOutput(view.color)
    }

    override fun renderInterpolated(view: PerViewData2, width: Int, height: Int) {
        val result = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.NONE]
        useFrame(result) {
            val shader = predictiveShader
            shader.use()
            // invalidate filtering
            view.color.bindTrulyLinear(shader, "colorTex")
            view.motion.bindTrulyNearest(shader, "motionTex")
            // why do we need to invert it???
            shader.v1f("t", 1f - (view.frameIndex + 1f) / (interFrames + 1f))
            flat01.draw(shader)
        }
        showOutput(result.getTexture0())
    }

    companion object {
        val predictiveShader = Shader(
            "predictive", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "t"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ), "" +
                    "void main() {\n" +
                    "   vec2 duv = texture(motionTex,uv).xy;\n" +
                    "   vec2 uv0 = uv+duv*(0.5*(t-1.0));\n" +
                    "   vec3 color = texture(colorTex, uv0).xyz;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}