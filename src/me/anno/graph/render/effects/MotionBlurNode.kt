package me.anno.graph.render.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

class MotionBlurNode : ActionNode(
    "Motion Blur",
    listOf(
        "Int", "Samples",
        "Float", "Shutter",
        "Texture", "Illuminated",
        "Texture", "Motion",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 16)
        setInput(2, 0.5f)
    }

    private val framebuffer = Framebuffer(name, 1, 1, arrayOf(TargetType.FP16Target3))

    override fun onDestroy() {
        super.onDestroy()
        framebuffer.destroy()
    }

    override fun executeAction() {

        val samples = getInput(1) as Int
        val shutter = getInput(2) as Float
        val color = ((getInput(3) as? Texture)?.tex as? Texture2D) ?: missingTexture
        val motion = ((getInput(4) as? Texture)?.tex as? Texture2D) ?: blackTexture

        useFrame(color.width, color.height, true, framebuffer, copyRenderer) {
            val shader = shader
            shader.use()
            GFX.check()
            color.bind(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
            motion.bindTrulyNearest(1)
            GFX.check()
            shader.v1i("samples", samples)
            shader.v1f("shutter", shutter)
            GFX.check()
            SimpleBuffer.flat01.draw(shader)
            GFX.check()
        }

        val result = framebuffer.getTexture0()
        setOutput(Texture(result), 1)

    }


    companion object {
        // definitely not ideal; we'd need to smear moving objects instead of smearing on moving objects
        val shader = Shader(
            "MotionBlur", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1I, "samples"),
                Variable(GLSLType.V1F, "shutter"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   vec2 motion = 0.5 * shutter * texture(motionTex,uv).xy;\n" + // 0.5, because we sample from both sides
                    "   float length = length(motion * textureSize(motionTex,0));\n" +
                    "   int samplesI = min(samples, int(round(length)));\n" +
                    "   vec4 res = texture(colorTex, uv);\n" +
                    "   if(samplesI > 1){\n" +
                    "       vec2 uv2 = uv, uv3 = uv;\n" +
                    "       vec2 duv = motion / float(samplesI-1);\n" +
                    "       res *= 2.0;\n" +
                    "       for(int i=1;i<samplesI;i++){\n" +
                    "           uv2 += duv;\n" +
                    "           uv3 -= duv;\n" +
                    "           res += texture(colorTex, uv2) + texture(colorTex, uv3);\n" +
                    "       }\n" +
                    "       res /= float(samplesI);\n" +
                    "       res *= 0.5;\n" +
                    "   }\n" +
                    "   result = res;\n" +
                    "}\n"
        ).setTextureIndices("colorTex", "motionTex") as Shader
    }
}