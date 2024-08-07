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
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.effects.FrameGenInitNode.Companion.interFrames

// todo make this work for VR
class FrameGenMixingNode : ActionNode(
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

    var color0 = Texture2D("frameGen0", 1, 1, 1)
    var color1 = Texture2D("frameGen1", 1, 1, 1)
    val motion = Texture2D("frameGenM", 1, 1, 1)
    val result = Texture2D("frameGenX", 1, 1, 1)

    private fun fill(width: Int, height: Int, data0: Texture2D, motion: Texture2D?) {
        data0.resize(width, height, TargetType.UInt8x3)
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
                val shader = interpolateShader
                shader.use()
                color0.bindTrulyNearest(shader, "colorTex0")
                color1.bindTrulyNearest(shader, "colorTex1")
                motion.bindTrulyNearest(shader, "motionTex") // motion from frame 0 to 1
                // why do we need to invert it???
                shader.v1f("t", 1f - (frameIndex + 1f) / (interFrames + 1f))
                flat01.draw(shader)
            }
            showOutput(result)
            frameIndex++
        } else {
            val data0 = color0
            val data1 = color1
            // copy input onto data0
            fill(width, height, data1, motion)
            if (data0.pointer == 0) {
                fill(width, height, data0, null)
            }
            this.color1 = data0
            this.color0 = data1
            showOutput(data0)
            frameIndex = 0
        }
    }

    override fun destroy() {
        super.destroy()
        color0.destroy()
        color1.destroy()
        motion.destroy()
        result.destroy()
    }

    companion object {
        val targets = listOf(TargetType.UInt8x4, TargetType.Float32x3)
        val interpolateShader = Shader(
            "interpolate", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "t"),
                Variable(GLSLType.S2D, "colorTex0"),
                Variable(GLSLType.S2D, "colorTex1"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ), "" +
                    "bool isOutOfBounds(vec2 uvi){\n" +
                    "   return uvi.x < 0.0 || uvi.x > 1.0 || uvi.y < 0.0 || uvi.y > 1.0;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   vec2 duv = texture(motionTex,uv).xy;\n" +
                    "   vec2 uv0 = uv+duv*(0.5*t);\n" +
                    "   vec2 uv1 = uv+duv*(0.5*(t-1.0));\n" +
                    "   vec3 color0 = texture(colorTex0, uv0).xyz;\n" +
                    "   vec3 color1 = texture(colorTex1, uv1).xyz;\n" +
                    // todo can we eliminate ghosting???
                    "   vec3 color = mix(color0, color1, t);\n" +
                    // if one is out of bounds, use the other
                    "   if(isOutOfBounds(uv0)) color = color1;\n" +
                    "   if(isOutOfBounds(uv1)) color = color0;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}