package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
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
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.effects.TimedRenderingNode.Companion.finish
import me.anno.graph.visual.render.scene.RenderViewNode

/**
 * Cheap and easy night effect.
 *
 * Strength and sky brightness factor can be configured by adding a NightSettings instance to your scene.
 * */
class NightNode : RenderViewNode(
    "Night",
    listOf(
        "Texture", "Illuminated",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    override fun executeAction() {

        val settings = GlobalSettings[NightSettings::class]

        val color1 = getTextureInput(1) ?: return finish()
        val depth = getTextureInput(2, depthTexture)
        if (settings.strength <= 0f) return finish(color1)

        timeRendering(name, timer) {
            val result = FBStack[name, color1.width, color1.height, 3, true, 1, DepthBufferType.NONE]
            useFrame(result, copyRenderer) {
                val shader = simpleNightShader
                shader.use()
                shader.v1f("exposure", 0.02f / settings.strength)
                shader.v1f("skyDarkening", settings.skyBrightnessFactor)
                color1.bindTrulyNearest(shader, "colorTex")
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
        private val simpleNightShader = Shader(
            "night", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
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
                    "   if (isFinite) {\n" + // don't apply distance-based fog onto sky
                    "       vec3 nightColored = vec3(color.g) * vec3(0.21,0.26,0.5);\n" +
                    "       float brightness = color.r + color.g + color.b;\n" +
                    "       color = mix(color, nightColored, 1.0/(exposure*brightness*brightness+1.0));\n" +
                    "   } else color *= skyDarkening;\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}