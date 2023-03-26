package me.anno.gpu.pipeline.transparency

import me.anno.engine.ui.render.RendererLib.combineLightCode
import me.anno.engine.ui.render.RendererLib.lightCode
import me.anno.engine.ui.render.RendererLib.skyMapCode
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
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
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.input.Input
import me.anno.language.translation.NameDesc
import me.anno.utils.Color.black
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.structures.tuples.IntPair
import org.lwjgl.opengl.GL11C.*

/**
 * Weighted Blended Order-Independent Transparency by Morgan McGuire and Louis Bavoil
 * */
class WeightedBlended : TransparentPass() {

    companion object {
        private const val name0 = "weightedBlend"
        private const val name1 = "weightedBlend3"

        val blend0 = BlendMode(NameDesc(name0), name0)
            .set(GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)

        val blend2 = BlendMode(NameDesc(name1), name1)
            .set(GL_ONE, GL_ONE, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)

        private const val mixing = "" +
                // good mixing formula?
                "   vec3 tint = clamp(rgb,vec3(0),vec3(1)) * (1.0-alpha);\n" +
                "   vec4 s0 = texture(diffuseSrcTex, uv);\n" +
                "   vec4 s1 = texture(emissiveSrcTex, uv);\n" +
                "   float mx = clamp(alpha * 5.0, 0.0, 1.0);\n" +
                "   diffuse  = vec4(mix(s0.rgb, s0.rgb * tint,       mx),s0.a);\n" +
                "   emissive = vec4(mix(s1.rgb, s1.rgb * tint + rgb, mx),s1.a);\n"

        private const val blendWeight = "" +
                // turn invZ into value for blending
                "float invZ = length(finalPosition);\n" + // gl_FragCoord.z seems very unstable... not initialized???
                "float weight = finalAlpha * clamp(invZ, 1e-3, 1e3);\n"

        private val l0 = DeferredLayerType("result0", "result0", 4, 0)
        private val l1 = DeferredLayerType("result1", "result1", 4, 0)
        private val l01 = DeferredSettingsV2(listOf(l0, l1), 1, false)
        val applyShader = LazyMap<IntPair, Shader> {
            Shader(
                "apply0", coordsList, coordsVShader, uvList, listOf(
                    Variable(GLSLType.V1B, "perTargetBlending"),
                    Variable(GLSLType.S2D, "accuTexture"),
                    Variable(GLSLType.S2D, "revealTexture"),
                    Variable(GLSLType.S2D, "diffuseSrcTex"),
                    Variable(GLSLType.S2D, "emissiveSrcTex"),
                    Variable(GLSLType.V4F, "diffuse", VariableMode.OUT).apply { ignored = true },
                    Variable(GLSLType.V4F, "emissive", VariableMode.OUT).apply { ignored = true },
                ), "" +
                        "layout(location=${it.first}) out vec4 diffuse;\n" +
                        "layout(location=${it.second}) out vec4 emissive;\n" +
                        "void main() {\n" +
                        "   ivec2 uvi = ivec2(gl_FragCoord.xy);\n" +
                        "   vec4  data0 = texelFetch(accuTexture, uvi, 0);\n" +
                        "   float data1 = texelFetch(revealTexture, uvi, 0).r;\n" +
                        "   float alpha; vec3 rgb;\n" +
                        "   if (perTargetBlending) {\n" +
                        "       alpha = 1.0 - data1;\n" +
                        "       rgb = data0.rgb / clamp(data0.a, 1e-4, 5e4);\n" +
                        "   } else {\n" +
                        "       alpha = 1.0 - data0.a;\n" +
                        "       rgb = data0.rgb / clamp(data1, 1e-4, 5e4);\n" +
                        "   }\n" +
                        mixing +
                        "}\n"
            )
        }

        object Renderer0 : Renderer(name0, l01) {
            override fun getPostProcessing(): ShaderStage {
                val vars = Renderers.pbrRenderer.getPostProcessing()!!.variables.filter { !it.isOutput } +
                        listOf(
                            Variable(GLSLType.V4F, "result0", VariableMode.OUT),
                            Variable(GLSLType.V4F, "result1", VariableMode.OUT)
                        )
                return ShaderStage(
                    vars, "" +
                            lightCode + // calculates the light onto this surface, stores diffuseLight and specularLight
                            combineLightCode +
                            skyMapCode + blendWeight +
                            "finalColor *= finalAlpha;\n" +
                            "result0 = vec4(finalColor, finalAlpha) * weight;\n" + // gl_FragData[0] = vec4(Ci, ai) * w(zi, ai);
                            "result1 = vec4(finalAlpha);\n" // gl_FragData[1] = vec4(ai);
                )
            }
        }

        object Renderer1 : Renderer(name1, l01) {
            override fun getPostProcessing(): ShaderStage {
                val vars = Renderers.pbrRenderer.getPostProcessing()!!.variables.filter { !it.isOutput } +
                        listOf(
                            Variable(GLSLType.V4F, "result0", VariableMode.OUT),
                            Variable(GLSLType.V4F, "result1", VariableMode.OUT)
                        )
                return ShaderStage(
                    vars, "" +
                            lightCode + // calculates the light onto this surface, stores diffuseLight and specularLight
                            combineLightCode +
                            skyMapCode + blendWeight +
                            "result0 = vec4(finalColor * weight, finalAlpha);\n" +  // gl_FragData[0] = vec4(Ci * w(zi, ai), ai);
                            "result1 = vec4(finalAlpha * weight,0,0,1);\n" // gl_FragData[1].r = ai * w(zi, ai);
                )
            }
        }

        private val clear0 = intArrayOf(0, -1)
        private val targets = arrayOf(TargetType.FP16Target4, TargetType.FP16Target1)
        private val blend0s = arrayOf(BlendMode.PURE_ADD, blend0)
    }

    override fun draw1(pipeline: Pipeline) {

        val b0 = GFXState.currentBuffer
        val r0 = GFXState.currentRenderer
        val s0 = r0.deferredSettings!!

        val l0 = s0.findLayer(DeferredLayerType.COLOR)!!
        val l1 = s0.findLayer(DeferredLayerType.EMISSIVE)!!

        val tmp = getFB(targets)
        val perTargetBlending = true // GFX.glVersion >= 40 && !Input.isControlDown // todo with per-target-blending doesn't work :/
        val renderer = if (perTargetBlending) Renderer0 else Renderer1

        useFrame(b0.w, b0.h, true, tmp, renderer) {
            if (perTargetBlending) tmp.clearColor(clear0)
            else tmp.clearColor(black)
            GFXState.depthMode.use(DepthMode.CLOSER) {
                GFXState.depthMask.use(false) {
                    val blend: Any = if (perTargetBlending) blend0s else blend2
                    GFXState.blendMode.use(blend) {
                        draw2(pipeline)
                    }
                }
            }
        }

        combine {
            val shader = applyShader[IntPair(l0.index, l1.index)]
            shader.use()
            shader.v1b("perTargetBlending", perTargetBlending)
            s0.findTexture(b0, l0).bindTrulyNearest(shader, "diffuseSrcTex")
            s0.findTexture(b0, l1).bindTrulyNearest(shader, "emissiveSrcTex")
            tmp.getTextureI(0).bindTrulyNearest(shader, "accuTexture")
            tmp.getTextureI(1).bindTrulyNearest(shader, "revealTexture")
            flat01.draw(shader)
        }
    }

}