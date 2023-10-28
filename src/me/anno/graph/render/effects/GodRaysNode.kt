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
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import org.joml.Vector2f
import org.joml.Vector3f

class GodRaysNode : ActionNode(
    "God Rays",
    listOf(
        "Int", "Samples",
        "Vector3f", "Falloff", // todo can this look nice in the case of non-chromatic?
        "Vector3f", "Sun Color",
        "Vector2f", "Sun Position",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 128)
        setInput(2, Vector3f(0.01f))
        setInput(3, Vector3f(5f))
        setInput(4, Vector2f(0f, 0.1f))
    }

    private val framebuffer = Framebuffer(name, 1, 1, arrayOf(TargetType.FP16Target3))

    override fun onDestroy() {
        super.onDestroy()
        framebuffer.destroy()
    }

    override fun executeAction() {

        val samples = getInput(1) as Int
        val falloff = getInput(2) as Vector3f
        val sunColor = getInput(3) as Vector3f
        val sunPosition = getInput(4) as Vector2f
        val color = ((getInput(5) as? Texture)?.tex as? Texture2D) ?: return
        val depth = ((getInput(6) as? Texture)?.tex as? Texture2D) ?: return

        useFrame(color.width, color.height, true, framebuffer, copyRenderer) {
            val shader = shader
            shader.use()
            GFX.check()
            color.bind(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
            depth.bindTrulyNearest(1)
            GFX.check()
            shader.v3f("intensity", sunColor)
            shader.v3f("falloff", falloff)
            shader.v1i("samples", samples)
            shader.v2f("lightPos", sunPosition)
            shader.v1f("maxDensity", 1f) // >= 1f // todo what is this?
            GFX.check()
            SimpleBuffer.flat01.draw(shader)
            GFX.check()
        }

        val result = framebuffer.getTexture0()
        setOutput(1, Texture(result))
    }

    companion object {
        val shader = Shader(
            "god-rays", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V2F, "lightPos"),
                Variable(GLSLType.V3F, "falloff"),
                Variable(GLSLType.V3F, "intensity"),
                Variable(GLSLType.V1I, "samples"),
                Variable(GLSLType.V1F, "maxDensity"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   vec2 deltaUV = lightPos - uv;\n" +
                    "   vec2 texSize = vec2(textureSize(colorTex,0));\n" +
                    "   float pixelDistance = length(dot(abs(deltaUV), texSize));\n" +
                    "   float dist01 = pixelDistance / texSize.y;\n" + // [0,1]
                    "   int steps = max(1, min(int(pixelDistance * maxDensity), samples));\n" +
                    "   vec2 dir = deltaUV / float(steps);\n" +
                    "   vec2 uv2 = uv;\n" +
                    "   vec3 sum = vec3(0.0);\n" +
                    "   vec3 factor = intensity;\n" +
                    "   vec3 falloff2 = 1.0 - falloff;\n" +
                    // walk from light pos to our position
                    // todo use depth buffer to block rays
                    "   for(int i=0;i<steps;i++){\n" +
                    "       sum += factor;\n" + // todo only sun color is of importance
                    "       factor *= falloff2;\n" + // todo falloff depends on position
                    "       uv2 += dir;\n" +
                    "   }\n" +
                    "   result = texture(colorTex, uv) + vec4(sum, 0.0);\n" +
                    "}\n"
        ).setTextureIndices("colorTex", "depthTex") as Shader
    }
}