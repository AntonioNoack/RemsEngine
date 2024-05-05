package me.anno.graph.render.effects

import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.graph.render.Texture
import me.anno.graph.render.scene.RenderViewNode
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput

/**
 * draws anime outlines where a large depth difference is
 * */
class AnimeOutlineNode : RenderViewNode(
    "Anime Outline",
    listOf(
        "Float", "Strength",
        "Float", "Sensitivity",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 0.5f)
        setInput(2, 500f)
    }

    override fun executeAction() {
        val strength = getFloatInput(1)
        val sensitivity = getFloatInput(2)
        val color0 = getInput(3) as? Texture
        val color = color0?.texOrNull
        val depth = (getInput(4) as? Texture)?.texOrNull
        if (color == null || depth == null || sensitivity <= 0f || strength <= 0f) {
            setOutput(1, color0 ?: Texture(missingTexture))
        } else {
            val result = FBStack[name, color.width, color.height, 3, true, 1, DepthBufferType.NONE]
            useFrame(result, copyRenderer) {
                val shader = shader
                shader.use()
                shader.v1f("sensitivity", sensitivity)
                shader.v1f("maxDarkness", 1f - strength)
                color.bindTrulyNearest(shader, "colorTex")
                depth.bindTrulyNearest(shader, "depthTex")
                (renderView.pipeline.bakedSkybox?.getTexture0() ?: whiteCube)
                    .bind(shader, "skyTex", Filtering.LINEAR, Clamping.CLAMP)
                bindDepthUniforms(shader)
                flat01.draw(shader)
            }
            setOutput(1, Texture(result.getTexture0()))
        }
    }

    companion object {
        val shader = Shader(
            "anime-outlines", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "maxDarkness"),
                Variable(GLSLType.V1F, "sensitivity"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthVars, quatRot + rawToDepth +
                    "float sample(int dx,int dy){\n" +
                    "    return min(1e37,rawToDepth(textureOffset(depthTex,uv,ivec2(dx,dy)).x));\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   float v0 = sample(0,0), px = sample(1,0), mx = sample(-1,0), py = sample(0,1), my = sample(0,-1);\n" +
                    "   float div = 1.0 / max(v0,max(max(px,mx),max(py,my)));\n" +
                    "   float dx = (2.0 * v0 - (px + mx)) * div;\n" +
                    "   float dy = (2.0 * v0 - (py + my)) * div;\n" +
                    "   float notLine = max(1.0 - sensitivity * (dx*dx+dy*dy), maxDarkness);\n" +
                    "   vec3 color = texture(colorTex,uv).xyz;\n" +
                    "   result = vec4(color * notLine, 1.0);\n" +
                    "}\n"
        )
    }
}