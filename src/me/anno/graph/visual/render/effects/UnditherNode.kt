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
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.isZWMapping
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.utils.GFXFeatures

/**
 * Removes dither pattern from 2x2-dithered materials.
 * Uses depth and normal information. Depth-dithering is typical, but normal-dithering is possible, too.
 * */
class UnditherNode : RenderViewNode(
    "Undither",
    listOf(
        "Texture", "Illuminated",
        "Texture", "Normal",
        "Texture", "Depth"
    ), listOf(
        "Texture", "Illuminated"
    )
) {

    override fun executeAction() {
        val colorT = getTextureInput(1)
        val normalT = getInput(2) as? Texture
        val depthT = getTextureInput(3)
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
                    if (normalT != null) {
                        normalT.tex.bindTrulyNearest(shader, "normalTex")
                        shader.v1b("normalZW", normalT.isZWMapping)
                    } else {
                        TextureLib.normalTexture.bindTrulyNearest(shader, "normalTex")
                        shader.v1b("normalZW", false)
                    }
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
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), rawToDepth + octNormalPacking +
                    // textureOffset isn't inlined, because it needs a constant expression at the end on some
                    // devices, and inlining isn't guaranteed.
                    "vec3 getColor(ivec2 uvi){\n" +
                    "   return texelFetch(colorTex,uvi,0).xyz;\n" +
                    "}\n" +
                    "vec3 getColorSq(ivec2 uvi){\n" +
                    "   vec3 color = getColor(uvi);\n" +
                    "   return color * color;\n" +
                    "}\n" +

                    "float getDepth(ivec2 uvi) {\n" +
                    "   float depth = rawToDepth(texelFetch(depthTex,uvi,0).x);\n" +
                    "   return log2(clamp(depth,1e-38,1e38));\n" +
                    "}\n" +

                    "vec3 getNormal(ivec2 uvi) {\n" +
                    "   vec4 raw = texelFetch(normalTex,uvi,0);\n" +
                    "   return UnpackNormal(normalZW ? raw.zw : raw.xy);\n" +
                    "}\n" +

                    // only blur if all neighbors are similar, but at the same time different to ourselves
                    "void main() {\n" +
                    "   ivec2 texSize = textureSize(colorTex,0), texSizeM1 = texSize - 1;\n" +
                    "   ivec2 uvi = ivec2(vec2(texSize)*uv);\n" +
                    "   ivec2 uvi0 = clamp(uvi+ivec2(1,0), ivec2(0,0), texSizeM1);\n" +
                    "   ivec2 uvi1 = clamp(uvi-ivec2(1,0), ivec2(0,0), texSizeM1);\n" +
                    "   ivec2 uvi2 = clamp(uvi+ivec2(0,1), ivec2(0,0), texSizeM1);\n" +
                    "   ivec2 uvi3 = clamp(uvi-ivec2(0,1), ivec2(0,0), texSizeM1);\n" +

                    // depth-dithering-detection
                    "   float d0 = getDepth(uvi0);\n" +
                    "   float d1 = getDepth(uvi1);\n" +
                    "   float d2 = getDepth(uvi2);\n" +
                    "   float d3 = getDepth(uvi3);\n" +
                    "   float maxV = max(max(d0,d1),max(d2,d3));\n" +
                    "   float minV = min(min(d0,d1),min(d2,d3));\n" +
                    "   bool blur = false;\n" +
                    "   if (maxV < minV + 0.2){\n" +
                    "       float dx = getDepth(uvi);\n" +
                    "       if (dx < minV || dx > maxV) {\n" +
                    "           blur = true;\n" +
                    "       }\n" +
                    "   }\n" +

                    // normal-dithering-detection
                    // todo this may be costly... a) measure its performance, b) if costly, add a flag for it
                    "   if(!blur && textureSize(normalTex,0) == texSize) {\n" +
                    "       vec3 n0 = getNormal(uvi0);\n" +
                    "       vec3 n1 = getNormal(uvi1);\n" +
                    "       vec3 n2 = getNormal(uvi2);\n" +
                    "       vec3 n3 = getNormal(uvi3);\n" +
                    "       float d01 = dot(n0,n1), d12 = dot(n1,n2), d23 = dot(n2,n3), d30 = dot(n3,n0);\n" +
                    "       float minDot = min(min(d01,d12),min(d23,d30));\n" +
                    "       if (minDot > 0.9) {\n" + // all neighbors are consistent
                    "           vec3 nx = getNormal(uvi);\n" +
                    "           float d0x = dot(n0,nx), d1x = dot(n1,nx), d2x = dot(n2,nx), d3x = dot(n3,nx);\n" +
                    "           float maxDot = max(max(d0x,d1x),max(d2x,d3x));\n" +
                    "           blur = maxDot + 0.1 < minDot;\n" + // different from all
                    "       }\n" +
                    "   }\n" +

                    // apply undithering
                    "   vec3 color = getColor(uvi);\n" +
                    "   if (blur) {\n" +
                    if (GFXFeatures.hasWeakGPU) {
                        "" +
                                "color = color * 0.5 + 0.125 * (\n" +
                                "    getColor(uvi+ivec2(1,0)) +\n" +
                                "    getColor(uvi-ivec2(1,0)) +\n" +
                                "    getColor(uvi+ivec2(0,1)) +\n" +
                                "    getColor(uvi-ivec2(0,1))\n" +
                                ");\n"
                    } else {
                        "" +
                                "color = (color * color) * 0.5 + 0.125 * (\n" +
                                "    getColorSq(uvi+ivec2(1,0)) +\n" +
                                "    getColorSq(uvi-ivec2(1,0)) +\n" +
                                "    getColorSq(uvi+ivec2(0,1)) +\n" +
                                "    getColorSq(uvi-ivec2(0,1))\n" +
                                ");\n" +
                                "color = sqrt(max(color,vec3(0.0)));\n"
                    } +
                    "   }\n" +
                    // blur ? vec3(1.0) : vec3(0.0)
                    "   result = vec4(color,1.0);\n" +
                    "}\n"
        )
    }
}