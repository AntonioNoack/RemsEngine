package me.anno.engine.pbr

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
            "    float NdotL = clamp(dot(N, L), 0.0, 1.0);\n" +
            "    float NdotV = clamp(dot(N, V), 0.0, 1.0);\n" +
            "    float k = (roughness + 1.0) * (roughness + 1.0) * 0.125;\n" +
            "    \n" +
            "    float GL = NdotL / (NdotL * (1.0 - k) + k);\n" +
            "    float GV = NdotV / (NdotV * (1.0 - k) + k);\n" +
            "    \n" +
            "    return GL * GV;\n" +
            "}\n"

    /**
     * Compute the BRDF, as it is described in [1], with a reference
     * to [5], although the formula does not seem to appear there.
     * The inputs are the base color and metallic/roughness values,
     * the normal vector of the surface N, the vector from the surface
     * to the viewer V, the vector from the surface to the light L,
     * and the half vector H
     * */
    val specularBRDF = "" +
            "vec3 computeSpecularBRDF(vec4 baseColor, float metallic, float roughness, vec3 V, vec3 N, vec3 L, vec3 H){\n" +
            // Compute the microfacet distribution (D)
            "    float microfacetDistribution = computeMicrofacetDistribution(H, N, roughness);\n" +
            "    \n" +
            // Compute the specularly reflected color (F)\n
            "    vec3 specularInputColor = (baseColor.rgb * metallic);\n" +
            "    vec3 specularReflectance = computeSpecularReflectance(specularInputColor, V, H);\n" +
            "    \n" +
            // Compute the geometric specular attenuation (G)
            "    float specularAttenuation = computeSpecularAttenuation(roughness, V, N, L, H);\n" +
            "    \n" +
            // The seemingly arbitrary clamping to avoid divide-by-zero was inspired by [6].
            "    float NdotV = dot(N, V);\n" +
            "    float NdotL = dot(N, L);\n" +
            "    vec3 specularBRDF = vec3(0.0);\n" +
            "    if (NdotV > 0.0001 && NdotL > 0.0001){\n" +
            "        float d = microfacetDistribution;\n" +
            "        vec3 f = specularReflectance;\n" +
            "        float g = specularAttenuation;\n" +
            "        specularBRDF = (d * f * g) / (4.0 * NdotL * NdotV);\n" +
            "    }\n" +
            "    return specularBRDF;\n" +
            "}\n"

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
            specularBRDF +
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
            "    vec3 specularInputColor = (baseColor.rgb * metallic);\n" + // specular part (lerped influence)
            "    vec3 specularBRDF = computeSpecularBRDF(baseColor, metallic, roughness, V, normal, lightDir, H);\n" +
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