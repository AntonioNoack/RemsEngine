package me.anno.experiments.ocean

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.types.Booleans.hasFlag

object OceanShader : ECSMeshShader("ocean") {

    val waveHeight = "float getWaveHeight(vec2 pos) {\n" +
            "   float y = 0.0;\n" +
            "   y += texture(waveHeightMap, pos * 0.01 + normal1Offset).x;\n" +
            "   y += texture(waveHeightMap, pos * 0.02 + normal2Offset).x;\n" +
            "   y += texture(waveHeightMap, pos * 0.04 + normal3Offset).x;\n" +
            "   return (y - 1.5);\n" +
            "}\n"

    override fun animateVertex(key: ShaderKey): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "ocean-waves",
                listOf(
                    Variable(GLSLType.V3F, "localPosition", VariableMode.INOUT),
                    Variable(GLSLType.V3F, "normal", VariableMode.INOUT),
                    Variable(GLSLType.S2D, "waveHeightMap"),
                    Variable(GLSLType.V2F, "normal1Offset"),
                    Variable(GLSLType.V2F, "normal2Offset"),
                    Variable(GLSLType.V2F, "normal3Offset"),
                    Variable(GLSLType.V4F, "waveBounds")
                ),
                """
                vec2 pos = localPosition.xz;
                vec2 minus = pos - waveBounds.xy;
                vec2 plus = waveBounds.zw - pos;
                float waveScale = 0.2 * min(min(plus.x,plus.y), min(minus.x,minus.y));
                localPosition.y += getWaveHeight(pos) * clamp(waveScale, 0.0, 1.0);
                """
            ).add(waveHeight)
        )
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key) + listOf(
                    Variable(GLSLType.S2D, "depthTex"),
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.S2D, "waveHeightMap"),
                    Variable(GLSLType.V2F, "normal1Offset"),
                    Variable(GLSLType.V2F, "normal2Offset"),
                    Variable(GLSLType.V2F, "normal3Offset"),
                ) + depthVars,
                concatDefines(key).toString() +
                        discardByCullingPlane +

                        // todo reflection is added twice?
                        // todo why is the reflection map only available in deferred mode?
                        if (key.flags.hasFlag(NEEDS_COLORS)) {
                            """
                    vec2 pos = localPosition.xz;             
                    vec2 dx = vec2(1.0,0.0), dz = vec2(0.0,1.0);
                    float dyx = getWaveHeight(pos+dx) - getWaveHeight(pos-dx);
                    float dyz = getWaveHeight(pos+dz) - getWaveHeight(pos-dz);
                    vec3 finalNormal = normalize(vec3(dyx, 2.0, dyz));
                    
                    // base normal already provided by vertex stage
                    vec3 N = normalize(finalNormal);
                    vec3 V = normalize(-finalPosition);
            
                    // --- Fresnel ---
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 5.0);
            
                    // --- Water colors ---
                    vec3 deepColor = vec3(0.0, 0.05, 0.1);
                    vec3 shallowColor = vec3(0.0, 0.3, 0.5);
            
                    // fake depth using view angle
                    ivec2 depthUV = ivec2(gl_FragCoord.xy);
                    float sampledDepth = rawToDepth(texelFetch(depthTex,depthUV,0).x);
                    float zDistance = 1.0 / gl_FragCoord.w;
                    float distanceToSurface = sampledDepth - zDistance;
                    
                    vec3 depthFactor3 = exp(-vec3(0.35, 0.10, 0.02) * 0.1 * abs(distanceToSurface / N.y));
                    vec3 waterColor = mix(deepColor, shallowColor, depthFactor3);
                    float depthFactor = depthFactor3.g;
            
                    // --- Reflection ---
                    vec3 reflectionDir = reflect(-V, N);
                    float lod = 0.05 + (1.0 - depthFactor) * 4.0;
            
                    vec3 reflectionColor = vec3(0.0);
                    #ifdef DEFERRED
                        reflectionColor = textureLod(reflectionMap, reflectionDir, lod).rgb;
                    #endif
            
                    // --- Foam ---
                    float foam = smoothstep(0.7, 1.0, depthFactor) * 0.3;
            
                    // --- Final composition ---
                    finalColor = mix(waterColor, reflectionColor, fresnel);
                    finalColor += foam;
            
                    finalMetallic = 0.1;
                    finalRoughness = mix(0.02, 0.2, depthFactor);
                    finalReflectivity = fresnel;
            
                    finalEmissive = vec3(0.0);
                    finalAlpha = 1.0;
                    finalOcclusion = 1.0;
                    finalSheen = 0.0;
                    """ + v0 + reflectionPlaneCalculation +
                                    reflectionMapCalculation
                        } else ""

            ).add(ShaderLib.brightness).add(rawToDepth).add(RendererLib.getReflectivity)
                .add(ShaderLib.applyTiling).add(waveHeight)
        )
    }

}