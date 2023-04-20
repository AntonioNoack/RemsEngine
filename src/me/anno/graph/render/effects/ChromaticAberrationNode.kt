package me.anno.graph.render.effects

import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import org.joml.Vector2f

class ChromaticAberrationNode : ActionNode(
    "Chromatic Aberration",
    listOf(
        "Float", "Strength",
        "Float", "Power",
        "Vector2f", "ROffset",
        "Vector2f", "BOffset",
        "Texture", "Illuminated",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 1.0f) // strength
        setInput(2, 1.5f) // power
        setInput(3, Vector2f())
        setInput(4, Vector2f())
    }

    override fun executeAction() {
        val strength = (getInput(1) as Float) * 0.001f
        val color = ((getInput(5) as? Texture)?.tex as? Texture2D)
        if (color == null) {
            setOutput(Texture(missingTexture), 1)
        } else {
            val power = getInput(2) as Float
            val rOffset = getInput(3) as Vector2f
            val bOffset = getInput(4) as Vector2f
            val fp = color.isHDR
            val result = FBStack[name, color.w, color.h, 3, fp, 1, false]
            useFrame(result, copyRenderer) {
                val shader = shader
                shader.use()
                shader.v2f("rOffset", rOffset)
                shader.v2f("bOffset", bOffset)
                shader.v4f("params", color.w.toFloat() / color.h, 1f, strength, power)
                color.bind(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                flat01.draw(shader)
            }
            setOutput(Texture(result.getTexture0()), 1)
        }
    }

    companion object {
        val shader = Shader(
            "chromatic", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V2F, "rOffset"),
                Variable(GLSLType.V2F, "bOffset"),
                Variable(GLSLType.V4F, "params"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   vec2 uv2 = params.xy * (uv * 2.0 - 1.0);\n" +
                    "   vec2 duv = params.z * sign(uv2) * pow(abs(uv2),vec2(params.w));\n" +
                    "   vec2 ga = texture(colorTex,uv).ga;\n" +
                    "   result = vec4(texture(colorTex,uv-duv+rOffset).r,ga.x,texture(colorTex,uv+duv+bOffset).b,ga.y);\n" +
                    "}\n"
        ).setTextureIndices("colorTex", "depthTex") as Shader
    }
}