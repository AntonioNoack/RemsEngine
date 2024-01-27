package me.anno.gpu.pipeline.transparency

import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib
import me.anno.engine.ui.render.RendererLib.fresnelSchlick
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely2
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
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

    // todo refractions (of non-transparent objects) :3
    //  this would need a copy of the two buffers we write to
    // theoretically needs a search, again...
    // depends on normal

    companion object {

        val GlassRenderer = object : Renderer(
            "glass", DeferredSettings(
                listOf(
                    DeferredLayerType.COLOR,
                    DeferredLayerType.EMISSIVE
                )
            )
        ) {
            override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
                val vars = pbrRenderer.getPixelPostProcessing(flags).first().variables.filter { !it.isOutput }
                return listOf(
                    ShaderStage( // todo glass needs to be shadowed @sunlight
                        "glass",
                        vars + listOf(Variable(GLSLType.V1F, "IOR")), "" +
                                colorToLinear +
                                RendererLib.lightCode + // calculates the light onto this surface, stores diffuseLight and specularLight
                                RendererLib.combineLightCode +
                                RendererLib.skyMapCode +
                                colorToSRGB +
                                "float milkiness = 0.5 * finalRoughness;\n" +
                                "float fresnel = fresnelSchlick(abs(dot(finalNormal,normalize(finalPosition))), gl_FrontFacing ? 1.0 / IOR : IOR);\n" +
                                "finalAlpha = mix(fresnel, 1.0, milkiness);\n" +
                                "finalEmissive = finalColor * finalAlpha;\n" + // reflections
                                "finalColor = -log(finalColor0) * finalAlpha;\n" // diffuse tinting
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
                    Variable(sampleType, "surfaceGlassTex"),
                    Variable(GLSLType.V3F, "refX"),
                    Variable(GLSLType.V3F, "refY"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT),
                ), "" +
                        (if (multisampled) "" +
                                "#define getTex(s) texelFetch(s,uvi,gl_SampleID)\n" else
                            "#define getTex(s) texture(s,uv)\n") +
                        "void main() {\n" +
                        (if (multisampled) "" +
                                "ivec2 uvi = ivec2(uv*textureSize(diffuseGlassTex));\n" else "") +
                        "   vec4 diffuseData = getTex(diffuseGlassTex);\n" +
                        "   vec4 emissiveData = getTex(emissiveGlassTex);\n" +
                        "   float tr = clamp(diffuseData.a,0.0,1.0);\n" +
                        "   vec3 tint = exp(-diffuseData.rgb);\n" +
                        "   vec4 diffuse = getTex(diffuseSrcTex);\n" +
                        "   vec4 surface = getTex(surfaceGlassTex);\n" +
                        "   result = vec4(diffuse.rgb * tint * (1.0-tr) +\n" +
                        "       tint * tr +\n" +
                        "       emissiveData.rgb / (diffuseData.a + 0.01), 1.0);\n" +
                        "}\n"
            )
        }
    }

    override fun blendTransparentStage(pipeline: Pipeline, stage: PipelineStageImpl) {

        val old = GFXState.currentBuffer
        val tmp = getFB(arrayOf(TargetType.Float16x4, TargetType.Float16x3))
        useFrame(old.width, old.height, true, tmp, GlassRenderer) {
            tmp.clearColor(0)
            GFXState.depthMode.use(DepthMode.CLOSE) {
                GFXState.depthMask.use(false) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {
                        drawTransparentStage(pipeline, stage)
                    }
                }
            }
        }

        renderPurely2 {
            val multisampled = tmp.samples > 1
            val shader = applyShader[multisampled]
            shader.use()

            // bind all textures
            old.getTextureIMS(0).bindTrulyNearest(shader, "diffuseSrcTex")
            tmp.getTextureIMS(0).bindTrulyNearest(shader, "diffuseGlassTex")
            tmp.getTextureIMS(1).bindTrulyNearest(shader, "emissiveGlassTex")

            flat01.draw(shader)
        }

        tmp.destroy()
    }
}