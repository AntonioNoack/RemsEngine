package me.anno.engine.pbr

import kotlin.math.PI

// from jGLTF
// https://github.com/javagl/JglTF/blob/bb081a2e31b522bacb051d22053768d78c363814/jgltf-model/src/main/resources/pbr.frag
object PBRLibraryGLTF {

    // todo regular plastic has specular as well... why not in this model?

    // we probably have specular dots on thin lines, because we don't handle it -> fix it with some magic-maths integration
    // (strong curvature on metal against the sun will show this problem)
    // to do: good idea, but unfortunately in forward mode, finalNormal won't wary as much on 1px thin lines, because it may consist of many triangles -> true delta unknown to the shader
    val angularCorrection = "" +
            "vec3 curvature = max(abs(dFdx(finalNormal)), abs(dFdy(finalNormal)));\n" +
            "float roughness = 1.0 - (1.0 - finalRoughness) * max(0.0, 1.0 - length(curvature));\n" +
            "roughness = max(roughness, 0.01);\n"

    val maxDivisor = 1e-5 // preventing divisions by zero

    // we can extract common factors (which always appear, and never change for a pixel)
    // alpha, Dx, rp1, k, 1-k
    val specularBRDFv2NoDivInlined2Start = "" +
            angularCorrection +
            "float alpha = roughness * roughness;\n" +
            "float Dx = alpha * alpha, DxM1 = Dx - 1.0;\n" +
            "float rp1 = roughness + 1.0;\n" +
            "float k = clamp(rp1 * rp1, 1.0, 4.0) * 0.125, invK = 1.0-k;\n" +
            "vec3 invSpecularColor = 1.0 - specularColor;\n" +
            "float DxPi4 = Dx * ${0.25 / PI};\n"

    // factor extracted from the BRDF
    // also ambient must be added at the end, or we would need to divide by DxPi4 at the start
    val specularBRDFv2NoDivInlined2End = "specularLight *= specularColor;\n"

    // (specularColor, finalRoughness, V, finalNormal, NdotL, NdotV, H)
    val specularBRDFv2NoDivInlined2 = "" +
            // Compute the microfacet distribution (D)
            "#ifndef DOTS\n" +
            "   float NdotH = clamp(dot(finalNormal, H), 0.0, 1.0);\n" + // clamp is probably unnecessary; could be inserted back
            "#endif\n" +
            "float NdotH_squared = NdotH * NdotH;\n" +
            "float x = NdotH_squared * DxM1 + 1.0;\n" +
            // "    float Dx = alpha_squared;\n" +
            // Compute the specularly reflected color (F)
            // "    float HdotV = clamp(dot(H, V), 0.0, 1.0);\n" + // clamp is probably unnecessary ^^
            "#ifndef DOTS\n" +
            "   float HdotV = clamp(dot(H, V), 0.0, 1.0);\n" +
            "#endif\n" +
            "float invHov = 1.0 - HdotV, invHov2 = invHov * invHov, invHov5 = invHov * invHov2 * invHov2;\n" +
            // is x*(x*x)*(x*x) faster than pow5? yes it is, by 2.8x
            // "    vec3  F = specularInputColor + (1.0 - specularInputColor) * pow(1.0 - HdotV, 5.0);\n" +
            "vec3 F = specularColor + invSpecularColor * invHov5;\n" +
            "vec2 t = vec2(NdotL,NdotV) * invK + k;\n" +
            //"    float Gx = NdotL;\n" +
            // NdotL is already in the light equation, NdotV is in G
            // also we don't need two divisions, we can use one
            "vec3 computeSpecularBRDF = (DxPi4 / max(x * x * t.x * t.y, $maxDivisor)) * F;\n"

    // the following functions can be used, if the color isn't yet available
    // (or you want to use one texture access less)
    // they have missing color, when H || V, so when you look perpendicular onto the surface
    val specularBRDFv2NoColorStart = "" +
            angularCorrection +
            "float alpha = roughness * roughness;\n" +
            "float Dx = alpha * alpha, DxM1 = Dx - 1.0;\n" +
            "float rp1 = roughness + 1.0;\n" +
            "float k = rp1 * rp1 * 0.125, invK = 1.0-k;\n" +
            "float DxPi4 = Dx * ${0.25 / PI};\n"

    // (finalRoughness, finalNormal, NdotL, NdotV, H)
    val specularBRDFv2NoColor = "" +
            // Compute the microfacet distribution (D)
            "float NdotH = clamp(dot(finalNormal, H), 0.0, 1.0);\n" + // clamp is probably unnecessary; could be inserted back
            "float NdotH_squared = NdotH * NdotH;\n" +
            "float x = NdotH_squared * DxM1 + 1.0;\n" +
            // "    float Dx = alpha_squared;\n" +
            // skipped: computing the specularly reflected color (F)
            // "    vec3  F = specularInputColor + (1.0 - specularInputColor) * pow(1.0 - HdotV, 5.0);\n" +
            // Compute the geometric specular attenuation (G)
            // "    float NdotL = dot(N, L);\n" + // guaranteed to be > 0; already defined
            // "    float NdotV = abs(dot(N, V));\n" + // back face gets same shading (if < 0); clamping is needed here
            "vec2 t = vec2(NdotL,NdotV) * invK + k;\n" +
            //"    float Gx = NdotL;\n" +
            // NdotL is already in the light equation, NdotV is in G
            // also we don't need two divisions, we can use one
            // to do 1. where is our bug, that we need to limit the value?
            // to do 2. why can we set the limit so high? how is it processed further?
            "#define computeSpecularBRDF DxPi4 / max(x * x * t.x * t.y, $maxDivisor)\n"

    val specularBRDFv2NoColorEnd = "specularLight *= finalMetallic;\n" // * specularColor, just without color

}