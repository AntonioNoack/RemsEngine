package me.anno.engine.ui.render

import me.anno.ecs.components.light.LightType
import me.anno.engine.pbr.PBRLibraryGLTF
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.gpu.pipeline.LightShaders.translucencyNL
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded

object RendererLib {

    val fresnelSchlick =  "" +
            "float fresnelSchlick(float cosine, float ior) {\n" +
            "   float r0 = (1.0 - ior) / (1.0 + ior);\n" +
            "   r0 = r0 * r0;\n" +
            "   return r0 + (1.0 - r0) * pow(1.0 - cosine, 5.0);\n" +
            "}\n"

    val getReflectivity = "" +
            "float getReflectivitySq(float roughness, float metallic){\n" +
            "   return max(mix(0.1,1.0,metallic),0.0) * max(0.0,1.0-roughness);\n" +
            "}\n" +
            "float getReflectivity(float roughness, float metallic){\n" +
            "   return sqrt(getReflectivitySq(roughness,metallic));\n" +
            "}\n"

    val skyMapCode = "" +
            // todo it would be nice, if we could search the reflectionMap using its depth
            //  like screen-space reflections to get a more 3d look
            "   if(dot(finalPosition,finalPosition) < 1e38){\n" +
            "       float reflectivity = getReflectivitySq(finalRoughness,finalMetallic);\n" +
            "       if(reflectivity > 0.0){\n" +
            "           vec3 dir = $cubemapsAreLeftHanded * reflect(V, finalNormal);\n" +
            "           float lod = finalRoughness * 10.0;\n" +
            "           vec3 skyColor = 0.15 * finalEmissive + finalColor0 * textureLod(reflectionMap, dir, lod).rgb;\n" +
            "           finalColor = mix(finalColor, skyColor, sqrt(reflectivity) * (1.0 - finalOcclusion));\n" +
            "       }\n" +
            "   }\n"

    val sampleSkyboxForAmbient = "" +
            // depends on metallic & roughness
            // todo why does the LOD look to be well-chosen, although I have done nothing?
            "vec3 sampleSkyboxForAmbient(vec3 dir, float reflectivity) {\n" +
            "   return (1.0 - reflectivity) * texture(skybox, -${cubemapsAreLeftHanded} * dir).rgb;\n" +
            "}\n"

    val lightCode = "" +
            colorToLinear +
            "   vec3 V = normalize(-finalPosition);\n" +
            // light calculations
            "   float NdotV = abs(dot(finalNormal,V));\n" +
            "   vec3 finalColor0 = finalColor;\n" +
            "   float reflectivity = getReflectivity(finalRoughness,finalMetallic);\n" +
            "   vec3 diffuseColor = finalColor * (1.0 - reflectivity);\n" +
            "   vec3 specularColor = finalColor * reflectivity;\n" +
            "   vec3 diffuseLight = sampleSkyboxForAmbient(finalNormal, reflectivity);\n" +
            "   vec3 specularLight = vec3(0.0);\n" +
            "   bool hasSpecular = dot(specularColor,vec3(1.0)) > 0.0001;\n" +
            "   bool hasDiffuse = dot(diffuseColor,vec3(1.0)) > 0.0001;\n" +
            "   vec3 lightPos = vec3(0.0), lightDir = vec3(0.0), lightNor = vec3(0.0);\n" +
            "   vec3 effectiveSpecular = vec3(0.0), effectiveDiffuse = vec3(0.0);\n" +
            "   float NdotL = 0.0;\n" + // normal dot light
            "   if(hasDiffuse || hasSpecular){\n" +
            PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start +
            "       for(int i=0;i<numberOfLights;i++){\n" +
            "           mat4x3 camSpaceToLightSpace = invLightMatrices[i];\n" +
            // local space, for falloff and such
            "           lightPos = matMul(camSpaceToLightSpace, vec4(finalPosition,1.0));\n" +
            "           lightNor = normalize(matMul(camSpaceToLightSpace, vec4(finalNormal,0.0)));\n" +
            "           vec3 viewDir = normalize(matMul(camSpaceToLightSpace, vec4(finalPosition, 0.0)));\n" +
            // "       if(!hasSpecular && dot(dir,dir) >= 1.0) continue;\n" +
            "           vec4 data0 = lightData0[i];\n" + // color, type
            "           float data1 = lightData1[i];\n" + // point: radius, spot: angle
            "           vec4 data2 = shadowData[i];\n" +
            "           vec3 lightColor = data0.rgb;\n" +
            "           int lightType = int(data0.a);\n" +
            "           int shadowMapIdx0 = int(data2.r);\n" +
            "           int shadowMapIdx1 = int(data2.g);\n" +
            "           float shaderV0 = data1, shaderV1 = data2.z, shaderV2 = data2.w;\n" +
            // local coordinates of the point in the light "cone"
            // removed switch(), because WebGL had issues with continue inside it...
            LightType.entries.joinToString("") {
                val start = if (it.ordinal == 0) "if" else " else if"
                val co = "continue" // cutoff keyword
                val ws = true // with shadows
                "$start(lightType == ${it.ordinal}){\n${LightType.getShaderCode(it, co, ws)}}"
            } + "\n" +
            "           if(hasSpecular && dot(effectiveSpecular, vec3(NdotL)) > ${0.5 / 255.0}){\n" +
            "               specularLight += effectiveSpecular;// * computeSpecularBRDF;\n" +
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
            colorToLinear +
            "   finalColor = diffuseColor * diffuseLight + specularLight;\n" +
            "   finalColor = finalColor * (1.0 - finalOcclusion) + finalEmissive;\n"
}