package me.anno.gpu.pipeline.transparency

import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.structures.tuples.IntPair

/**
 * order-independent transparency for deferred rendering;
 * issue: lights are not tinted by lights before them
 * */
class GlassPass : TransparentPass() {

    // todo refractions (of non-transparent objects) :3
    //  this would need a whole copy of the scene with all buffers :/
    // theoretically needs a search, again...
    // depends on normal

    //
    //   diffuse *= glass color       | diffuse *= [exp(sum(log(glassColor[i]))) = product(glassColor[i])]
    //  emissive += glass reflection  | emissive += sum(glassReflection[i]) // times previous glass color...
    //  depth stays the same

    companion object {

        val GlassRenderer = object : Renderer(
            "glass", DeferredSettingsV2(
                listOf(DeferredLayerType.COLOR, DeferredLayerType.EMISSIVE),
                1, false
            )
        ) {
            override fun getPostProcessing(flags: Int): List<ShaderStage> {
                val vars = pbrRenderer.getPostProcessing(flags).first().variables.filter { !it.isOutput }
                return listOf(ShaderStage("glass",
                    vars, "" +
                            colorToLinear +
                            RendererLib.lightCode + // calculates the light onto this surface, stores diffuseLight and specularLight
                            RendererLib.combineLightCode +
                            RendererLib.skyMapCode +
                            colorToSRGB +
                            "finalEmissive = finalColor * finalAlpha;\n" + // reflections
                            "finalColor = -log(finalColor0) * finalAlpha;\n" + // diffuse tinting ; todo light needs to get tinted by closer glass-panes...
                            "finalAlpha = 1.0;\n"
                ))
            }
        }

        // override diffuseColor and finalEmissive in shader
        val applyShader = LazyMap<IntPair, Shader> {
            Shader(
                "applyGlass", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList, listOf(
                    Variable(GLSLType.S2D, "diffuseSrcTex"),
                    Variable(GLSLType.S2D, "emissiveSrcTex"),
                    Variable(GLSLType.S2D, "diffuseGlassTex"),
                    Variable(GLSLType.S2D, "emissiveGlassTex"),
                    Variable(GLSLType.V3F, "refX"),
                    Variable(GLSLType.V3F, "refY"),
                    Variable(GLSLType.V4F, "diffuse", VariableMode.OUT).apply { slot = it.first },
                    Variable(GLSLType.V4F, "emissive", VariableMode.OUT).apply { slot = it.second },
                ), "" +
                        "void main() {\n" +
                        "   vec4 tint = vec4(exp(-texture(diffuseGlassTex,uv).rgb),1.0);\n" +
                        "   diffuse  = texture(diffuseSrcTex,uv) * tint;\n" +
                        "   emissive = texture(emissiveSrcTex,uv) * tint + vec4(texture(emissiveGlassTex,uv).rgb,0.0);\n" +
                        "}\n"
            )
        }
    }

    override fun draw1(pipeline: Pipeline) {

        val b0 = GFXState.currentBuffer
        val tmp = getFB(arrayOf(TargetType.FP16Target3, TargetType.FP16Target3))
        useFrame(b0.width, b0.height, true, tmp, GlassRenderer) {
            tmp.clearColor(0)
            GFXState.depthMode.use(DepthMode.CLOSER) {
                GFXState.depthMask.use(false) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {
                        draw2(pipeline)
                    }
                }
            }
        }

        val r0 = GFXState.currentRenderer

        val s0 = r0.deferredSettings!!

        val l0 = s0.findLayer(DeferredLayerType.COLOR)!!
        val l1 = s0.findLayer(DeferredLayerType.EMISSIVE)!!

        combine {
            val shader = applyShader[IntPair(l0.texIndex, l1.texIndex)]
            shader.use()

            // bind all textures
            s0.findTexture(b0, l0).bindTrulyNearest(shader, "diffuseSrcTex")
            s0.findTexture(b0, l1).bindTrulyNearest(shader, "emissiveSrcTex")
            tmp.getTextureI(0).bindTrulyNearest(shader, "diffuseGlassTex")
            tmp.getTextureI(1).bindTrulyNearest(shader, "emissiveGlassTex")

            flat01.draw(shader)
        }

        tmp.destroy()

    }

}