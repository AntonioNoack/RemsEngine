package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.gpu.GFXState.timeRendering
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

/**
 * Adds chromatic aberration (color fringes) as a post-process effect.
 *
 * Add a ChromaticAberrationSettings instance to your scene to configure this.
 * */
class ChromaticAberrationNode : TimedRenderingNode(
    "Chromatic Aberration",
    listOf("Texture", "Illuminated"),
    listOf("Texture", "Illuminated")
) {

    override fun executeAction() {
        val settings = GlobalSettings[ChromaticAberrationSettings::class]
        val strength = settings.strength * 0.001f
        val color = getTextureInput(1)
        if (color == null || strength <= 0f) {
            setOutput(1, Texture(color ?: missingTexture))
        } else {
            timeRendering(name, timer) {
                val fp = color.isHDR
                val result = FBStack[name, color.width, color.height, 3, fp, 1, DepthBufferType.NONE]
                useFrame(result, copyRenderer) {
                    val shader = shader
                    shader.use()
                    shader.v2f("rOffset", settings.rOffset)
                    shader.v2f("bOffset", settings.bOffset)
                    shader.v4f("params", color.width.toFloat() / color.height, 1f, strength, settings.power)
                    color.bindTrulyLinear(shader, "colorTex")
                    flat01.draw(shader)
                }
                setOutput(1, Texture(result.getTexture0()))
            }
        }
    }

    companion object {
        val shader = Shader(
            "chromatic", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
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