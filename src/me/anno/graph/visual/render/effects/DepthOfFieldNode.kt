package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.visual.render.Texture
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import kotlin.math.tan

/**
 * Depth of Field effect from https://blog.voxagon.se/2018/05/04/bokeh-depth-of-field-in-single-pass.html,
 * can become very GPU hungry, but also looks very nice for cutscenes.
 * */
class DepthOfFieldNode : TimedRenderingNode(
    "Depth of Field",
    listOf(
        "Float", "Focus Point",
        "Float", "Focus Scale",
        "Float", "Rad Scale", // Smaller = nicer blur, larger = faster
        "Float", "Max Blur Size", // in pixels
        "Float", "Spherical",
        "Bool", "Apply Tone Mapping",
        "Texture", "Illuminated",
        "Texture", "Depth"
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 1f)
        setInput(2, 0.25f)
        setInput(3, 0.5f)
        setInput(4, 20f)
        setInput(5, 0f)
        setInput(6, false)
    }

    override fun executeAction() {

        val focusPoint = getFloatInput(1)
        val focusScale = getFloatInput(2)
        val radScale = getFloatInput(3)
        val maxBlurSize = getFloatInput(4)
        val spherical = getFloatInput(5)

        val applyToneMapping = getBoolInput(6)
        val color = getTextureInput(7) ?: return
        val depth = getTextureInput(8) ?: return

        timeRendering(name, timer) {
            val result = render(
                color, depth, spherical, focusPoint, focusScale,
                clamp(maxBlurSize, 1f, 20f),
                clamp(radScale, 0.25f, 2f), applyToneMapping
            ).getTexture0()
            setOutput(1, Texture(result))
        }
    }

    companion object {

        fun bindDepth(shader: Shader, spherical: Float) {
            DepthTransforms.bindDepthUniforms(shader)
            val factor = 2f * spherical
            shader.v2f(
                "fovFactorUV",
                factor * tan(RenderState.fovXRadians * 0.5f),
                factor * tan(RenderState.fovYRadians * 0.5f),
            )
        }

        fun render(
            color: ITexture2D, depth: ITexture2D, spherical: Float,
            focusPoint: Float, focusScale: Float, maxBlurSize: Float, radScale: Float,
            applyToneMapping: Boolean,
        ): IFramebuffer {
            val w = Maths.ceilDiv(color.width, 2)
            val h = Maths.ceilDiv(color.height, 2)
            val coc = FBStack["coc", w, h, 4, false, 1, DepthBufferType.NONE]
            GFXState.useFrame(coc) {
                val shader = cocShader
                shader.use()
                bindDepth(shader, spherical)
                shader.v1f("focusPointInv", 1f / focusPoint)
                shader.v1f("focusScale", focusScale)
                color.bind(shader, "colorTex", Filtering.TRULY_LINEAR, Clamping.CLAMP)
                depth.bindTrulyNearest(shader, "depthTex")
                SimpleBuffer.flat01.draw(shader)
            }
            val buffer = FBStack["dof", color.width, color.height, 4, true, 1, DepthBufferType.NONE]
            GFXState.useFrame(buffer) {
                val shader = dofShader
                shader.use()
                bindDepth(shader, spherical)
                shader.v1f("focusPointInv", 1f / focusPoint)
                shader.v1f("focusScale", focusScale)
                shader.v1f("maxBlurSize", maxBlurSize)
                shader.v1f("radScale", radScale)
                shader.v1b("applyToneMapping", applyToneMapping)
                shader.v2f("pixelSize", 1f / color.width, 1f / color.height)
                coc.getTexture0().bind(shader, "cocTex", Filtering.LINEAR, Clamping.CLAMP)
                color.bindTrulyNearest(shader, "colorTex")
                depth.bindTrulyNearest(shader, "depthTex")
                SimpleBuffer.flat01.draw(shader)
            }
            return buffer
        }

        const val getBlurSize = "" +
                "float getBlurSize(float depth, vec2 uv) {\n" +
                "   float len = length(vec3((uv-d_uvCenter)*fovFactorUV.xy, 1.0));\n" +
                "   float coc = (focusPointInv - 1.0 / (depth * len)) * focusScale;\n" +
                "   return min(abs(coc), 1.0);\n" +
                "}"

        val cocShader = Shader(
            "coc", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "focusScale"),
                Variable(GLSLType.V1F, "focusPointInv"),
                Variable(GLSLType.V2F, "fovFactorUV"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + DepthTransforms.depthVars, "" +
                    DepthTransforms.rawToDepth +
                    getBlurSize +
                    "void main() {\n" +
                    "   float depth = rawToDepth(textureLod(depthTex,uv,0.0).r);\n" +
                    "   vec3 color = textureLod(colorTex,uv,0.0).xyz;\n" +
                    "   result = clamp(vec4(color,getBlurSize(depth,uv)),0.0,1.0);\n" +
                    "}\n"
        )

        val dofShader = Shader(
            "dof", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "focusScale"),
                Variable(GLSLType.V1F, "focusPointInv"),
                Variable(GLSLType.V2F, "fovFactorUV"),
                Variable(GLSLType.V1F, "maxBlurSize"),
                Variable(GLSLType.V1F, "radScale"),
                Variable(GLSLType.V1B, "applyToneMapping"),
                Variable(GLSLType.V2F, "pixelSize"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.S2D, "cocTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + DepthTransforms.depthVars, "" +
                    ShaderLib.quatRot +
                    DepthTransforms.rawToDepth +
                    getBlurSize +
                    Renderers.tonemapGLSL +
                    "const float GOLDEN_ANGLE = 2.39996323;\n" +
                    "vec3 dof(vec2 uv, float centerDepth, float centerSize){\n" +
                    "   vec3 color = textureLod(colorTex,uv,0.0).rgb;\n" +
                    "   float tot = 1.0;\n" +
                    "   float radius = radScale;\n" +
                    "   float sizeLimit = centerSize*2.0;\n" +
                    "   float ang = 0.0;\n" +
                    "   float midRadius = min(centerSize, 3.0);\n" +
                    "   // [loop]\n" + // hlsl instruction
                    "   for (; radius<midRadius; ang += GOLDEN_ANGLE){\n" +
                    "       vec2 tc = uv + vec2(cos(ang), sin(ang)) * pixelSize * radius;\n" +
                    "       vec3 sampleColor = textureLod(colorTex,tc,0.0).rgb;\n" +
                    "       float sampleDepth = rawToDepth(textureLod(depthTex,tc,0.0).r);\n" +
                    "       float sampleSize = getBlurSize(sampleDepth,uv) * maxBlurSize;\n" +
                    "       if (sampleDepth > centerDepth) sampleSize = min(sampleSize, sizeLimit);\n" +
                    "       float m = smoothstep(radius-0.5, radius+0.5, sampleSize);\n" +
                    "       color += mix(color/tot, sampleColor, m);\n" +
                    "       tot += 1.0;\n" +
                    "       radius += radScale/radius;\n" +
                    "   }\n" +
                    "   // [loop]\n" + // hlsl instruction
                    "   for (; radius<centerSize; ang += GOLDEN_ANGLE){\n" +
                    "       vec2 tc = uv + vec2(cos(ang), sin(ang)) * pixelSize * radius;\n" +
                    "       vec4 sampleColor = textureLod(cocTex,tc,0.0);\n" +
                    "       float sampleDepth = rawToDepth(textureLod(depthTex,tc,0.0).r);\n" +
                    "       float sampleSize = sampleColor.a * maxBlurSize;\n" +
                    "       if (sampleDepth > centerDepth) sampleSize = min(sampleSize, sizeLimit);\n" +
                    "       float m = smoothstep(radius-0.5, radius+0.5, sampleSize);\n" +
                    "       color += mix(color/tot, sampleColor.rgb, m);\n" +
                    "       tot += 1.0;\n" +
                    "       radius += radScale/radius;\n" +
                    "   }\n" +
                    "   return color/tot;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   float centerDepth = rawToDepth(texture(depthTex,uv).r);\n" +
                    "   float centerSize = getBlurSize(centerDepth,uv) * maxBlurSize;\n" +
                    "   if(centerSize < 0.5) {\n" +
                    "       result = textureLod(colorTex,uv,0.0);\n" +
                    "   } else {\n" +
                    "       result = vec4(dof(uv,centerDepth,centerSize),1.0);\n" +
                    "   }\n" +
                    "   if(applyToneMapping) result = tonemap(result);\n" +
                    "}\n"
        )
    }
}