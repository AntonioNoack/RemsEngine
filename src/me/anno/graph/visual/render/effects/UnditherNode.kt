package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.popDrawCallName
import me.anno.gpu.GFXState.pushDrawCallName
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.scene.RenderViewNode

/**
 * removes dither pattern from 2x2-dithered materials
 * */
class UnditherNode : RenderViewNode(
    "Undither",
    listOf(
        "Texture", "Illuminated",
        "Texture", "Depth",
    ),
    listOf("Texture", "Illuminated")
) {

    override fun executeAction() {

        val colorT = (getInput(1) as? Texture)?.texOrNull ?: return
        val depthT = (getInput(2) as? Texture)?.texOrNull ?: return

        val width = colorT.width
        val height = depthT.height
        val samples = 1

        pushDrawCallName(name)
        val framebuffer = FBStack[name, width, height, 4, colorT.isHDR, samples, DepthBufferType.NONE]
        useFrame(framebuffer, copyRenderer) {
            val shader = shader
            shader.use()
            bindDepthUniforms(shader)
            shader.v2f("duv", 1f / width, 1f / height)
            colorT.bindTrulyNearest(shader, "colorTex")
            depthT.bindTrulyNearest(shader, "depthTex")
            flat01.draw(shader)
        }
        setOutput(1, Texture.texture(framebuffer, 0))
        popDrawCallName()
    }

    companion object {
        val shader = Shader(
            "undithering",
            coordsList, coordsUVVertexShader, uvList, depthVars + listOf(
                Variable(GLSLType.V2F, "duv"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), rawToDepth + "" +
                    "float getDepth(vec2 duv){\n" +
                    "   float depth = rawToDepth(texture(depthTex,uv+duv).x);\n" +
                    "   return log2(clamp(depth,1e-38,1e38));\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   vec3 color = texture(colorTex,uv).xyz;\n" +
                    "   float d0 = getDepth(vec2(-duv.x,0.0));\n" +
                    "   float d1 = getDepth(vec2(+duv.x,0.0));\n" +
                    "   float d2 = getDepth(vec2(0.0,-duv.y));\n" +
                    "   float d3 = getDepth(vec2(0.0,+duv.y));\n" +
                    "   float max = max(max(d0,d1),max(d2,d3));\n" +
                    "   float min = min(min(d0,d1),min(d2,d3));\n" +
                    // only blur if all neighbors are similar, but at the same time different to ourselves
                    "   if(max < min + 0.2){\n" +
                    "       float dx = getDepth(vec2(0.0));\n" +
                    "       if(dx < min || dx > max){\n" +
                    "           color = color * 0.5 + 0.125 * (\n" +
                    "               texture(colorTex,uv+vec2(-duv.x,0.0)).xyz +\n" +
                    "               texture(colorTex,uv+vec2(+duv.x,0.0)).xyz +\n" +
                    "               texture(colorTex,uv+vec2(0.0,-duv.y)).xyz +\n" +
                    "               texture(colorTex,uv+vec2(0.0,+duv.y)).xyz\n" +
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