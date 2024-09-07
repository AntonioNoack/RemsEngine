package me.anno.graph.visual.render.effects.framegen

import me.anno.cache.ICacheData
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

// todo implement realtime-reactivity like FrameGenProjectiveNode
class FrameGenPredictiveNode : FrameGenOutputNode<FrameGenPredictiveNode.PerViewData2>(
    "FrameGenPredictive", listOf(
        "Int", "Width",
        "Int", "Height",
        "Texture", "Illuminated",
        "Texture", "Motion",
    ), listOf("Texture", "Illuminated")
) {

    class PerViewData2 : ICacheData {
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

    override fun canInterpolate(view: PerViewData2): Boolean {
        return view.color.isCreated() && view.motion.isCreated()
    }

    override fun renderOriginal(view: PerViewData2, width: Int, height: Int) {
        // copy input onto data0
        fill(width, height, view.color, 3, TargetType.UInt8x3, whiteTexture)
        fill(width, height, view.motion, 4, TargetType.Float16x2, blackTexture)
        showOutput(view.color)
    }

    override fun renderInterpolated(view: PerViewData2, width: Int, height: Int, fraction: Float) {
        val result = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.NONE]
        useFrame(result) {
            val shader = predictiveShader
            shader.use()
            // invalidate filtering
            view.color.bindTrulyLinear(shader, "colorTex")
            view.motion.bindTrulyNearest(shader, "motionTex")
            shader.v1f("duvScale", -0.5f * fraction) // why is negative correct???
            flat01.draw(shader)
        }
        showOutput(result.getTexture0())
    }

    companion object {
        val predictiveShader = Shader(
            "predictive", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "duvScale"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ), "" +
                    "void main() {\n" +
                    "   vec2 duv = texture(motionTex,uv).xy;\n" +
                    "   vec2 uv0 = uv+duv*duvScale;\n" +
                    "   vec3 color = texture(colorTex, uv0).xyz;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}