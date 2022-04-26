package me.anno.gpu.shader

import me.anno.gpu.shader.ShaderLib.attr0List
import me.anno.gpu.shader.ShaderLib.attr0VShader
import me.anno.gpu.shader.builder.Variable

object FlatSymbols {

    val flatShaderHalfArrow = BaseShader(
        "flatShaderTexture",
        attr0List, attr0VShader, ShaderLib.uvList, listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V4F, "backgroundColor"),
            Variable(GLSLType.V1F, "smoothness")
        ), "" +
                "void main(){\n" +
                "   float delta = smoothness * mix(dFdx(uv.x), -dFdy(uv.y), 0.667);\n" +
                "   float p0 = -0.5 + (uv.x - uv.y);\n" +
                "   float p1 = -1.5 + (uv.x + uv.y);\n" +
                "   float mn = max(p0, p1);\n" +
                "   float fc = mn/delta+0.5;\n" +
                "   if(fc >= 1.0) discard;\n" +
                "   gl_FragColor = mix(color, backgroundColor, clamp(fc, 0.0, 1.0));\n" +
                "}"
    )

    val flatShaderCircle = BaseShader(
        "flatShaderTexture",
        attr0List, attr0VShader, ShaderLib.uvList, listOf(
            Variable(GLSLType.V4F, "innerColor"),
            Variable(GLSLType.V4F, "circleColor"),
            Variable(GLSLType.V4F, "backgroundColor"),
            Variable(GLSLType.V1F, "innerRadius"),
            Variable(GLSLType.V1F, "outerRadius"),
            Variable(GLSLType.V1F, "smoothness"),
            Variable(GLSLType.V2F, "degrees")
        ), "" +
                "void main(){\n" +
                "   vec2 uv2 = uv*2.0-1.0;\n" +
                "   float radius = length(uv2), safeRadius = max(radius, 1e-16);\n" +
                "   float delta = smoothness * 2.0 * mix(dFdx(uv.x), -dFdy(uv.y), clamp(abs(uv2.y)/safeRadius, 0.0, 1.0));\n" +
                "   if(degrees.x != degrees.y){\n" +
                "       float angle = atan(uv2.y, uv2.x);\n" +
                // todo this angle logic is not good enough... somehow employ the same logic as in 3d
                "       float alpha0 = clamp((angle - degrees.x)/delta + .5, 0.0, 1.0);\n" +
                "       float alpha1 = clamp((degrees.y - angle)/delta + .5, 0.0, 1.0);\n" +
                "       vec4 baseColor = mix(innerColor, circleColor, clamp((radius-innerRadius)/delta+0.5, 0.0, 1.0));\n" +
                "       gl_FragColor = mix(backgroundColor, baseColor, (1.0 - clamp((radius-outerRadius)/delta+0.5, 0.0, 1.0)) * max(alpha0, alpha1));\n" +
                "   } else {\n" +
                "       vec4 baseColor = mix(innerColor, circleColor, clamp((radius-innerRadius)/delta+0.5, 0.0, 1.0));\n" +
                "       gl_FragColor = mix(baseColor, backgroundColor, clamp((radius-outerRadius)/delta+0.5, 0.0, 1.0));\n" +
                "   }" +
                "}"
    )

    init {
        flatShaderHalfArrow.ignoreUniformWarnings(ShaderLib.blacklist)
        flatShaderCircle.ignoreUniformWarnings(ShaderLib.blacklist)
    }

}