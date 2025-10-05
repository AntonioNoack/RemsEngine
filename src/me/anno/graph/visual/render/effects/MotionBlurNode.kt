package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.visual.render.Texture

/**
 * Adds motion blur effect (many players dislike this, so make it configurable!).
 *
 * Add a MotionBlurSettings instance to your scene to configure this.
 * */
class MotionBlurNode : TimedRenderingNode(
    "Motion Blur",
    listOf(
        "Texture", "Illuminated",
        "Texture", "Motion",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    override fun executeAction() {

        val settings = GlobalSettings[MotionBlurSettings::class]

        val color = getTextureInput(1, missingTexture)
        val motion = getTextureInput(2, blackTexture)
        val depth = getTextureInput(3, depthTexture)
        val depthMask = getTextureInputMask(3)

        timeRendering(name, timer) {
            val framebuffer = FBStack[
                name, color.width, color.height, if (color.isHDR) TargetType.Float16x3 else TargetType.UInt8x3,
                1, DepthBufferType.NONE
            ]
            framebuffer.isSRGBMask = 1
            useFrame(color.width, color.height, true, framebuffer, copyRenderer) {
                val shader = shader
                shader.use()

                DepthTransforms.bindDepthUniforms(shader)
                shader.v1i("maxSamples", settings.maxSamples)
                shader.v1f("shutter", 0.5f * settings.shutter)
                shader.v4f("depthMask", depthMask)

                color.bindTrulyNearest(shader, "colorTex")
                motion.bindTrulyNearest(shader, "motionTex")
                depth.bindTrulyNearest(shader, "depthTex")

                SimpleBuffer.flat01.draw(shader)
                GFX.check()
            }
            setOutput(1, Texture.texture(framebuffer, 0))
        }
    }


    companion object {
        // definitely not ideal; we'd need to smear moving objects instead of smearing on moving objects
        private val shader = Shader(
            "MotionBlur", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1I, "maxSamples"),
                Variable(GLSLType.V1F, "shutter"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + DepthTransforms.depthVars, "" +
                    DepthTransforms.rawToDepth +
                    "float depthIndex(vec2 uv) {\n" +
                    "   float raw = dot(texture(depthTex,uv), depthMask);\n" +
                    "   float depth = rawToDepth(raw);\n" +
                    "   return log2(clamp(depth,1e-38,1e38));\n" +
                    "}\n" +
                    "float depthWeight(float deltaDepth) {\n" +
                    "   float exponent = deltaDepth * deltaDepth * 5.0;\n" +
                    "   return exponent < 5.0 ? exp(-exponent) : 0.0;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   vec2 motion = shutter * texture(motionTex,uv).xy;\n" + // 0.5, because we sample from both sides
                    "   float length1 = length(motion * vec2(textureSize(motionTex,0)));\n" +
                    "   int samplesI = min(maxSamples, int(round(length1)));\n" +
                    "   vec4   color = texture(colorTex, uv);\n" +
                    "   float depth0 = depthIndex(uv);\n" +
                    "   if (samplesI > 1){\n" +
                    "       vec2 uv2 = uv, uv3 = uv;\n" +
                    "       vec2 duv = motion / float(samplesI-1);\n" +
                    "       float weight = 1.0;\n" +
                    "       for(int i=1;i<samplesI;i++){\n" +
                    "           uv2 += duv;\n" +
                    "           uv3 -= duv;\n" +
                    // discard samples with too different depth
                    "           float weight2 = depthWeight(depthIndex(uv2) - depth0);\n" +
                    "           float weight3 = depthWeight(depthIndex(uv3) - depth0);\n" +
                    "           if (weight2 > 0.0) color += weight2 * texture(colorTex, uv2);\n" +
                    "           if (weight3 > 0.0) color += weight3 * texture(colorTex, uv3);\n" +
                    "           weight += weight2 + weight3;\n" +
                    "       }\n" +
                    "       color /= weight;\n" +
                    "   }\n" +
                    "   result = color;\n" +
                    "}\n"
        )
    }
}