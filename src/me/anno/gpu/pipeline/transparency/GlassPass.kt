package me.anno.gpu.pipeline.transparency

import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib
import me.anno.engine.ui.render.RendererLib.fresnelSchlick
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely2
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.utils.structures.maps.LazyMap

/**
 * order-independent transparency for deferred rendering;
 * issue: glass panes are not tinted by panes before them
 * */
class GlassPass : TransparentPass() {
    companion object {
        val GlassRenderer = object : Renderer(
            "glass", DeferredSettings(
                listOf(
                    // todo better semantic naming for these
                    DeferredLayerType.COLOR,
                    DeferredLayerType.ALPHA,
                    DeferredLayerType.EMISSIVE,
                    DeferredLayerType.METALLIC,
                    DeferredLayerType.CLEAT_COAT_ROUGH_METALLIC,
                )
            )
        ) {
            override fun bind(shader: Shader) {
                super.bind(shader)
                shader.v3f("dirX", RenderState.cameraDirectionRight)
                shader.v3f("dirY", RenderState.cameraDirectionUp)
            }

            override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
                val vars = pbrRenderer.getPixelPostProcessing(flags).first().variables.filter { !it.isOutput }
                return listOf(
                    ShaderStage( // todo glass needs to be shadowed @sunlight
                        "glass",
                        vars + listOf(
                            Variable(GLSLType.V1F, "IOR"),
                            Variable(GLSLType.V3F, "dirX"),
                            Variable(GLSLType.V3F, "dirY"),
                        ), "" +
                                colorToLinear +
                                RendererLib.lightCode + // calculates the light onto this surface, stores diffuseLight and specularLight
                                RendererLib.combineLightCode +
                                RendererLib.skyMapCode +
                                colorToSRGB +
                                "float milkiness = 0.5 * finalRoughness;\n" +
                                "float iorValue = gl_FrontFacing ? 1.0 / IOR : IOR;\n" +
                                "float fresnel = fresnelSchlick(abs(dot(finalNormal,normalize(finalPosition))), iorValue);\n" +
                                "finalAlpha = mix(fresnel, 1.0, milkiness);\n" +
                                "finalEmissive = finalColor * finalAlpha;\n" + // reflections
                                "finalColor = -log(finalColor0) * finalAlpha;\n" + // diffuse tinting, product
                                "finalMetallic = -log(IOR) * finalAlpha;\n" +
                                "finalClearCoatRoughMetallic = vec2(dot(finalNormal,dirX), dot(finalNormal,dirY)) * finalAlpha;\n"
                    )
                        .add(fresnelSchlick)
                        .add(getReflectivity)
                        .add(sampleSkyboxForAmbient)
                )
            }
        }

        // override diffuseColor and finalEmissive in shader
        val applyShader = LazyMap<Boolean, Shader> { multisampled ->
            val sampleType = if (multisampled) GLSLType.S2DMS else GLSLType.S2D
            Shader(
                "applyGlass", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                    Variable(sampleType, "diffuseSrcTex"),
                    Variable(sampleType, "diffuseGlassTex"),
                    Variable(sampleType, "emissiveGlassTex"),
                    Variable(sampleType, "normalGlassTex"),
                    Variable(sampleType, "surfaceGlassTex"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT),
                ), "" +
                        (if (multisampled) "" +
                                "#define getTex(s) texelFetch(s,uvi,gl_SampleID)\n" else
                            "#define getTex(s) texture(s,uv)\n") +
                        (if (multisampled) "" +
                                "#define getTexD(s,duv) texelFetch(s,uvi+ivec2(round(duv)),gl_SampleID)\n" else
                            "#define getTexD(s,duv) texture(s,uv+duv/vec2(resolution))\n") +
                        "void main() {\n" +
                        "   ivec2 resolution = textureSize(diffuseGlassTex" +
                        (if (multisampled) ");\n" else ",0);\n") +
                        (if (multisampled) "" +
                                "ivec2 uvi = ivec2(uv*resolution);\n" else "") +
                        "   vec4 diffuseData = getTex(diffuseGlassTex);\n" +
                        // skip all calculations, if we don't look at glass
                        "   if(diffuseData.a <= 0.0){\n" +
                        "       result = getTex(diffuseSrcTex);\n" +
                        "   } else {\n" +
                        "       vec4 emissiveData = getTex(emissiveGlassTex);\n" +
                        "       float tr = clamp(diffuseData.a,0.0,1.0);\n" +
                        "       vec3 tint = exp(-diffuseData.rgb);\n" +
                        "       vec4 surface = getTex(surfaceGlassTex);\n" +
                        "       float normFactor = 1.0 / (diffuseData.a + 0.01);\n" +
                        "       vec2 normalData = getTex(normalGlassTex).xy;\n" +
                        // only a part is refracted -> mix it in -> how much?
                        "       vec2 refractUV = -normalData * emissiveData.a * normFactor;\n" +
                        "       vec3 diffuse = vec3(\n" +
                        "           getTexD(diffuseSrcTex, refractUV * 560.0).r," +
                        "           getTexD(diffuseSrcTex, refractUV * 570.0).g," +
                        "           getTexD(diffuseSrcTex, refractUV * 580.0).b" +
                        "       );\n" +
                        // todo mix in linear space
                        "       result = vec4(diffuse * tint * (1.0-tr) +\n" +
                        "           tint * tr +\n" +
                        "           emissiveData.rgb * normFactor, 1.0);\n" +
                        "   }\n" +
                        "}\n"
            )
        }
    }

    override fun blendTransparentStage(pipeline: Pipeline, stage: PipelineStageImpl) {

        // todo baked ambient occlusion is somehow put into depth buffer, when we split FBs

        val old = GFXState.currentBuffer
        val tmp = getFB(listOf(TargetType.Float16x4, TargetType.Float16x4, TargetType.Float16x2))
        useFrame(old.width, old.height, true, tmp, GlassRenderer) {
            tmp.clearColor(0)
            val depthMode = if (GFX.supportsClipControl) DepthMode.CLOSE
            else DepthMode.FORWARD_CLOSE
            GFXState.depthMode.use(depthMode) {
                GFXState.depthMask.use(false) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {
                        stage.draw(pipeline)
                    }
                }
            }
        }

        // because we use fraction, we no longer copy 1:1, so we need a backup
        val copy = FBStack["glass-copy", old.width, old.height, 3, true, old.samples, DepthBufferType.NONE]
        useFrame(copy) {
            GFX.copy(old)
        }

        renderPurely2 {
            val multisampled = tmp.samples > 1
            val shader = applyShader[multisampled]
            shader.use()

            // bind all textures
            copy.getTextureIMS(0).bindTrulyNearest(shader, "diffuseSrcTex")
            tmp.getTextureIMS(0).bindTrulyNearest(shader, "diffuseGlassTex")
            tmp.getTextureIMS(1).bindTrulyNearest(shader, "emissiveGlassTex")
            tmp.getTextureIMS(2).bindTrulyNearest(shader, "normalGlassTex")

            flat01.draw(shader)
        }

        tmp.destroy()
    }
}