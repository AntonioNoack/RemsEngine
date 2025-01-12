package me.anno.gpu.pipeline.transparency

import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.Blitting
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
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.structures.maps.LazyMap

/**
 * order-independent transparency for deferred rendering;
 * issue: glass panes are not tinted by panes before them
 * */
class GlassPass : TransparentPass() {
    companion object {

        /**
         * depth while drawing glass-materials;
         * may be multi-sampled
         * */
        var glassPassDepth: ITexture2D? = null
        // todo value for single-sampled glass depth? lazy maybe?

        val diffuseTinting = DeferredLayerType("diffTint", "finalTinting", 3, 0)
        val opacity = DeferredLayerType("opacity", "finalAlpha", 1, 0)
        val reflections = DeferredLayerType("reflect", "finalReflections", 3, 0)
        val refraction = DeferredLayerType("refract", "finalRefraction", 1, 0)
        val normal2d = DeferredLayerType("normal2d", "finalNormal2D", 2, 0)

        val GlassRenderer = object : Renderer(
            "glass", DeferredSettings(
                listOf(
                    diffuseTinting,
                    DeferredLayerType.ALPHA,
                    reflections,
                    refraction,
                    normal2d,
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
                    ShaderStage(
                        "glass",
                        vars + listOf(
                            Variable(GLSLType.V1F, "IOR"),
                            Variable(GLSLType.V3F, "dirX"),
                            Variable(GLSLType.V3F, "dirY"),
                            Variable(GLSLType.V1F, "finalOcclusion"),
                            Variable(GLSLType.V3F, "finalTinting", VariableMode.OUT),
                            Variable(GLSLType.V3F, "finalReflections", VariableMode.OUT),
                            Variable(GLSLType.V1F, "finalRefraction", VariableMode.OUT),
                            Variable(GLSLType.V2F, "finalNormal2D", VariableMode.OUT)
                        ), "" +
                                colorToLinear +
                                RendererLib.lightCode + // calculates the light onto this surface, stores diffuseLight and specularLight
                                RendererLib.combineLightCode +
                                RendererLib.skyMapCode +
                                colorToSRGB +
                                "float milkiness = 0.5 * finalRoughness;\n" +
                                "float iorValue = gl_FrontFacing ? 1.0 / IOR : IOR;\n" +
                                "float fresnel = 0.04;\n" + // we don't have SSR for glass, so this is the best we can do visually
                                "finalAlpha = mix(fresnel, 1.0, milkiness);\n" +
                                "finalReflections = finalColor * finalAlpha;\n" + // color from sky
                                "finalTinting = -log(finalColor0) * finalAlpha;\n" + // actual color
                                "finalRefraction = IOR == 1.0 ? 0.0 : -log(IOR) * finalAlpha;\n" +
                                "finalNormal2D = IOR == 1.0 ? vec2(0.0) : vec2(dot(finalNormal,dirX), dot(finalNormal,dirY)) * finalAlpha;\n"
                    ).add(getReflectivity).add(sampleSkyboxForAmbient).add(brightness)
                )
            }
        }

        // override diffuseColor and finalEmissive in shader
        val applyShader = LazyMap { multisampled: Boolean ->
            val sampleType = if (multisampled) GLSLType.S2DMS else GLSLType.S2D
            Shader(
                "applyGlass", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
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
                        "       float normFactor = 0.5 / (diffuseData.a + 0.01);\n" +
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

    override fun blendTransparentStage(pipeline: Pipeline, stage: PipelineStageImpl, colorInput: ITexture2D) {

        // todo baked ambient occlusion is somehow put into depth buffer, when we split FBs

        val old = GFXState.currentBuffer
        val tmp = getFB(listOf(TargetType.Float16x4, TargetType.Float16x4, TargetType.Float16x2))
        glassPassDepth = old.depthTexture
        useFrame(old.width, old.height, true, tmp, GlassRenderer) {
            tmp.clearColor(0)
            val depthMode = if (GFX.supportsClipControl) DepthMode.CLOSE
            else DepthMode.FORWARD_CLOSE
            GFXState.depthMode.use(depthMode) {
                GFXState.depthMask.use(false) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {
                        GFXState.drawLines.use(Mesh.drawDebugLines) {
                            stage.draw(pipeline)
                        }
                    }
                }
            }
        }
        glassPassDepth = null

        // because we have refraction, we no longer copy 1:1, so we need a backup
        val copy = FBStack["glass-copy", old.width, old.height, 3, true, old.samples, DepthBufferType.NONE]
        useFrame(copy) {
            Blitting.copy(old, false)
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
    }
}