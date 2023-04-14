package me.anno.ecs.components.camera.effects

import me.anno.ecs.annotations.Range
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ReverseDepth.rawToDepthVars
import me.anno.gpu.shader.ReverseDepth.rawToDepth
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.maths.Maths.ceilDiv
import kotlin.math.tan

// https://blog.voxagon.se/2018/05/04/bokeh-depth-of-field-in-single-pass.html
class DepthOfFieldEffect : CameraEffect() {

    var focusPoint = 1f
    var focusScale = 0.25f
    var radScale = 0.5f // Smaller = nicer blur, larger = faster
    var maxBlurSize = 20f // in pixels

    @Range(0.0, 1.0)
    var spherical = 0f

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val color = layers[DeferredLayerType.SDR_RESULT]!!.getTexture0()
        val depth = layers[DeferredLayerType.DEPTH]!!.getTexture0()
        val output = render(color, depth)
        write(layers, DeferredLayerType.SDR_RESULT, output)
    }

    override fun listInputs() =
        listOf(DeferredLayerType.HDR_RESULT, DeferredLayerType.DEPTH, DeferredLayerType.POSITION)

    override fun listOutputs() =
        listOf(DeferredLayerType.SDR_RESULT)

    fun bindDepth(shader: Shader) {
        val factor = 2f * spherical
        shader.v3f(
            "fovFactor",
            factor * tan(RenderState.fovXRadians * 0.5f),
            factor * tan(RenderState.fovYRadians * 0.5f),
            RenderState.near
        )
    }

    fun render(color: ITexture2D, depth: ITexture2D): IFramebuffer {
        val coc = FBStack["coc", ceilDiv(color.w, 2), ceilDiv(color.h, 2), 4, false, 1, false]
        useFrame(coc) {
            val shader = cocShader
            shader.use()
            bindDepth(shader)
            shader.v1f("focusPoint", focusPoint)
            shader.v1f("focusScale", focusScale)
            shader.v1f("maxBlurSize", maxBlurSize)
            shader.v1f("radScale", radScale)
            color.bind(shader, "colorTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
            depth.bindTrulyNearest(shader, "depthTex")
            flat01.draw(shader)
        }
        val buffer = FBStack["dof", color.w, color.h, 4, true, 1, false]
        useFrame(buffer) {
            val shader = dofShader
            shader.use()
            bindDepth(shader)
            shader.v1f("focusPoint", focusPoint)
            shader.v1f("focusScale", focusScale)
            shader.v1f("maxBlurSize", maxBlurSize)
            shader.v1f("radScale", radScale)
            shader.v2f("pixelSize", 1f / color.w, 1f / color.h)
            color.bindTrulyNearest(shader, "colorTex")
            depth.bindTrulyNearest(shader, "depthTex")
            coc.getTexture0().bind(shader, "cocTex", GPUFiltering.LINEAR, Clamping.CLAMP)
            flat01.draw(shader)
        }
        return buffer
    }

    override fun clone() = DepthOfFieldEffect()

    companion object {

        val coc = "" +
                "float getBlurSize(float depth, vec2 uv) {\n" +
                "   float len = length(vec3((uv-0.5)*fovFactor.xy, 1.0));\n" +
                "   float coc = (1.0 / focusPoint - 1.0 / (depth * len)) * focusScale;\n" +
                "   return min(abs(coc), 1.0);\n" +
                "}"

        val cocShader = Shader(
            "dof", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "focusScale"),
                Variable(GLSLType.V1F, "focusPoint"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + rawToDepthVars, "" +
                    rawToDepth +
                    coc +
                    "void main() {\n" +
                    "   float depth = rawToDepth(texture(depthTex,uv).r);\n" +
                    "   vec3 color = texture(colorTex,uv).xyz;\n" +
                    "   result = clamp(vec4(color,getBlurSize(depth,uv)),0.0,1.0);\n" +
                    "}\n"
        )

        val dofShader = Shader(
            "dof", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "focusScale"),
                Variable(GLSLType.V1F, "focusPoint"),
                Variable(GLSLType.V1F, "maxBlurSize"),
                Variable(GLSLType.V1F, "radScale"),
                Variable(GLSLType.V2F, "pixelSize"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.S2D, "cocTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + rawToDepthVars, "" +
                    quatRot +
                    rawToDepth +
                    coc +
                    "const float GOLDEN_ANGLE = 2.39996323;\n" +
                    "vec3 dof(vec2 uv, float centerDepth, float centerSize){\n" +
                    "   vec3 color = texture(colorTex, uv).rgb;\n" +
                    "   float tot = 1.0;\n" +
                    "   float radius = radScale;\n" +
                    "   float sizeLimit = centerSize*2.0;\n" +
                    "   float ang = 0.0;\n" +
                    "   float midRadius = min(centerSize, 3.0);\n" +
                    "   for (; radius<midRadius; ang += GOLDEN_ANGLE){\n" +
                    "       vec2 tc = uv + vec2(cos(ang), sin(ang)) * pixelSize * radius;\n" +
                    "       vec3 sampleColor = texture(colorTex, tc).rgb;\n" +
                    "       float sampleDepth = rawToDepth(texture(depthTex,tc).r);\n" +
                    "       float sampleSize = getBlurSize(sampleDepth,uv) * maxBlurSize;\n" +
                    "       if (sampleDepth > centerDepth) sampleSize = min(sampleSize, sizeLimit);\n" +
                    "       float m = smoothstep(radius-0.5, radius+0.5, sampleSize);\n" +
                    "       color += mix(color/tot, sampleColor, m);\n" +
                    "       tot += 1.0;\n" +
                    "       radius += radScale/radius;\n" +
                    "   }\n" +
                    "   for (; radius<centerSize; ang += GOLDEN_ANGLE){\n" +
                    "       vec2 tc = uv + vec2(cos(ang), sin(ang)) * pixelSize * radius;\n" +
                    "       vec4 sampleColor = texture(cocTex, tc);\n" +
                    "       float sampleDepth = rawToDepth(texture(depthTex,tc).r);\n" +
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
                    "       result = texture(colorTex,uv);\n" +
                    "   } else {\n" +
                    "       result = vec4(dof(uv,centerDepth,centerSize),1.0);\n" +
                    "   }\n" +
                    "}\n"
        ).apply {
            setTextureIndices("cocTex", "depthTex", "colorTex")
        }
    }
}