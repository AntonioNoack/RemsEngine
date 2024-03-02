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
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.graph.render.Texture
import me.anno.graph.render.scene.RenderViewNode
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput

/**
 * cheap and easy night effect;
 * please use proper color grading, and don't forget to make the sky dark, too;
 * */
class NightNode : RenderViewNode(
    "Night",
    listOf(
        "Float", "Strength",
        "Float", "Sky Darkening",
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 1f)
        setInput(2, 0.01f)
    }

    override fun executeAction() {
        val strength = getFloatInput(1)
        val skyDarkening = getFloatInput(2)
        val color0 = getInput(3) as? Texture
        val color = (color0?.tex as? Texture2D)
        val depth = ((getInput(4) as? Texture)?.tex as? Texture2D) ?: depthTexture
        if (color == null || strength <= 0f) {
            setOutput(1, color0 ?: Texture(missingTexture))
        } else {
            val result = FBStack[name, color.width, color.height, 3, true, 1, DepthBufferType.NONE]
            useFrame(result, copyRenderer) {
                val shader = shader
                shader.use()
                shader.v1f("exposure", 0.02f / strength)
                shader.v1f("skyDarkening", skyDarkening)
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
            "night", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "skyDarkening"),
                Variable(GLSLType.V1F, "exposure"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthVars, quatRot + rawToDepth +
                    "void main(){\n" +
                    // find all camera parameters like in SSAO, just in world space
                    "   float distance = rawToDepth(texture(depthTex,uv).x);\n" +
                    "   bool isFinite = distance < 1e38;\n" +
                    "   vec3 color = texture(colorTex,uv).xyz;\n" +
                    "   if(isFinite){\n" + // don't apply distance-based fog onto sky
                    "       vec3 nightColored = vec3(color.g) * vec3(0.21,0.26,0.5);\n" +
                    "       float brightness = color.r + color.g + color.b;\n" +
                    "       color = mix(color, nightColored, 1.0/(exposure*brightness*brightness+1.0));\n" +
                    "   } else color *= skyDarkening;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}