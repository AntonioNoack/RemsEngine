package me.anno.engine.ui.render

import me.anno.ecs.components.light.LightType
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.gpu.pipeline.LightShaders.addDiffuseLight
import me.anno.gpu.pipeline.LightShaders.addSpecularLight
import me.anno.gpu.pipeline.LightShaders.combineLightFinishLine
import me.anno.gpu.pipeline.LightShaders.mixAndClampLight
import me.anno.gpu.pipeline.LightShaders.startLightSum
import me.anno.gpu.shader.ShaderLib.roughnessIfMissing
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded

object RendererLib {

    val fresnelSchlick = "" +
            "#ifndef FRESNEL_SCHLICK\n" +
            "#define FRESNEL_SCHLICK\n" +
            "float fresnelSchlickR0(float ior) {\n" +
            "   float r0 = (1.0 - ior) / (1.0 + ior);\n" +
            "   r0 = r0 * r0;\n" +
            "   return r0;\n" +
            "}\n" +
            "float fresnelSchlick(float cosine, float ior) {\n" +
            "   float r0 = fresnelSchlickR0(ior);\n" +
            "   return r0 + (1.0 - r0) * sqrt(max(1.0 - cosine, 0.0));\n" +
            "}\n" +
            "#endif\n"

    val getReflectivity = "" +
            "#ifndef GET_REFLECTIVITY\n" +
            "#define GET_REFLECTIVITY\n" +
            "float getReflectivity(float roughness, float metallic){\n" +
            "   float result = mix(mix(0.1, 1.0, metallic), 0.0, roughness);\n" +
            "   return result > 0.0 ? min(result,1.0) : 0.0;\n" + // clamping incl. handling for NaN
            "}\n" +
            "#endif\n"

    val skyMapCode = "" +
            colorToSRGB +
            // todo it would be nice, if we could search the reflectionMap using its depth
            //  like screen-space reflections to get a more 3d look
            "   if(dot(finalPosition,finalPosition) < 1e38){\n" +
            "       float reflectivity = finalReflectivity;\n" +
            "       if(reflectivity > 0.0){\n" +
            "           vec3 dir = $cubemapsAreLeftHanded * reflect(V, finalNormal);\n" +
            "           float lod = finalRoughness * 10.0;\n" +
            "           vec3 skyColor = 0.15 * finalEmissive + finalColor0 * textureLod(reflectionMap, dir, lod).rgb;\n" +
            "           finalColor = mix(finalColor, skyColor, reflectivity * (1.0 - finalOcclusion));\n" +
            "       }\n" +
            "   }\n"

    val sampleSkyboxForAmbient = "" +
            // depends on metallic & roughness
            "vec3 sampleSkyboxForAmbient(vec3 dir, float finalRoughness, float reflectivity) {\n" +
            "   float lod = finalRoughness * 10.0;\n" +
            "   return (1.0 - reflectivity) * textureLod(reflectionMap, -${cubemapsAreLeftHanded} * dir, lod).rgb;\n" +
            "}\n"

    val defineLightVariables = "" +
            "vec3 lightColor = data0.rgb;\n" +
            "int lightType = int(data0.w);\n" +
            "int shadowMapIdx0 = int(data2.x);\n" +
            "int shadowMapIdx1 = canHaveShadows ? int(data2.y) : 0;\n" +
            "float shaderV0 = data1.x, shaderV1 = data1.y, shaderV2 = data1.z, shaderV3 = data1.w;\n"

    val lightCode = "" +
            colorToLinear +
            "   vec3 V = normalize(-finalPosition);\n" +
            // light calculations
            "   float NdotV = abs(dot(finalNormal,V));\n" +
            "   vec3 finalColor0 = finalColor;\n" +
            startLightSum +
            "   float NdotL = 0.0;\n" + // normal dot light
            "   for(int i=0;i<numberOfLights;i++){\n" +
            "       mat4x3 camSpaceToLightSpace = invLightMatrices[i];\n" +
            "       vec3 lightDir = vec3(0.0,0.0,-1.0);\n" +
            // local space, for falloff and such
            "       vec3 lightPos = matMul(camSpaceToLightSpace, vec4(finalPosition,1.0));\n" +
            "       vec3 lightNor = normalize(matMul(camSpaceToLightSpace, vec4(finalNormal,0.0)));\n" +
            "       vec3 viewDir = normalize(matMul(camSpaceToLightSpace, vec4(finalPosition, 0.0)));\n" +
            "       vec3 effectiveDiffuse = vec3(0.0), effectiveSpecular = vec3(0.0);\n" +
            "       vec4 data0 = lightData0[i];\n" + // color, type
            "       vec4 data1 = lightData1[i];\n" + // point: radius, spot: angle
            "       vec4 data2 = lightData2[i];\n" +
            defineLightVariables +
            // local coordinates of the point in the light "cone"
            // removed switch(), because WebGL had issues with continue inside it...
            LightType.entries.joinToString("") { type ->
                val start = if (type.ordinal == 0) "if" else " else if"
                val cutoffKeyword = "continue"
                val withShadows = true
                "$start(lightType == ${type.id}){\n${LightType.getShaderCode(type, cutoffKeyword, withShadows)}}"
            } + "\n" +
            addSpecularLight +
            addDiffuseLight +
            "   }\n"

    val combineLightCode = "" +
            "   vec3 light;\n" +
            mixAndClampLight +
            // for roughness, use finalRoughness, if available, or 1-reflectivity, if not
            roughnessIfMissing +
            "   light += sampleSkyboxForAmbient(finalNormal, finalRoughness, reflectivity);\n" +
            // cheating to make metallic stuff not too dark when using forward rendering
            // -> doesn't work as nicely, as I had hoped
            // "   light += 0.5 * reflectivity * brightness(diffuseLight);\n" +
            colorToLinear +
            "   float invOcclusion = (1.0 - finalOcclusion);\n" +
            combineLightFinishLine
}