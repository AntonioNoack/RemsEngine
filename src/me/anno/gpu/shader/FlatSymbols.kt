package me.anno.gpu.shader

import me.anno.gpu.shader.ShaderLib.uiVertexShader
import me.anno.gpu.shader.ShaderLib.uiVertexShaderList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.TAUf

object FlatSymbols {

    val flatShaderHalfArrow = BaseShader(
        "flatShaderHalfArrow",
        uiVertexShaderList, uiVertexShader, ShaderLib.uvList, listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V4F, "backgroundColor"),
            Variable(GLSLType.V1F, "smoothness"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ), "" +
                "void main(){\n" +
                "   float delta = smoothness * mix(dFdx(uv.x), -dFdy(uv.y), 0.667);\n" +
                "   float p0 = -0.5 + (uv.x - uv.y);\n" +
                "   float p1 = -1.5 + (uv.x + uv.y);\n" +
                "   float mn = max(p0, p1);\n" +
                "   float fc = mn*dx+0.5;\n" +
                "   if(fc >= 1.0) discard;\n" +
                "   vec4 fragColor = mix(color, backgroundColor, clamp(fc, 0.0, 1.0));\n" +
                "   finalColor = fragColor.rgb;\n" +
                "   finalAlpha = fragColor.a;\n" +
                "}"
    )

    val circleAngleLogic = """
                float a0 = angleLimits.x;
                float a1 = angleLimits.y;
                
                float x = uv2.x;
                float y = uv2.y;
                
                float angle = atan(-y, x);
                float sweep = mod(angleLimits.y - angleLimits.x + $TAUf, $TAUf);
                float relativeAngle = mod(angle - angleLimits.x + $TAUf, $TAUf);
                
                // Estimate angular derivatives (atan is discontinuous).
                vec2 dx = dFdx(uv2);
                vec2 dy = dFdy(uv2);
                float da = dot(uv2,uv2) / max(
                    abs(uv2.y * dx.x - uv2.x * dx.y) +
                    abs(uv2.y * dy.x - uv2.x * dy.y),
                    1e-18
                );
                
                 // force symmetric anti-aliasing at start-angle
                if ((relativeAngle - $TAUf) * da > -0.5) relativeAngle -= $TAUf;
                
                float alpha0 = relativeAngle;
                float alpha1 = sweep - relativeAngle;
                float angleAlpha = clamp(min(alpha0, alpha1) * da + 0.5, 0.0, 1.0);
                """.trimIndent()

    val flatShaderCircle = BaseShader(
        "flatShaderCircle",
        uiVertexShaderList, uiVertexShader, ShaderLib.uvList, listOf(
            Variable(GLSLType.V4F, "innerColor"),
            Variable(GLSLType.V4F, "circleColor"),
            Variable(GLSLType.V4F, "backgroundColor"),
            Variable(GLSLType.V1F, "innerRadius"),
            Variable(GLSLType.V1F, "outerRadius"),
            Variable(GLSLType.V1F, "smoothness"),
            Variable(GLSLType.V2F, "angleLimits"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ), "" +
                "void main(){\n" +
                "   vec2 uv2 = uv*2.0-1.0;\n" +
                "   float radius = length(uv2), safeRadius = max(radius, 1e-16);\n" +
                "   float dr = 1.0 / fwidth(dot(uv2,uv2)-1.0);\n" +
                "   vec4 fragColor;\n" +
                "   if(angleLimits.x != angleLimits.y) {\n" +

                circleAngleLogic +

                "       vec4 baseColor = mix(innerColor, circleColor, clamp((radius-innerRadius) * dr + 0.5, 0.0, 1.0));\n" +
                "       fragColor = mix(backgroundColor, baseColor, (clamp((outerRadius-radius) * dr + 0.5, 0.0, 1.0)) * angleAlpha);\n" +
                "   } else {\n" +
                "       vec4 baseColor = mix(innerColor, circleColor, clamp((radius-innerRadius) * dr + 0.5, 0.0, 1.0));\n" +
                "       fragColor = mix(baseColor, backgroundColor, clamp((radius-outerRadius) * dr + 0.5, 0.0, 1.0));\n" +
                "   }\n" +
                "   finalColor = fragColor.rgb;\n" +
                "   finalAlpha = fragColor.a;\n" +
                "}"
    )
}