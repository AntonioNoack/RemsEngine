package me.anno.engine.ui.render

import me.anno.ecs.components.light.LightType
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
            LightType.values().joinToString("") {
                val start = if (it.ordinal == 0) "if" else " else if"
                val co = "continue" // cutoff keyword
                val ws = true // with shadows
                "$start(lightType == ${it.ordinal}){\n${LightType.getShaderCode(it, co, ws)}}"
            } + "\n" +
            "           if(hasSpecular && dot(effectiveSpecular, vec3(NdotL)) > ${0.5 / 255.0}){\n" +
            "               vec3 lightDirWS = normalize(matMul(lightMatrices[i],vec4(lightDir,0.0)));\n" +
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