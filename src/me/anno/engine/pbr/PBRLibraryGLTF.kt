package me.anno.engine.pbr

import kotlin.math.PI

// from jGLTF
object PBRLibraryGLTF {

    /**
     * References:
     * [1] : Real Shading in Unreal Engine 4 (B. Karis)
     *       http://blog.selfshadow.com/publications/s2013-shading-course/karis/s2013_pbs_epic_notes_v2.pdf
     * [2] : Moving Frostbite to Physically Based Rendering 2.0 (S. Lagarde)
     *       http://www.frostbite.com/wp-content/uploads/2014/11/course_notes_moving_frostbite_to_pbr_v2.pdf
     * [3] : Microfacet Models for Refraction through Rough Surfaces (B. Walter)
     *       http://www.cs.cornell.edu/~srm/publications/EGSR07-btdf.pdf
     * [4] : An Inexpensive BRDF Model for Physically-based Rendering (C. Schlick)
     *       https://www.cs.virginia.edu/~jdl/bib/appearance/analytic models/schlick94b.pdf
     * [5] : A Reflectance Model for Computer Graphics (R. Cook)
     *       http://graphics.pixar.com/library/ReflectanceModel/paper.pdf
     * [6] : Crafting a Next-Gen Material Pipeline for The Order: 1886 (D. Neubelt)
     *       http://blog.selfshadow.com/publications/s2013-shading-course/rad/s2013_pbs_rad_notes.pdf
     * */

    /**
     * Computation of the specular distribution of microfacet normals on the
     * surface. This is also referred to as "NDF", the "normal distribution
     * function". It receives the half-vector H, the surface normal N, and the
     * roughness. This implementation is based on the description in [1], which
     * is supposed to be a summary of [3], although I didn't do the maths...
     */
    val microfacetDistribution = "" +
            "float computeMicrofacetDistribution(vec3 H, vec3 N, float roughness){\n" +
            "    \n" +
            "    float alpha = roughness * roughness;\n" +
            "    float alpha_squared = alpha * alpha;\n" +
            "    \n" +
            "    float NdotH = clamp(dot(N, H), 0.0, 1.0);\n" +
            "    float NdotH_squared = NdotH * NdotH;\n" +
            "    \n" +
            "    float x = NdotH_squared * (alpha_squared - 1.0) + 1.0;\n" +
            "    return alpha_squared / (M_PI * x * x);\n" +
            "}"

    /**
     * Computation of the Fresnel specular reflectance, using the approximation
     * described in [4]. It receives the specular color, the
     * direction from the surface point to the viewer V, and the half-vector H.
     * */
    val specularReflectance = "" +
            "vec3 computeSpecularReflectance(vec3 specularColor, vec3 V, vec3 H){\n" +
            "    float HdotV = clamp(dot(H, V), 0.0, 1.0);\n" +
            "    return specularColor + (1.0 - specularColor) * pow(1.0 - HdotV, 5.0);\n" +
            "}\n"

    /**
     * Computation of the geometric shadowing or "specular geometric attenuation".
     * This describes how much the microfacets of the surface shadow each other,
     * based on the roughness of the surface.
     * This implementation is based on [1], which contains some odd tweaks and
     * cross-references to [3] and [4], which I did not follow in all depth.
     * Let's hope they know their stuff.
     * It receives the roughness value, the normal vector of the surface N,
     * the vector from the surface to the viewer V, the vector from the
     * surface to the light L, and the half vector H
     * */
    val specularAttenuation = "" +
            "float computeSpecularAttenuation(float roughness, vec3 V, vec3 N, vec3 L, vec3 H){\n" +
            "    float NdotL = dot(N, L);\n" + // guaranteed to be > 0
            "    float NdotV = abs(dot(N, V));\n" + // back face gets same shading (if < 0)
            "    float k = (roughness + 1.0) * (roughness + 1.0) * 0.125;\n" +
            "    \n" +
            "    float GL = NdotL / (NdotL * (1.0 - k) + k);\n" +
            "    float GV = NdotV / (NdotV * (1.0 - k) + k);\n" +
            "    \n" +
            "    return GL * GV;\n" +
            "}\n"


    val specularAttenuationNoDiv = "" + // without times NdotV, because we divide by it in the result
            "float computeSpecularAttenuation(float roughness, vec3 V, vec3 N, vec3 L, vec3 H){\n" +
            "    float NdotL = dot(N, L);\n" + // guaranteed to be > 0
            "    float NdotV = abs(dot(N, V));\n" + // back face gets same shading (if < 0)
            "    float k = (roughness + 1.0) * (roughness + 1.0) * 0.125;\n" +
            "    \n" +
            "    float invK = 1.0 - k;\n" +
            "    float t0 = NdotL * invK + k;\n" +
            "    float t1 = NdotV * invK + k;\n" +
            "    \n" +
            "    return NdotL / (t0 * t1);\n" +
            "}\n"

    /**
     * Compute the BRDF, as it is described in [1], with a reference
     * to [5], although the formula does not seem to appear there.
     * The inputs are the base color and metallic/roughness values,
     * the normal vector of the surface N, the vector from the surface
     * to the viewer V, the vector from the surface to the light L,
     * and the half vector H
     * */
    val specularBRDFv2 = "" +
            "vec3 computeSpecularBRDF(vec3 specularInputColor, float roughness, vec3 V, vec3 N, vec3 L, float NdotL, vec3 H){\n" +
            // Compute the microfacet distribution (D)
            "    float microfacetDistribution = computeMicrofacetDistribution(H, N, roughness);\n" +
            "    \n" +
            // Compute the specularly reflected color (F)\n
            "    vec3 specularReflectance = computeSpecularReflectance(specularInputColor, V, H);\n" +
            "    \n" +
            // Compute the geometric specular attenuation (G)
            "    float specularAttenuation = computeSpecularAttenuation(roughness, V, N, L, H);\n" +
            "    \n" +
            // The seemingly arbitrary clamping to avoid divide-by-zero was inspired by [6].
            "    float NdotV = abs(dot(N, V));\n" +
            "    vec3 specularBRDF = vec3(0.0);\n" +
            "    float d = microfacetDistribution;\n" +
            "    vec3 f = specularReflectance;\n" +
            "    float g = specularAttenuation;\n" +
            "    return (d * f * g) / (4.0 * NdotL * NdotV);\n" +
            "}\n"

    /*val specularBRDFv2NoDiv = "" +
            microfacetDistribution +
            specularReflectance +
            specularAttenuationNoDiv +
            "vec3 computeSpecularBRDF(vec3 specularInputColor, float roughness, vec3 V, vec3 N, vec3 L, float NdotL, vec3 H){\n" +
            // Compute the microfacet distribution (D)
            "    float D = computeMicrofacetDistribution(H, N, roughness);\n" +
            // Compute the specularly reflected color (F)\n
            "    vec3  F = computeSpecularReflectance(specularInputColor, V, H);\n" +
            // Compute the geometric specular attenuation (G)
            "    float G = computeSpecularAttenuation(roughness, V, N, L, H);\n" +
            "    return (D * F * G) * 0.25;\n" + // NdotL is already in the light equation, NdotV is in G
            "}\n"*/

    // todo regular plastic has specular as well... why not in this model?
    val specularBRDFv2NoDivInlined = "" +
            "vec3 computeSpecularBRDF(vec3 specularInputColor, float roughness, vec3 V, vec3 N, float NdotL, float NdotV, vec3 H){\n" +
            // Compute the microfacet distribution (D)
            "    float alpha = roughness * roughness;\n" +
            "    float Dx = alpha * alpha;\n" +
            "    float NdotH = dot(N, H);\n" + // clamp is probably unnecessary; could be inserted back
            "    float NdotH_squared = NdotH * NdotH;\n" +
            "    float x = NdotH_squared * (Dx - 1.0) + 1.0;\n" +
            // "    float Dx = alpha_squared;\n" +
            // Compute the specularly reflected color (F)\n
            // "    float HdotV = clamp(dot(H, V), 0.0, 1.0);\n" + // clamp is probably unnecessary ^^
            "    float HdotV = dot(H, V);\n" +
            "    float invHov = 1.0 - HdotV, invHov2 = invHov*invHov, invHov5 = invHov*invHov2*invHov2;\n" +
            // is x*(x*x)*(x*x) faster than pow5? yes it is, by 2.8x
            // "    vec3  F = specularInputColor + (1.0 - specularInputColor) * pow(1.0 - HdotV, 5.0);\n" +
            "    vec3  F = specularInputColor + (1.0 - specularInputColor) * invHov5;\n" +
            // Compute the geometric specular attenuation (G)
            // "    float NdotL = dot(N, L);\n" + // guaranteed to be > 0; already defined
            // "    float NdotV = abs(dot(N, V));\n" + // back face gets same shading (if < 0); clamping is needed here
            "    float rp1 = roughness + 1.0;\n" +
            "    float k = rp1 * rp1 * 0.125;\n" +
            "    vec2 t = vec2(NdotL,NdotV)*(1-k)+k;\n" +
            //"    float Gx = NdotL;\n" +
            // NdotL is already in the light equation, NdotV is in G
            // also we don't need two divisions, we can use one
            "    return ((Dx * NdotL) * ${0.25 / PI} / (x * x * t.x * t.y)) * F;\n" +
            "}\n"

    // we can extract common factors (which always appear, and never change for a pixel)
    // alpha, Dx, rp1, k, 1-k
    val specularBRDFv2NoDivInlined2Start = "" +
            "float alpha = finalRoughness * finalRoughness;\n" +
            "float Dx = alpha * alpha, DxM1 = Dx - 1.0;\n" +
            "float rp1 = finalRoughness + 1.0;\n" +
            "float k = rp1 * rp1 * 0.125, invK = 1.0-k;\n" +
            "vec3 invSpecularColor = 1.0 - specularColor;\n" +
            "float DxPi4 = Dx * ${0.25 / PI};\n"

    // factor extracted from the BRDF
    // also ambient must be added at the end, or we would need to divide by DxPi4 at the start
    val specularBRDFv2NoDivInlined2End = "specularLight = specularLight * DxPi4 + specularColor * ambientLight;\n"

    // (specularColor, finalRoughness, V, finalNormal, NdotL, NdotV, H)
    val specularBRDFv2NoDivInlined2 = "" +
            // Compute the microfacet distribution (D)
            "float NdotH = dot(finalNormal, H);\n" + // clamp is probably unnecessary; could be inserted back
            "float NdotH_squared = NdotH * NdotH;\n" +
            "float x = NdotH_squared * DxM1 + 1.0;\n" +
            // "    float Dx = alpha_squared;\n" +
            // Compute the specularly reflected color (F)
            // "    float HdotV = clamp(dot(H, V), 0.0, 1.0);\n" + // clamp is probably unnecessary ^^
            "float HdotV = dot(H, V);\n" +
            "float invHov = 1.0 - HdotV, invHov2 = invHov*invHov, invHov5 = invHov*invHov2*invHov2;\n" +
            // is x*(x*x)*(x*x) faster than pow5? yes it is, by 2.8x
            // "    vec3  F = specularInputColor + (1.0 - specularInputColor) * pow(1.0 - HdotV, 5.0);\n" +
            "vec3 F = specularColor + invSpecularColor * invHov5;\n" +
            // Compute the geometric specular attenuation (G)
            // "    float NdotL = dot(N, L);\n" + // guaranteed to be > 0; already defined
            // "    float NdotV = abs(dot(N, V));\n" + // back face gets same shading (if < 0); clamping is needed here
            "vec2 t = vec2(NdotL,NdotV)*invK+k;\n" +
            //"    float Gx = NdotL;\n" +
            // NdotL is already in the light equation, NdotV is in G
            // also we don't need two divisions, we can use one
            "vec3 computeSpecularBRDF = (NdotL / (x * x * t.x * t.y)) * F;\n"

    // the following functions can be used, if the color isn't yet available
    // (or you want to use one texture access less)
    // they have missing color, when H || V, so when you look perpendicular onto the surface
    val specularBRDFv2NoColorStart = "" +
            "float alpha = finalRoughness * finalRoughness;\n" +
            "float Dx = alpha * alpha, DxM1 = Dx - 1.0;\n" +
            "float rp1 = finalRoughness + 1.0;\n" +
            "float k = rp1 * rp1 * 0.125, invK = 1.0-k;\n" +
            "float DxPi4 = Dx * ${0.25 / PI};\n"

    // (finalRoughness, finalNormal, NdotL, NdotV, H)
    val specularBRDFv2NoColor = "" +
            // Compute the microfacet distribution (D)
            "float NdotH = dot(finalNormal, H);\n" + // clamp is probably unnecessary; could be inserted back
            "float NdotH_squared = NdotH * NdotH;\n" +
            "float x = NdotH_squared * DxM1 + 1.0;\n" +
            // "    float Dx = alpha_squared;\n" +
            // skipped: computing the specularly reflected color (F)
            // "    vec3  F = specularInputColor + (1.0 - specularInputColor) * pow(1.0 - HdotV, 5.0);\n" +
            // Compute the geometric specular attenuation (G)
            // "    float NdotL = dot(N, L);\n" + // guaranteed to be > 0; already defined
            // "    float NdotV = abs(dot(N, V));\n" + // back face gets same shading (if < 0); clamping is needed here
            "vec2 t = vec2(NdotL,NdotV)*invK+k;\n" +
            //"    float Gx = NdotL;\n" +
            // NdotL is already in the light equation, NdotV is in G
            // also we don't need two divisions, we can use one
            "#define computeSpecularBRDF NdotL/(x*x*t.x*t.y)\n"

    val specularBRDFv2NoColorEnd = "specularLight *= DxPi4;\n"

    /**
     * Compute the vector from the surface point to the light (L),
     * the vector from the surface point to the viewer (V),
     * and the half-vector between both (H)
     * The camera position in view space is fixed.
     * */
    val combineMainColor = "" +
            microfacetDistribution +
            specularReflectance +
            specularAttenuation +
            specularBRDFv2 +
            "vec3 combineMainColor(vec3 baseColor, vec3 normal, float metallic, float roughness, vec3 emissive, float occlusionFactor, vec3 lightDir){\n" +
            "    \n" +
            // if the light and the normal are in opposite directions, won't be light
            "    float NdotL = dot(normal, lightDir);\n" +
            "    if(NdotL <= 0.0) return vec3(0.0);\n" +
            "    \n" +
            "    vec3 V =-normalize(finalPosition);\n" + // norm(camera position - vertex position), camera position is (0,0,0)
            "    vec3 H = normalize(lightDir + V);\n" +
            "    \n" +
            "    vec3 diffuseColor = baseColor.rgb * (1.0 - metallic);\n" + // diffuse part (lerped influence)
            "    vec3 diffuse = diffuseColor * NdotL;\n" +
            "    \n" +
            "    vec3 specularInputColor = baseColor.rgb * metallic;\n" + // specular part (lerped influence)
            "    vec3 specularBRDF = computeSpecularBRDF(specularInputColor, roughness, V, normal, lightDir, H);\n" +
            "    vec3 specular = specularInputColor * specularBRDF;\n" +
            "    \n" +
            "    return diffuse + specular;\n" + // we could accumulate this in the light buffer
            "}\n"

    val combineWithEmissive = "" +
            combineMainColor +
            "vec3 combineWithEmissive(vec3 mainColor, float occlusionFactor, vec3 emissive){\n" +
            "   return mainColor * occlusionFactor + emissive" +
            "}\n"


}