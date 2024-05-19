package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.popDrawCallName
import me.anno.gpu.GFXState.pushDrawCallName
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.actions.ActionNode
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
        val strength = getFloatInput(1) * 0.001f
        val color = (getInput(5) as? Texture)?.texOrNull
        if (color == null) {
            setOutput(1, Texture(missingTexture))
        } else {
            pushDrawCallName(name)
            val power = getFloatInput(2)
            val rOffset = getInput(3) as Vector2f
            val bOffset = getInput(4) as Vector2f
            val fp = color.isHDR
            val result = FBStack[name, color.width, color.height, 3, fp, 1, DepthBufferType.NONE]
            useFrame(result, copyRenderer) {
                val shader = shader
                shader.use()
                shader.v2f("rOffset", rOffset)
                shader.v2f("bOffset", bOffset)
                shader.v4f("params", color.width.toFloat() / color.height, 1f, strength, power)
                color.bindTrulyLinear(shader, "colorTex")
                flat01.draw(shader)
            }
            setOutput(1, Texture(result.getTexture0()))
            popDrawCallName()
        }
    }

    companion object {
        val shader = Shader(
            "chromatic", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
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
        )
    }
}