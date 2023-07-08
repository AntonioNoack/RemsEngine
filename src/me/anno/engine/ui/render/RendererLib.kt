package me.anno.engine.ui.render

import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.engine.pbr.PBRLibraryGLTF
import me.anno.gpu.pipeline.LightShaders.translucencyNL

object RendererLib {

    val skyMapCode = "" +
            // todo it would be nice, if we could search the reflectionMap using its depth
            //  like screen-space reflections to get a more 3d look
            "   if(dot(finalPosition,finalPosition) < 1e38){\n" +
            "       float reflectivity = finalMetallic * (1.0 - finalRoughness);\n" +
            "       float maskSharpness = 1.0;\n" + // shouldn't be hardcoded
            "       reflectivity = (reflectivity - 1.0) * maskSharpness + 1.0;\n" +
            "       if(reflectivity > 0.0){\n" +
            // todo why do I need to flip x here???
            "           vec3 dir = vec3(-1,1,1) * reflect(V, finalNormal);\n" +
            "           vec3 skyColor = 0.15 * finalEmissive + finalColor0 * texture(reflectionMap, dir).rgb;\n" +
            "           finalColor = mix(finalColor, skyColor, sqrt(reflectivity) * (1.0 - finalOcclusion));\n" +
            "       }\n" +
            "   }\n"

    val lightCode = "" +
            "   vec3 V = normalize(-finalPosition);\n" +
            // light calculations
            "   float NdotV = abs(dot(finalNormal,V));\n" +
            "   vec3 finalColor0 = finalColor;\n" +
            "   vec3 diffuseColor = finalColor * (1.0 - finalMetallic);\n" +
            "   vec3 specularColor = finalColor * finalMetallic;\n" +
            "   vec3 diffuseLight = ambientLight, specularLight = vec3(0.0);\n" +
            "   bool hasSpecular = dot(specularColor,vec3(1.0)) > 0.001;\n" +
            "   bool hasDiffuse = dot(diffuseColor,vec3(1.0)) > 0.001;\n" +
            "   vec3 lightDirWS, localNormal, effectiveSpecular, effectiveDiffuse;\n" +
            "   float NdotL = 0.0;\n" + // normal dot light
            "   if(hasDiffuse || hasSpecular){\n" +
            PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start +
            "       for(int i=0;i<numberOfLights;i++){\n" +
            "           mat4x3 camSpaceToLightSpace = invLightMatrices[i];\n" +
            "           vec3 dir = matMul(camSpaceToLightSpace, vec4(finalPosition,1.0));\n" + // local coordinates for falloff
            // "       if(!hasSpecular && dot(dir,dir) >= 1.0) continue;\n" +
            "           vec4 data0 = lightData0[i];\n" + // color, type
            "           vec4 data1 = lightData1[i];\n" + // point: position, radius, spot: position, angle
            "           vec4 data2 = shadowData[i];\n" +
            "           vec3 lightColor = data0.rgb;\n" +
            "           int lightType = int(data0.a);\n" +
            "           lightDirWS = effectiveDiffuse = effectiveSpecular = vec3(0.0);\n" + // making Nvidia GPUs happy
            "           localNormal = normalize(matMul(mat3x3(camSpaceToLightSpace), finalNormal));\n" +
            "           int shadowMapIdx0 = int(data2.r);\n" +
            "           int shadowMapIdx1 = int(data2.g);\n" +
            // local coordinates of the point in the light "cone"
            // removed switch(), because WebGL had issues with continue inside it...
            "           if(lightType == ${LightType.DIRECTIONAL.id}){\n" +
            DirectionalLight.getShaderCode("continue", true) +
            "           } else if(lightType == ${LightType.POINT.id}){\n" +
            PointLight.getShaderCode("continue", true, hasLightRadius = true) +
            "           } else {\n" +
            SpotLight.getShaderCode("continue", true) +
            "           }\n" +
            "           if(hasSpecular && dot(effectiveSpecular, vec3(NdotL)) > ${0.5 / 255.0}){\n" +
            "               vec3 H = normalize(V + lightDirWS);\n" +
            PBRLibraryGLTF.specularBRDFv2NoDivInlined2 +
            "               specularLight += effectiveSpecular * computeSpecularBRDF;\n" +
            "           }\n" +
            // translucency; looks good and approximately correct
            // sheen is a fresnel effect, which adds light
            "           NdotL = mix(NdotL, $translucencyNL, finalTranslucency) + finalSheen;\n" +
            "           diffuseLight += effectiveDiffuse * clamp(NdotL, 0.0, 1.0);\n" +
            "       }\n" +
            PBRLibraryGLTF.specularBRDFv2NoDivInlined2End +
            "   }\n"

    val combineLightCode = "" +
            // respect reflectionMap, todo multiple samples?
            // todo base LOD on roughness (and maybe metallic)
            // respect sky -> sky can be baked as reflectionMap, if we find none :)
            "   finalColor = diffuseColor * diffuseLight + specularLight;\n" +
            "   finalColor = finalColor * (1.0 - finalOcclusion) + finalEmissive;\n"

}