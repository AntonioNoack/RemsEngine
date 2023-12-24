package me.anno.gpu.pipeline.transparency

import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.maths.Maths.hasFlag
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.toInt

/**
 * order-independent transparency for deferred rendering;
 * issue: glass panes are not tinted by panes before them
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
            "glass", DeferredSettings(
                listOf(DeferredLayerType.COLOR, DeferredLayerType.EMISSIVE)
            )
        ) {
            override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
                val vars = pbrRenderer.getPixelPostProcessing(flags).first().variables.filter { !it.isOutput }
                return listOf(
                    ShaderStage(
                        "glass",
                        vars + listOf(Variable(GLSLType.V1F, "IOR")), "" +
                                colorToLinear +
                                RendererLib.lightCode + // calculates the light onto this surface, stores diffuseLight and specularLight
                                RendererLib.combineLightCode +
                                RendererLib.skyMapCode +
                                colorToSRGB +
                                "float fresnel = fresnelSchlick(abs(dot(finalNormal,normalize(finalPosition))), gl_FrontFacing ? 1.0 / IOR : IOR);\n" +
                                "finalAlpha *= fresnel;\n" +
                                "finalEmissive = finalColor * finalAlpha;\n" + // reflections
                                "finalColor = -log(finalColor0) * finalAlpha;\n" + // diffuse tinting ; todo light needs to get tinted by closer glass-panes...
                                "finalAlpha = 1.0;\n"
                    ).add(
                        "" +
                                "float fresnelSchlick(float cosine, float ior) {\n" +
                                "   float r0 = (1.0 - ior) / (1.0 + ior);\n" +
                                "   r0 = r0 * r0;\n" +
                                "   return r0 + (1.0 - r0) * pow(1.0 - cosine, 5.0);\n" +
                                "}\n"
                    ).add(sampleSkyboxForAmbient)
                )
            }
        }

        // override diffuseColor and finalEmissive in shader
        val applyShader = LazyMap<Int, Shader> { bits ->
            val diffuseSlot = bits.and(0xff)
            val emissiveSlot = bits.ushr(8).and(0xff)
            val multisampled = bits.hasFlag(1 shl 16)
            val sampleType = if (multisampled) GLSLType.S2DMS else GLSLType.S2D
            Shader(
                "applyGlass", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                    Variable(sampleType, "diffuseSrcTex"),
                    Variable(sampleType, "emissiveSrcTex"),
                    Variable(sampleType, "diffuseGlassTex"),
                    Variable(sampleType, "emissiveGlassTex"),
                    Variable(GLSLType.V3F, "refX"),
                    Variable(GLSLType.V3F, "refY"),
                    Variable(GLSLType.V4F, "diffuse", VariableMode.OUT).apply { slot = diffuseSlot },
                    Variable(GLSLType.V4F, "emissive", VariableMode.OUT).apply { slot = emissiveSlot },
                ), "" +
                        (if(multisampled) "" +
                                "#define getTex(s) texelFetch(s,uvi,gl_SampleID)\n" else
                                    "#define getTex(s) texture(s,uv)\n") +
                        "void main() {\n" +
                        (if(multisampled) "" +
                                "ivec2 uvi = ivec2(uv*textureSize(diffuseGlassTex));\n" else "") +
                        "   vec4 tint = vec4(exp(-getTex(diffuseGlassTex).rgb),1.0);\n" +
                        "   diffuse  = getTex(diffuseSrcTex) * tint;\n" +
                        "   emissive = getTex(emissiveSrcTex) * tint + vec4(getTex(emissiveGlassTex).rgb,0.0);\n" +
                        "}\n"
            )
        }
    }

    override fun draw1(pipeline: Pipeline) {

        val b0 = GFXState.currentBuffer
        val tmp = getFB(arrayOf(TargetType.FP16Target3, TargetType.FP16Target3))
        useFrame(b0.width, b0.height, true, tmp, GlassRenderer) {
            tmp.clearColor(0)
            GFXState.depthMode.use(DepthMode.CLOSE) {
                GFXState.depthMask.use(false) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {
                        draw2(pipeline)
                    }
                }
            }
        }

        val r0 = GFXState.currentRenderer

        val s0 = r0.deferredSettings

        val l0 = s0?.findLayer(DeferredLayerType.COLOR)
        val l1 = s0?.findLayer(DeferredLayerType.EMISSIVE)

        combine {
            val diffuseSlot = l0?.texIndex ?: 0
            val emissiveSlot = l1?.texIndex ?: 1
            val bits = diffuseSlot or emissiveSlot.shl(8) or (tmp.samples > 1).toInt(1 shl 16)
            val shader = applyShader[bits]
            shader.use()

            // bind all textures
            (s0?.findTextureMS(b0, l0) ?: whiteTexture).bindTrulyNearest(shader, "diffuseSrcTex")
            (s0?.findTextureMS(b0, l1) ?: blackTexture).bindTrulyNearest(shader, "emissiveSrcTex")
            tmp.getTextureIMS(0).bindTrulyNearest(shader, "diffuseGlassTex")
            tmp.getTextureIMS(1).bindTrulyNearest(shader, "emissiveGlassTex")

            flat01.draw(shader)
        }

        tmp.destroy()
    }
}