package me.anno.engine.ui.render

import me.anno.ecs.components.light.LightType
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2End
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.shader.builder.Function
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.studio.rems.Scene
import me.anno.utils.maths.Maths.length
import org.joml.Vector4f
import org.lwjgl.opengl.GL20
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Renderers {

    val overdrawRenderer = object : Renderer("overdraw", true, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage(
                "overdraw", listOf(
                    Variable("vec3", "finalColor", false),
                    Variable("float", "finalAlpha", false)
                ), "" +
                        "finalColor = vec3(0.125);\n" +
                        "finalAlpha = 1.0;\n"
            )
        }
    }

    val cheapRenderer = object : Renderer("cheap", false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage(
                "cheap", listOf(
                    Variable("vec3", "finalColor", false)
                ), "finalColor = vec3(0.5);"
            )
        }
    }

    val pbrRenderer = object : Renderer("pbr", false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage("pbr", listOf(
                // light data
                Variable("vec3", "ambientLight"),
                Variable("int", "numberOfLights"),
                Variable("mat4x3", "invLightMatrices", RenderView.MAX_LIGHTS),
                Variable("vec4", "lightData0", RenderView.MAX_LIGHTS),
                Variable("vec4", "lightData1", RenderView.MAX_LIGHTS),
                Variable("vec4", "shadowData", RenderView.MAX_LIGHTS),
                // light maps for shadows
                // - spot lights, directional lights
                Variable("sampler2D", "shadowMapPlanar", MAX_PLANAR_LIGHTS),
                // - point lights
                Variable("samplerCube", "shadowMapCubic", MAX_CUBEMAP_LIGHTS),
                // debug
                Variable("int", "visualizeLightCount"),
                // material properties
                Variable("vec3", "finalEmissive"),
                Variable("float", "finalMetallic"),
                Variable("float", "finalRoughness"),
                Variable("float", "finalOcclusion"),
                Variable("float", "finalSheen"),
                Variable("vec3", "finalSheenNormal"),
                Variable("vec4", "finalClearCoat"),
                Variable("vec2", "finalClearCoatRoughMetallic"),
                // if the translucency > 0, the normal map probably should be turned into occlusion ->
                // no, or at max slightly, because the area around it will illuminate it
                Variable("float", "finalTranslucency"),
                Variable("float", "finalAlpha"),
                Variable("vec3", "finalPosition"),
                Variable("vec3", "finalNormal"),
                Variable("vec3", "finalColor", VariableMode.INOUT),
            ), "" +
                    // define all light positions, radii, types, and colors
                    // use the lights to illuminate the model
                    // light data
                    // a try of depth dithering, which can be used for plants, but is really expensive...
                    // "   gl_FragDepth = 1.0/(1.0+zDistance) * (1.0 + 0.001 * random(finalPosition.xy));\n" +
                    // shared pbr data
                    "   vec3 V = normalize(-finalPosition);\n" +
                    // light calculations
                    "   vec3 diffuseLight = ambientLight, specularLight = ambientLight;\n" +
                    "   float lightCount = 0;\n" +
                    "   float NdotV = abs(dot(finalNormal,V));\n" +
                    // fresnel for all fresnel based effects
                    "   float fresnel = 1.0 - NdotV, fresnel3 = pow(fresnel, 3.0);\n" +
                    "   if(finalClearCoat.w > 0.0){\n" +
                    // cheap clear coat effect
                    "       float clearCoatEffect = fresnel3 * finalClearCoat.w;\n" +
                    "       finalRoughness = mix(finalRoughness, finalClearCoatRoughMetallic.x, clearCoatEffect);\n" +
                    "       finalMetallic = mix(finalMetallic, finalClearCoatRoughMetallic.y, clearCoatEffect);\n" +
                    "       finalColor = mix(finalColor, finalClearCoat.rgb, clearCoatEffect);\n" +
                    "   }\n" +
                    // precalculate sheen
                    "   float sheenFresnel = 1.0 - abs(dot(finalSheenNormal,V));\n" +
                    "   float sheen = finalSheen * pow(sheenFresnel, 3.0);\n" +
                    "   vec3 diffuseColor = finalColor * (1.0 - finalMetallic);\n" +
                    "   vec3 specularColor = finalColor * finalMetallic;\n" +
                    "   bool hasSpecular = dot(specularColor,vec3(1.0)) > 0.001;\n" +
                    "   bool hasDiffuse = dot(diffuseColor,vec3(1.0)) > 0.001;\n" +
                    "   if(hasDiffuse || hasSpecular){\n" +
                    specularBRDFv2NoDivInlined2Start +
                    "       for(int i=0;i<numberOfLights;i++){\n" +
                    "           mat4x3 WStoLightSpace = invLightMatrices[i];\n" +
                    "           vec3 dir = invLightMatrices[i] * vec4(finalPosition,1.0);\n" + // local coordinates for falloff
                    // "       if(!hasSpecular && dot(dir,dir) >= 1.0) continue;\n" +
                    "           vec4 data0 = lightData0[i];\n" + // color, type
                    "           vec4 data1 = lightData1[i];\n" + // point: position, radius, spot: position, angle
                    "           vec4 data2 = shadowData[i];\n" +
                    "           vec3 lightColor = data0.rgb;\n" +
                    "           int lightType = int(data0.a);\n" +
                    "           vec3 lightPosition, lightDirWS, localNormal, effectiveLightColor, effectiveSpecular, effectiveDiffuse;\n" +
                    "           localNormal = normalize(WStoLightSpace * vec4(finalNormal,0.0));\n" +
                    "           float NdotL;\n" + // normal dot light
                    "           int shadowMapIdx0 = int(data2.r);\n" +
                    "           int shadowMapIdx1 = int(data2.g);\n" +
                    // local coordinates of the point in the light "cone"
                    "           switch(lightType){\n" +
                    LightType.values().joinToString("") {
                        "case ${it.id}:\n" +
                                when (it) {
                                    LightType.DIRECTIONAL -> {
                                        "" +
                                                // cutting it off may be useful for lamps, but a directional light typically is the sun or the moon
                                                "#define cutoff data2.a\n" +
                                                // box cutoff: max(max(abs(dir.x),abs(dir.y)),abs(dir.z))
                                                // sphere cutoff:
                                                "if(cutoff > 0.0){\n" +
                                                "   float cut = min(cutoff*(1-dot(dir,dir)),1);\n" +
                                                "   if(cut <= 0) continue;\n" +
                                                "   lightColor *= cut;\n" +
                                                "}\n" +
                                                "NdotL = localNormal.z;\n" + // dot(lightDirWS, globalNormal) = dot(lightDirLS, localNormal)
                                                // inv(W->L) * vec4(0,0,1,0) =
                                                // transpose(m3x3(W->L)) * vec3(0,0,1)
                                                "lightDirWS = normalize(vec3(WStoLightSpace[0][2],WStoLightSpace[1][2],WStoLightSpace[2][2]));\n" +
                                                "if(shadowMapIdx0 < shadowMapIdx1){\n" +
                                                // when we are close to the edge, we blend in
                                                "   float edgeFactor = min(20.0*(1.0-max(abs(dir.x),abs(dir.y))),1.0);\n" +
                                                "   if(edgeFactor > 0.0){\n" +
                                                "       #define shadowMapPower data2.b\n" +
                                                "       float invShadowMapPower = 1.0/shadowMapPower;\n" +
                                                "       vec2 shadowDir = dir.xy;\n" +
                                                "       vec2 nextDir = shadowDir * shadowMapPower;\n" +
                                                // find the best shadow map
                                                // blend between the two best shadow maps, if close to the border?
                                                // no, the results are already very good this way :)
                                                // at least at the moment, the seams are not obvious
                                                "       while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                                                "           shadowMapIdx0++;\n" +
                                                "           shadowDir = nextDir;\n" +
                                                "           nextDir *= shadowMapPower;\n" +
                                                "       }\n" +
                                                "       float depthFromShader = dir.z*.5+.5;\n" +
                                                "       if(depthFromShader > 0.0){\n" +
                                                // do the shadow map function and compare
                                                "           float depthFromTex = texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy, depthFromShader);\n" +
                                                // "           float val = texture_array_shadowMapPlanar(shadowMapIdx0, shadowDir.xy).r;\n" +
                                                // "           diffuseColor = vec3(val,val,dir.z);\n" + // nice for debugging
                                                "           lightColor *= 1.0 - edgeFactor * depthFromTex;\n" +
                                                "       }\n" +
                                                "   }\n" +
                                                "}\n" +
                                                "effectiveDiffuse = lightColor;\n" +
                                                "effectiveSpecular = lightColor;\n"
                                    }
                                    LightType.POINT -> {
                                        "" +
                                                "if(dot(dir,dir)>1.0) continue;\n" + // outside
                                                "float lightRadius = data1.a;\n" +
                                                "lightPosition = data1.rgb;\n" +
                                                // when light radius > 0, then adjust the light direction such that it looks as if the light was a sphere
                                                "lightDirWS = normalize(lightPosition - finalPosition);\n" +
                                                "if(lightRadius > 0.0){\n" +
                                                // todo effect is much more visible in the diffuse part
                                                // it's fine for small increased, but we wouldn't really use them...
                                                // should be more visible in the specular case...
                                                // in the ideal case, we move the light such that it best aligns the sphere...
                                                "   vec3 idealLightDirWS = normalize(reflect(finalPosition, finalNormal));\n" +
                                                "   lightDirWS = normalize(mix(lightDirWS, idealLightDirWS, clamp(lightRadius/(length(lightPosition-finalPosition)),0,1)));\n" +
                                                "}\n" +
                                                "NdotL = dot(lightDirWS, finalNormal);\n" +
                                                // shadow maps
                                                // shadows can be in every direction -> use cubemaps
                                                "if(shadowMapIdx0 < shadowMapIdx1){\n" +
                                                "   float near = data2.a;\n" +
                                                "   float maxAbsComponent = max(max(abs(dir.x),abs(dir.y)),abs(dir.z));\n" +
                                                "   float depthFromShader = near/maxAbsComponent;\n" +
                                                // todo how can we get rid of this (1,-1,-1), what rotation is missing?
                                                "   float depthFromTex = texture_array_depth_shadowMapCubic(shadowMapIdx0, dir*vec3(+1,-1,-1), depthFromShader);\n" +
                                                // "   float val = texture_array_shadowMapCubic(shadowMapIdx0, dir*vec3(+1,-1,-1)).r;\n" +
                                                // "   effectiveDiffuse = lightColor * vec3(vec2(val),depthFromShader);\n" + // nice for debugging
                                                //"   effectiveDiffuse = lightColor * (dir*.5+.5);\n" +
                                                "   lightColor *= 1.0 - depthFromTex;\n" +
                                                "}\n" +
                                                "effectiveDiffuse = lightColor * ${it.falloff};\n" +
                                                "dir *= 0.2;\n" + // less falloff by a factor of 5,
                                                // because specular light is more directed and therefore reached farther
                                                "effectiveSpecular = lightColor * ${it.falloff};\n"
                                    }
                                    LightType.SPOT -> {
                                        "" +
                                                "if(dir.z >= 0.0) continue;\n" + // backside
                                                "lightPosition = data1.rgb;\n" +
                                                "lightDirWS = normalize(lightPosition - finalPosition);\n" +
                                                "NdotL = dot(lightDirWS, finalNormal);\n" +
                                                "float coneAngle = data1.a;\n" +
                                                "vec2 shadowDir = dir.xy/(-dir.z * coneAngle);\n" +
                                                "float ringFalloff = dot(shadowDir,shadowDir);\n" +
                                                "if(ringFalloff > 1.0) continue;\n" + // outside of light
                                                // when we are close to the edge, we blend in
                                                "lightColor *= 1.0-ringFalloff;\n" +
                                                "if(shadowMapIdx0 < shadowMapIdx1){\n" +
                                                "   #define shadowMapPower data2.b\n" +
                                                "   vec2 nextDir = shadowDir * shadowMapPower;\n" +
                                                "   while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                                                "       shadowMapIdx0++;\n" +
                                                "       shadowDir = nextDir;\n" +
                                                "       nextDir *= shadowMapPower;\n" +
                                                "   }\n" +
                                                "   float near = data2.a;\n" +
                                                "   float depthFromShader = -near/dir.z;\n" +
                                                // do the shadow map function and compare
                                                "    float depthFromTex = texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy, depthFromShader);\n" +
                                                "    lightColor *= 1.0 - depthFromTex;\n" +
                                                "}\n" +
                                                "effectiveDiffuse = lightColor * ${it.falloff};\n" +
                                                "dir *= 0.2;\n" + // less falloff by a factor of 5,
                                                // because specular light is more directed and therefore reached farther
                                                "effectiveSpecular = lightColor * ${it.falloff};\n"
                                    }
                                } +
                                "break;\n"
                    } +
                    "           }\n" +
                    "           if(hasSpecular && dot(effectiveSpecular, vec3(NdotL)) > ${0.5 / 255.0}){\n" +
                    "               vec3 H = normalize(V + lightDirWS);\n" +
                    specularBRDFv2NoDivInlined2 +
                    "               specularLight += effectiveSpecular * computeSpecularBRDF;\n" +
                    "           }\n" +
                    // translucency; looks good and approximately correct
                    "           NdotL = mix(NdotL, 0.23, finalTranslucency);\n" +
                    // sheen is a fresnel effect, which adds light
                    // todo back side may not be dark
                    "           NdotL = NdotL + sheen;\n" +
                    "           if(NdotL > 0.0){\n" +
                    "               diffuseLight += effectiveDiffuse * min(NdotL, 1.0);\n" +
                    "               lightCount++;\n" +
                    "           }\n" +
                    "       }\n" +
                    specularBRDFv2NoDivInlined2End +
                    "   }\n" +
                    "   if(visualizeLightCount){\n" +
                    "       finalColor.r = lightCount * 0.125;\n" +
                    // reinhard tonemapping
                    "       finalColor = vec3(lightCount/(1.0+lightCount));\n" +
                    "   } else {\n" +
                    "       finalColor = diffuseColor * diffuseLight + specularColor * specularLight;\n" +
                    "       finalColor = finalColor * finalOcclusion + finalEmissive;\n" +
                    "       finalColor = finalColor/(1.0+finalColor);\n" +
                    // banding prevention
                    // -0.5, so we don't destroy blacks on OLEDs
                    "       finalColor -= random(uv) * ${1.0 / 255.0};\n" +
                    "   }\n"
            ).apply {
                val src = Scene.reinhardToneMapping +
                        Scene.noiseFunc
                functions.add(Function(src))
            }
        }
    }

    // todo if imported mesh has no materials, just create a sample material...

    // pbr rendering with a few fake lights (which have no falloff)
    val previewRenderer = object : Renderer("preview", false, ShaderPlus.DrawMode.COLOR) {

        val previewLights = listOf(
            // direction, strength
            Vector4f(-.5f, +1f, .5f, 5f),
            Vector4f(1f, 1f, 0f, 2f),
            Vector4f(0f, 0f, 1f, 1f)
        )

        override fun uploadDefaultUniforms(shader: Shader) {
            super.uploadDefaultUniforms(shader)
            GFX.check()
            shader.use()
            val uniform = shader["lightData"]
            if (uniform >= 0) {
                val tmp = ByteBuffer.allocateDirect(previewLights.size * 4 * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                tmp.position(0)
                for (data in previewLights) {
                    val f = length(data.x, data.y, data.z)
                    tmp.put(data.x / f)
                    tmp.put(data.y / f)
                    tmp.put(data.z / f)
                    tmp.put(data.w)
                }
                tmp.flip()
                GL20.glUniform4fv(uniform, tmp)
                tmp.limit(tmp.capacity()) // reset it
                GFX.check()
            }
        }

        override fun getPostProcessing(): ShaderStage {
            return ShaderStage(
                "previewRenderer", listOf(
                    Variable("vec4", "lightData", previewLights.size),
                    Variable("vec3", "finalColor", VariableMode.INOUT),
                    Variable("float", "finalAlpha", VariableMode.INOUT),
                    Variable("float", "finalRoughness"),
                    Variable("float", "finalMetallic"),
                    Variable("float", "finalSheen"),
                    Variable("vec3", "finalSheenNormal"),
                    Variable("vec4", "finalClearCoat"),
                    Variable("vec2", "finalClearCoatRoughMetallic"),
                    Variable("vec3", "finalNormal"),
                    Variable("vec3", "finalEmissive")
                ), "" +
                        // shared pbr data
                        "   vec3 V = normalize(-finalPosition);\n" +
                        // light calculations
                        "   float NdotV = abs(dot(finalNormal,V));\n" +
                        // fresnel for all fresnel based effects
                        "   float fresnel = 1.0 - NdotV, fresnel3 = pow(fresnel, 3.0);\n" +
                        "   if(finalClearCoat.w > 0.0){\n" +
                        // cheap clear coat effect
                        "       float clearCoatEffect = fresnel3 * finalClearCoat.w;\n" +
                        "       finalRoughness = mix(finalRoughness, finalClearCoatRoughMetallic.x, clearCoatEffect);\n" +
                        "       finalMetallic = mix(finalMetallic, finalClearCoatRoughMetallic.y, clearCoatEffect);\n" +
                        "       finalColor = mix(finalColor, finalClearCoat.rgb, clearCoatEffect);\n" +
                        "   }\n" +
                        // precalculate sheen
                        "   float sheenFresnel = 1.0 - abs(dot(finalSheenNormal,V));\n" +
                        "   float sheen = finalSheen * pow(sheenFresnel, 3.0);\n" +
                        // light calculation
                        "vec3 ambientLight = vec3(0.3);\n" +
                        "vec3 diffuseLight = ambientLight, specularLight = ambientLight;\n" +
                        "vec3 diffuseColor  = finalColor * (1.0 - finalMetallic);\n" +
                        "vec3 specularColor = finalColor * finalMetallic;\n" +
                        "bool hasSpecular = dot(specularColor, vec3(1.0)) > 0.0;\n" +
                        specularBRDFv2NoDivInlined2Start +
                        "for(int i=0;i<${previewLights.size};i++){\n" +
                        "   vec4 data = lightData[i];\n" +
                        "   vec3 lightDirection = data.xyz, lightColor = vec3(data.w);\n" +
                        "   float NdotL = dot(finalNormal, lightDirection);\n" +
                        "   if(NdotL > 0.0){\n" +
                        "       vec3 H = normalize(V + lightDirection);\n" +
                        "       if(hasSpecular){\n" +
                        specularBRDFv2NoDivInlined2 +
                        "           specularLight += lightColor * computeSpecularBRDF;\n" +
                        "       }\n" +
                        "       diffuseLight += lightColor * NdotL;\n" +
                        "   }\n" +
                        "}\n" +
                        specularBRDFv2NoDivInlined2End +
                        "finalColor = diffuseColor * diffuseLight + specularColor * specularLight;\n" +
                        "finalColor = finalColor * finalOcclusion + finalEmissive;\n" +
                        "finalColor = finalColor/(1.0+finalColor);\n" +
                        // a preview probably doesn't need anti-banding
                        // "finalColor -= random(uv) * ${1.0 / 255.0};\n" +
                        // make the border opaque, so we can see it better -> doesn't work somehow...
                        // "finalAlpha = clamp(finalAlpha + 10.0 * fresnel3, 0.0, 1.0);\n"
                        ""
            ).apply {
                val src = Scene.reinhardToneMapping +
                        Scene.noiseFunc
                functions.add(Function(src))
            }
        }
    }

    val simpleNormalRenderer = object : Renderer("simple-normal", false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage(
                "uiRenderer", listOf(
                    Variable("vec3", "finalColor", VariableMode.INOUT),
                    Variable("float", "finalAlpha", VariableMode.INOUT),
                    Variable("vec3", "finalNormal"),
                    Variable("vec3", "finalEmissive")
                ), "" +
                        "finalColor *= 0.6 - 0.4 * normalize(finalNormal).x;\n" +
                        "finalColor += finalEmissive;\n"
            )
        }
    }

    val attributeRenderers: List<Renderer> = DeferredLayerType.values()
        .run { toList().subList(0, kotlin.math.min(size, 9)) }.map {
            object : Renderer("attribute-$it", false, ShaderPlus.DrawMode.COLOR) {
                override fun getPostProcessing(): ShaderStage {
                    return ShaderStage(
                        "attribute", if (it == DeferredLayerType.COLOR) {
                            listOf(Variable("vec3", "finalColor", VariableMode.INOUT))
                        } else {
                            listOf(
                                Variable(DeferredSettingsV2.glslTypes[it.dimensions - 1], it.glslName, true),
                                Variable("vec3", "finalColor", false)
                            )
                        },
                        if (it == DeferredLayerType.COLOR) {
                            ""
                        } else {
                            "finalColor = ${
                                when (it.dimensions) {
                                    1 -> "vec3(${it.glslName}${it.map01})"
                                    2 -> "vec3(${it.glslName}${it.map01},1)"
                                    3 -> "(${it.glslName}${it.map01})"
                                    4 -> "(${it.glslName}${it.map01}).rgb"
                                    else -> ""
                                }
                            };\n"
                        }
                    )
                }
            }
        }

    val MAX_PLANAR_LIGHTS = 8
    val MAX_CUBEMAP_LIGHTS = 8

}