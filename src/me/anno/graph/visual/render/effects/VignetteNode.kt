package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.visual.render.Texture
import me.anno.maths.Maths.length
import me.anno.utils.Color.black4
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.min

/**
 * Blends the border with a specified color.
 * */
class VignetteNode : TimedRenderingNode(
    "Vignette",
    listOf(
        "Texture", "Illuminated",
        "Vector4f", "Color",
        "Float", "Power",
        "Float", "Strength",
    ),
    listOf("Texture", "Illuminated")
) {

    init {
        setInput(2, black4)
        setInput(3, 3f)
        setInput(4, 1f)
    }

    override fun executeAction() {
        val color = getInput(1) as? Texture
        val strength = getFloatInput(4)
        val result = if (strength > 0f) {
            val colorT = color?.texOrNull ?: return
            val result = FBStack[name, colorT.width, colorT.height, 4, colorT.isHDR, 1, DepthBufferType.NONE]
            timeRendering(name, timer) {
                useFrame(result) {
                    val shader = shader
                    shader.use()
                    val scaleX = 2f * max(colorT.width, colorT.height) /
                            length(colorT.width.toFloat(), colorT.height.toFloat())
                    shader.v2f(
                        "scale",
                        scaleX * min(colorT.width / colorT.height.toFloat(), 1f),
                        scaleX * min(colorT.height / colorT.width.toFloat(), 1f),
                    )
                    shader.v4f("color", getInput(2) as Vector4f)
                    shader.v1f("power", getFloatInput(3))
                    shader.v1f("strength", strength)
                    colorT.bindTrulyNearest(0)
                    SimpleBuffer.flat01.draw(shader)
                }
            }
            Texture(result.getTexture0())
        } else color
        setOutput(1, result)
    }

    companion object {
        private val shader = Shader(
            "vignette", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V4F, "color"),
                Variable(GLSLType.V2F, "scale"),
                Variable(GLSLType.V1F, "power"),
                Variable(GLSLType.V1F, "strength"),
                Variable(GLSLType.S2D, "sourceTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), ShaderLib.brightness +
                    "void main(){\n" +
                    "   vec4 srcColor = texture(sourceTex, uv);\n" +
                    "   float vignetting = pow(length((uv-.5)*scale) * strength, power);\n" +
                    "   result = mix(srcColor, color, clamp(vignetting, 0.0, 1.0));\n" +
                    "}\n"
        )
    }
}