package me.anno.gpu.pipeline.transparency

import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.gpu.Blitting
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely2
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.shader.FlatShaders.DONT_READ_DEPTH
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.structures.maps.LazyMap

/**
 * Order-Independent Transparency for simple, diffuse layers;
 * with just one layer, it should be identical to depth-tested alpha blending:
 * with more layers, the colors should mix regardless of order (for now)
 *
 * todo we might want a third glass pass type: forward rendering and this blending, not just flat colors
 *  e.g. for fading in objects like when placing objects
 * */
class AlphaBlendPass : TransparentPass() {
    companion object {

        val transparentRenderer = object : Renderer("alphaBlend") {
            override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
                return listOf(
                    ShaderStage(
                        "alphaBlend",
                        listOf(
                            Variable(GLSLType.V3F, "finalColor"),
                            Variable(GLSLType.V1F, "finalAlpha", VariableMode.INMOD),
                            Variable(GLSLType.V4F, "finalTinting", VariableMode.OUT),
                        ), "" +
                                colorToLinear +
                                // this formula ensures that if a single layer is present,
                                // alpha-blending is applied according to the formula mix(oldColor, newColor, newAlpha)
                                "finalAlpha = clamp(finalAlpha, 0.0, 0.996);\n" + // prevent infinitely large factors
                                "float weight = finalAlpha / (1.0 - finalAlpha);\n" + // max factor ~ 250
                                "finalTinting = vec4(finalColor * weight, weight);\n"
                    )
                )
            }
        }

        val applyShader = LazyMap { multisampled: Boolean ->
            val sampleType = if (multisampled) GLSLType.S2DMS else GLSLType.S2D
            Shader(
                "applyAlphaBlend", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                    Variable(sampleType, "tintingTex"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT),
                ), "" +
                        (if (multisampled) "" +
                                "#define getTex(s) texelFetch(s,uvi,gl_SampleID)\n" else
                            "#define getTex(s) texture(s,uv)\n") +
                        (if (multisampled) "" +
                                "#define getTexD(s,duv) texelFetch(s,uvi+ivec2(round(duv)),gl_SampleID)\n" else
                            "#define getTexD(s,duv) texture(s,uv+duv/vec2(resolution))\n") +
                        "void main() {\n" +
                        "   ivec2 resolution = textureSize(tintingTex" +
                        (if (multisampled) ");\n" else ",0);\n") +
                        (if (multisampled) "" +
                                "ivec2 uvi = ivec2(uv*resolution);\n" else "") +
                        "   vec4 color = getTex(tintingTex);\n" +
                        "   color.rgb *= 1.0 / color.a;\n" +
                        "   color.rgb = sqrt(max(color.rgb, vec3(0.0)));\n" +
                        "   result = vec4(color.rgb, 1.0);\n" +
                        "}\n"
            )
        }
    }

    override fun renderTransparentStage(
        pipeline: Pipeline,
        stage: PipelineStageImpl,
        colorInput: ITexture2D,
        depthInput: ITexture2D
    ) {
        val old = GFXState.currentBuffer
        val tintingSum = getFramebufferWithAttachedDepth(listOf(TargetType.Float16x4))
        useFrame(old.width, old.height, true, tintingSum, transparentRenderer) {
            GFXState.depthMask.use(false) {

                colorInput.bindTrulyNearest(0)
                Blitting.copyColorAndDepth( // copy the color squared, and set alpha to 1.0
                    colorInput.samples, monochrome = false,
                    1, DONT_READ_DEPTH,
                    convertSRGBToLinear = true,
                    convertLinearToSRGB = false,
                    alphaOverride = 1f
                )

                val depthMode = if (GFX.supportsClipControl) DepthMode.CLOSE else DepthMode.FORWARD_CLOSE
                GFXState.depthMode.use(depthMode) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {
                        GFXState.drawLines.use(Mesh.drawDebugLines) {
                            stage.draw(pipeline)
                        }
                    }
                }
            }
        }

        renderPurely2 {
            val multisampled = tintingSum.samples > 1
            val shader = applyShader[multisampled]
            shader.use()
            tintingSum.getTextureIMS(0).bindTrulyNearest(shader, "tintingTex")
            flat01.draw(shader)
        }
    }
}