package me.anno.graph.visual.render.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.effects.FrameGenInitNode.Companion.interFrames

// todo make this work for VR
class FrameGenPredictiveNode : ActionNode(
    "FrameGenOutput", listOf(
        "Int", "Width",
        "Int", "Height",
        "Texture", "Illuminated",
        "Texture", "Motion",
        "Texture", "Depth",
    ), listOf(
        "Texture", "Illuminated"
    )
) {

    val color0 = Texture2D("frameGenC", 1, 1, 1)
    val motion = Texture2D("frameGenM", 1, 1, 1)
    val result = Texture2D("frameGenX", 1, 1, 1)

    private fun fill(width: Int, height: Int, data0: Texture2D, motion: Texture2D?) {
        data0.resize(width, height, TargetType.UInt8x3)
        data0.filtering = Filtering.TRULY_LINEAR
        data0.clamping = Clamping.CLAMP
        useFrame(data0) {
            GFX.copyNoAlpha((getInput(3) as? Texture)?.texOrNull ?: whiteTexture)
        }
        if (motion != null) {
            motion.resize(width, height, TargetType.Float32x2)
            useFrame(motion) {
                GFX.copyNoAlpha((getInput(4) as? Texture)?.texOrNull ?: blackTexture)
            }
        }
    }

    private fun showOutput(data0: Texture2D) {
        setOutput(1, Texture(data0, null, "xyz", DeferredLayerType.COLOR))
    }

    var frameIndex = Int.MAX_VALUE
    override fun executeAction() {
        val width = getIntInput(1)
        val height = getIntInput(2)
        if (frameIndex < interFrames) {
            val result = result
            result.resize(width, height, TargetType.UInt8x3)
            useFrame(result) {
                val shader = predictiveShader
                shader.use()
                // invalidate filtering
                color0.bindTrulyLinear(shader, "colorTex")
                motion.bindTrulyNearest(shader, "motionTex") // motion from frame 0 to 1
                // why do we need to invert it???
                shader.v1f("t", 1f - (frameIndex + 1f) / (interFrames + 1f))
                flat01.draw(shader)
            }
            showOutput(result)
            frameIndex++
        } else {
            // copy input onto data0
            fill(width, height, color0, motion)
            showOutput(color0)
            frameIndex = 0
        }
    }

    override fun destroy() {
        super.destroy()
        color0.destroy()
        motion.destroy()
        result.destroy()
    }

    companion object {
        val targets = listOf(TargetType.UInt8x4, TargetType.Float32x3)
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
                    "   vec2 uv0 = uv+duv*(0.5*(t-1.0));\n" + // todo why is clamping needed and not even working properly???
                    "   vec3 color = texture(colorTex, uv0).xyz;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}