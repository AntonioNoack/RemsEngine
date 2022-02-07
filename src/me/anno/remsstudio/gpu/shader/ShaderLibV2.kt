package me.anno.remsstudio.gpu.shader

import me.anno.gpu.shader.*
import me.anno.gpu.shader.builder.Variable
import me.anno.remsstudio.objects.effects.MaskType

object ShaderLibV2 {

    lateinit var shader3DMasked: BaseShader
    lateinit var shader3DGaussianBlur: BaseShader

    fun init() {

        val v3DMasked = ShaderLib.v3DBase +
                "${OpenGLShader.attribute} vec2 attr0;\n" +
                "void main(){\n" +
                "   finalPosition = vec3(attr0*2.0-1.0, 0.0);\n" +
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                "   uv = gl_Position.xyw;\n" +
                ShaderLib.positionPostProcessing +
                "}"

        val y3DMasked = listOf(
            Variable(GLSLType.V3F, "uv"),
            Variable(GLSLType.V3F, "finalPosition"),
            Variable(GLSLType.V1F, "zDistance")
        )

        val f3DMasked = "" +
                "precision highp float;\n" +
                "uniform sampler2D maskTex, tex, tex2;\n" +
                "uniform float useMaskColor;\n" +
                "uniform float invertMask;\n" +
                "uniform vec2 pixelating;\n" +
                "uniform vec2 windowSize, offset;\n" +
                "uniform int maskType;\n" +
                "uniform float maxSteps;\n" +
                "uniform vec4 settings;\n" +
                ShaderLib.brightness +
                ShaderLib.getColorForceFieldLib +
                ShaderLib.rgb2uv +
                "float maxV3(vec3 rgb){return max(rgb.r, max(rgb.g, rgb.b));}\n" +
                "float minV3(vec3 rgb){return min(rgb.r, min(rgb.g, rgb.b));}\n" +
                "void main(){\n" +
                "   vec2 uv1 = uv.xy/uv.z;\n" +
                "   vec2 uv2 = uv1 * 0.5 + 0.5, uv3, uv4;\n" +
                "   vec4 mask = texture(maskTex, uv2);\n" +
                "   vec4 color;\n" +
                "   float effect, inverseEffect;\n" +
                "   switch(maskType){\n" +
                GLSLLib.case(MaskType.MASKING.id, "shader/mask-effects/Masking.glsl") +
                GLSLLib.case(MaskType.TRANSITION.id, "shader/mask-effects/Transition.glsl") +
                GLSLLib.case(MaskType.QUAD_PIXELATION.id, "shader/mask-effects/QuadPixelating.glsl") +
                GLSLLib.case(MaskType.TRI_PIXELATION.id, "shader/mask-effects/TriPixelating.glsl") +
                GLSLLib.case(MaskType.HEX_PIXELATION.id, "shader/mask-effects/HexPixelating.glsl") +
                GLSLLib.case(MaskType.VORONOI_PIXELATION.id, "shader/mask-effects/VoronoiPixelating.glsl") +
                GLSLLib.case(MaskType.RADIAL_BLUR_1.id, "shader/mask-effects/RadialBlur1.glsl") +
                GLSLLib.case(MaskType.RADIAL_BLUR_2.id, "shader/mask-effects/RadialBlur2.glsl") +
                GLSLLib.case(MaskType.GREEN_SCREEN.id, "shader/mask-effects/GreenScreen.glsl") +
                "       case ${MaskType.GAUSSIAN_BLUR.id}:\n" +
                "       case ${MaskType.BOKEH_BLUR.id}:\n" +
                "       case ${MaskType.BLOOM.id}:\n" + // just mix two images
                "           effect = mix(mask.a, dot(vec3(0.3), mask.rgb), useMaskColor);\n" +
                "           effect = mix(effect, 1.0 - effect, invertMask);\n" +
                "           color = mix(texture(tex, uv2), texture(tex2, uv2), effect);\n" +
                "           break;\n" +
                "       case ${MaskType.UV_OFFSET.id}:\n" +
                "           vec2 offset = (mask.rg-mask.gb) * pixelating;\n" +
                "           color = texture(tex, uv2 + offset);\n" +
                "           break;\n" +
                "   }\n" +
                "   if(color.a <= 0.001) discard;\n" +
                "   if(${ShaderLib.hasForceFieldColor}) color *= getForceFieldColor();\n" +
                "   vec3 finalColor = color.rgb;\n" +
                "   float finalAlpha = min(color.a, 1.0);\n" +
                "}"
        shader3DMasked =
            ShaderLib.createShaderPlus("3d-masked", v3DMasked, y3DMasked, f3DMasked, listOf("maskTex", "tex", "tex2"))
        shader3DMasked.ignoreUniformWarnings(listOf("tiling"))

        val f3DGaussianBlur = "" +
                "uniform sampler2D tex;\n" +
                "uniform vec2 stepSize;\n" +
                "uniform float steps;\n" +
                "uniform float threshold;\n" +
                ShaderLib.brightness +
                "void main(){\n" +
                "   vec2 uv2 = uv.xy/uv.z * 0.5 + 0.5;\n" +
                "   vec4 color;\n" +
                "   float sum = 0.0;\n" +
                // test all steps for -pixelating*2 .. pixelating*2, then average
                "   int iSteps = max(0, int(2.7 * steps));\n" +
                "   if(iSteps == 0){\n" +
                "       color = texture(tex, uv2);\n" +
                "   } else {\n" +
                "       color = vec4(0.0);\n" +
                "       for(int i=-iSteps;i<=iSteps;i++){\n" +
                "           float fi = float(i);\n" +
                "           float relativeX = fi/steps;\n" +
                "           vec4 colorHere = texture(tex, uv2 + fi * stepSize);\n" +
                "           float weight = i == 0 ? 1.0 : exp(-relativeX*relativeX);\n" +
                "           sum += weight;\n" +
                "           color += vec4(max(vec3(0.0), colorHere.rgb - threshold), colorHere.a) * weight;\n" +
                "       }\n" +
                "       color /= sum;\n" +
                "   }\n" +
                "   gl_FragColor = color;\n" +
                "}"
        shader3DGaussianBlur =
            ShaderLib.createShader("3d-blur", v3DMasked, y3DMasked, f3DGaussianBlur, listOf("tex"))


    }

}