package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.Renderers
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import kotlin.math.abs

/**
 * Apply LUT color map, ideally directly apply tone mapping within this node to avoid banding.
 * */
class LUTColorMapNode : RenderViewNode(
    "LUT Color Map",
    listOf(
        "Float", "LUT Strength",
        "Float", "Tone Mapping Exposure",
        "Bool", "Apply Tone Mapping",
        "File", "LUT Source",
        "Texture", "Illuminated",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 1f)
        setInput(2, 1f)
        setInput(3, false)
        setInput(4, InvalidRef)
    }

    override fun executeAction() {
        val strength = getFloatInput(1)
        val toneMappingExposure = getFloatInput(2)
        val applyToneMapping = getBoolInput(3)
        val lutSource = getInput(4) as? FileReference ?: InvalidRef
        val lut = TextureCache.getLUT(lutSource)
        val color0 = getInput(5) as? Texture
        val color1 = color0.texOrNull
        if (color1 == null || lut == null || abs(strength) < 1e-7f) {
            val result = if (color1 != null) {
                Texture(ToneMappingNode.applyToneMapping(color1, toneMappingExposure, name, timer))
            } else {
                color0 ?: Texture(missingTexture)
            }
            setOutput(1, result)
            return
        }
        timeRendering(name, timer) {
            val result = FBStack[name, color1.width, color1.height, 3, false, 1, DepthBufferType.NONE]
            useFrame(result, copyRenderer) {
                val shader = lutShader
                shader.use()
                shader.v1f("strength", strength)
                shader.v1f("exposure", toneMappingExposure)
                shader.v1b("applyToneMapping", applyToneMapping)
                color1.bindTrulyNearest(shader, "colorTex")
                lut.bindTrulyLinear(shader, "lutTex")
                (renderView.pipeline.bakedSkybox?.getTexture0() ?: whiteCube)
                    .bind(shader, "skyTex", Filtering.LINEAR, Clamping.CLAMP)
                bindDepthUniforms(shader)
                flat01.draw(shader)
            }
            setOutput(1, Texture(result.getTexture0()))
        }
    }

    companion object {
        private val lutShader = Shader(
            "lut", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "strength"),
                Variable(GLSLType.V1F, "exposure"),
                Variable(GLSLType.V1B, "applyToneMapping"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S3D, "lutTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), Renderers.tonemapGLSL +
                    "void main() {\n" +
                    "   vec3 color = texture(colorTex,uv).rgb;\n" +
                    "   if(applyToneMapping) color = tonemap(exposure * color);\n" +
                    // mixing in sRGB instead of linear isn't the best, but the LUT is sRGB anyway
                    "   color = mix(color, texture(lutTex,color,0.0).rgb, strength);\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        )
    }
}