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

// todo make this work for VR
// todo can we mix this with FSR2?
class FrameGenMixingNode : FrameGenOutputNode<FrameGenMixingNode.PerViewData1>(
    "FrameGenMixing", listOf(
        "Int", "Width",
        "Int", "Height",
        "Texture", "Illuminated",
        "Texture", "Motion",
    ), listOf("Texture", "Illuminated")
) {

    class PerViewData1 : PerViewData() {
        var color0 = Texture2D("frameGen0", 1, 1, 1)
        var color1 = Texture2D("frameGen1", 1, 1, 1)
        val motion = Texture2D("frameGenM", 1, 1, 1)
        override fun destroy() {
            color0.destroy()
            color1.destroy()
            motion.destroy()
        }
    }

    override fun createPerViewData(): PerViewData1 {
        return PerViewData1()
    }

    override fun renderOriginal(view: PerViewData1, width: Int, height: Int) {
        val color0 = view.color0
        val color1 = view.color1
        val motion = view.motion
        // copy input onto data0
        fill(width, height, color1, 3, TargetType.UInt8x3, whiteTexture)
        fill(width, height, motion, 4, TargetType.Float16x2, blackTexture)
        if (color0.pointer == 0) {
            fill(width, height, color0, 3, TargetType.UInt8x3, whiteTexture)
        }
        view.color1 = color0
        view.color0 = color1
        showOutput(color0)
    }

    override fun renderInterpolated(view: PerViewData1, width: Int, height: Int) {
        val result = FBStack["frameGen", width, height, TargetType.UInt8x3, 1, DepthBufferType.NONE]
        useFrame(result) {
            val shader = interpolateShader
            shader.use()
            view.color0.bindTrulyLinear(shader, "colorTex0")
            view.color1.bindTrulyLinear(shader, "colorTex1")
            view.motion.bindTrulyNearest(shader, "motionTex") // motion from frame 0 to 1
            // why do we need to invert it???
            shader.v1f("t", 1f - (view.frameIndex + 1f) / (interFrames + 1f))
            flat01.draw(shader)
        }
        showOutput(result.getTexture0())
    }

    companion object {
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
                    //  depth-plausibility: moved color must have roughly the same depth
                    "   vec3 color = mix(color0, color1, t);\n" +
                    // if one is out of bounds, use the other
                    "   if(isOutOfBounds(uv0)) color = color1;\n" +
                    "   if(isOutOfBounds(uv1)) color = color0;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}