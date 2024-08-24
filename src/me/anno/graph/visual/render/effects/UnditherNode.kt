package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.scene.RenderViewNode

/**
 * removes dither pattern from 2x2-dithered materials
 * */
class UnditherNode : RenderViewNode(
    "Undither",
    listOf("Texture", "Illuminated", "Texture", "Depth"),
    listOf("Texture", "Illuminated")
) {

    override fun executeAction() {
        val colorT = (getInput(1) as? Texture).texOrNull
        val depthT = (getInput(2) as? Texture).texOrNull
        if (colorT != null && depthT != null) {
            timeRendering(name, timer) {
                val width = colorT.width
                val height = colorT.height
                val samples = 1
                val framebuffer = FBStack[name, width, height, 3, colorT.isHDR, samples, DepthBufferType.NONE]
                useFrame(framebuffer, copyRenderer) {
                    val shader = shader
                    shader.use()
                    bindDepthUniforms(shader)
                    colorT.bindTrulyNearest(shader, "colorTex")
                    depthT.bindTrulyNearest(shader, "depthTex")
                    flat01.draw(shader)
                }
                setOutput(1, Texture.texture(framebuffer, 0))
            }
        } else setOutput(1, getInput(1))
    }

    companion object {
        val shader = Shader(
            "undithering",
            emptyList(), coordsUVVertexShader, uvList, depthVars + listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), rawToDepth + "" +
                    // textureOffset isn't inlined, because it needs a constant expression at the end on some
                    // devices, and inlining isn't guaranteed.
                    "vec3 getColor(ivec2 uv){\n" +
                    "   ivec2 uvi = clamp(uv, ivec2(0,0), textureSize(colorTex,0)-1);\n" +
                    "   return texelFetch(colorTex,uvi,0).xyz;\n" +
                    "}\n" +
                    "float getDepth(ivec2 uv){\n" +
                    "   ivec2 uvi = clamp(uv, ivec2(0,0), textureSize(depthTex,0)-1);\n" +
                    "   float depth = rawToDepth(texelFetch(depthTex,uvi,0).x);\n" +
                    "   return log2(clamp(depth,1e-38,1e38));\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec2 uvi = ivec2(vec2(textureSize(colorTex,0))*uv);\n" +
                    "   vec3 color = getColor(uvi);\n" +
                    "   float d0 = getDepth(uvi+ivec2(1,0));\n" +
                    "   float d1 = getDepth(uvi-ivec2(1,0));\n" +
                    "   float d2 = getDepth(uvi+ivec2(0,1));\n" +
                    "   float d3 = getDepth(uvi-ivec2(0,1));\n" +
                    "   float maxV = max(max(d0,d1),max(d2,d3));\n" +
                    "   float minV = min(min(d0,d1),min(d2,d3));\n" +
                    // only blur if all neighbors are similar, but at the same time different to ourselves
                    "   if(maxV < minV + 0.2){\n" +
                    "       float dx = getDepth(uvi);\n" +
                    "       if(dx < minV || dx > maxV){\n" +
                    "           color = color * 0.5 + 0.125 * (\n" +
                    "               getColor(uvi+ivec2(1,0)) +\n" +
                    "               getColor(uvi-ivec2(1,0)) +\n" +
                    "               getColor(uvi+ivec2(0,1)) +\n" +
                    "               getColor(uvi-ivec2(0,1))\n" +
                    "           );\n" +
                    "       }\n" +
                    "   }\n" +
                    "   result = vec4(color,1.0);\n" +
                    "}\n"
        )

        init {
            shader.ignoreNameWarnings("d_camRot,d_uvCenter")
        }
    }
}