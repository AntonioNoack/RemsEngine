package me.anno.engine.ui.render

import me.anno.ecs.components.light.LightType
import me.anno.engine.pbr.PBRLibraryGLTF
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
import me.anno.utils.Maths.length
import org.joml.Vector4f
import org.lwjgl.opengl.GL20
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Renderers {

    val overdrawRenderer = object : Renderer(true, ShaderPlus.DrawMode.COLOR) {
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

    val cheapRenderer = object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage(
                "cheap", listOf(
                    Variable("vec3", "finalColor", false)
                ), "finalColor = vec3(0.5);"
            )
        }
    }

    val baseRenderer = object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage("pbr", listOf(
                Variable("vec3", "ambientLight"),
                Variable("int", "numberOfLights"),
                Variable("mat4x3", "invLightMatrices", RenderView.MAX_LIGHTS),
                Variable("vec4", "lightData0", RenderView.MAX_LIGHTS),
                Variable("vec4", "lightData1", RenderView.MAX_LIGHTS),
                Variable("int", "visualizeLightCount"),
                Variable("vec3", "finalEmissive"),
                Variable("float", "finalMetallic"),
                Variable("float", "finalRoughness"),
                Variable("float", "finalOcclusion"),
                Variable("float", "finalSheen"),
                Variable("vec3", "finalSheenCoatNormal"),
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
                    // todo use sheen normal
                    "   float sheen = finalSheen * fresnel3;\n" +
                    "   vec3 diffuseColor = finalColor * (1.0 - finalMetallic);\n" +
                    "   vec3 specularColor = finalColor * finalMetallic;\n" +
                    "   bool hasSpecular = dot(specularColor,vec3(1.0)) > 0.001;\n" +
                    "   bool hasDiffuse = dot(diffuseColor,vec3(1.0)) > 0.001;\n" +
                    "   if(hasDiffuse || hasSpecular){\n" +
                    PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start +
                    "       for(int i=0;i<numberOfLights;i++){\n" +
                    "           mat4x3 WStoLightSpace = invLightMatrices[i];\n" +
                    "           vec3 dir = invLightMatrices[i] * vec4(finalPosition,1.0);\n" + // local coordinates for falloff
                    // "       if(!hasSpecular && dot(dir,dir) >= 1.0) continue;\n" +
                    "           vec4 data0 = lightData0[i];\n" +
                    "           vec4 data1 = lightData1[i];\n" +
                    "           vec3 lightColor = data0.rgb;\n" +
                    "           int lightType = int(data0.a);\n" +
                    "           vec3 lightPosition, lightDirWS, localNormal, effectiveLightColor, effectiveSpecular, effectiveDiffuse;\n" +
                    "           localNormal = normalize(WStoLightSpace * vec4(finalNormal,0.0));\n" +
                    "           float NdotL;\n" + // normal dot light
                    // local coordinates of the point in the light "cone"
                    "           switch(lightType){\n" +
                    LightType.values().joinToString("") {
                        "case ${it.id}:\n" +
                                when (it) {
                                    LightType.DIRECTIONAL -> {
                                        "" +
                                                "NdotL = localNormal.z;\n" + // dot(lightDirWS, globalNormal) = dot(lightDirLS, localNormal)
                                                // inv(W->L) * vec4(0,0,1,0) =
                                                // transpose(m3x3(W->L)) * vec3(0,0,1)
                                                "lightDirWS = normalize(vec3(WStoLightSpace[0][2],WStoLightSpace[1][2],WStoLightSpace[2][2]));\n" +
                                                "effectiveDiffuse = lightColor;\n" +
                                                "effectiveSpecular = lightColor;\n"
                                    }
                                    LightType.POINT_LIGHT -> {
                                        "" +
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
                                                "effectiveDiffuse = lightColor * ${it.falloff};\n" +
                                                "dir *= 0.2;\n" + // less falloff by a factor of 5,
                                                // because specular light is more directed and therefore reached farther
                                                "effectiveSpecular = lightColor * ${it.falloff};\n"
                                    }
                                    LightType.SPOT_LIGHT -> {
                                        "" +
                                                "lightPosition = data1.rgb;\n" +
                                                "lightDirWS = normalize(lightPosition - finalPosition);\n" +
                                                "NdotL = dot(lightDirWS, finalNormal);\n" +
                                                "float coneAngle = data1.a;\n" +
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
                    PBRLibraryGLTF.specularBRDFv2NoDivInlined2 +
                    "               specularLight += effectiveSpecular * computeSpecularBRDF;\n" +
                    "           }\n" +
                    // translucency; looks good and approximately correct
                    "           NdotL = mix(NdotL, 0.23, finalTranslucency);\n" +
                    // sheen is a fresnel effect, which adds light
                    "           NdotL = NdotL + sheen;\n" +
                    "           if(NdotL > 0.0){\n" +
                    "               diffuseLight += effectiveDiffuse * min(NdotL, 1.0);\n" +
                    "               lightCount++;\n" +
                    "           }\n" +
                    "       }\n" +
                    PBRLibraryGLTF.specularBRDFv2NoDivInlined2End +
                    "   }\n" +
                    "   finalColor = diffuseColor * diffuseLight + specularColor * specularLight;\n" +
                    "   finalColor = finalColor * finalOcclusion + finalEmissive;\n" +
                    "   finalColor = reinhard(visualizeLightCount > 0 ? vec3(lightCount * 0.125) : finalColor);\n" +
                    "   " +
                    // banding prevention
                    // -0.5, so we don't destroy blacks on OLEDs
                    "   finalColor -= random(uv) * ${1.0 / 255.0};\n"// + "}"
            ).apply {
                val src = Scene.reinhardToneMapping +
                        Scene.noiseFunc
                functions.add(Function(src))
            }
        }
    }

    // todo if imported mesh has no materials, just create a sample material...

    // pbr rendering with a few fake lights (which have no falloff)
    val previewRenderer = object : Renderer(false, ShaderPlus.DrawMode.COLOR) {

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
                    Variable("vec3", "finalNormal"),
                    Variable("vec3", "finalEmissive")
                ), "" +
                        "vec3 ambientLight = vec3(0.1);\n" +
                        "vec3 diffuseLight = ambientLight, specularLight = ambientLight;\n" +
                        "vec3 diffuseColor = finalColor * (1.0 - finalMetallic);\n" +
                        "vec3 specularColor = finalColor * finalMetallic;\n" +
                        "vec3 V = normalize(-finalPosition);\n" +
                        "bool hasSpecular = dot(specularColor, vec3(1.0)) > 0.0;\n" +
                        "for(int i=0;i<${previewLights.size};i++){\n" +
                        "   vec4 data = lightData[i];\n" +
                        "   vec3 lightDirection = data.xyz, lightColor = vec3(data.w);\n" +
                        "   float NdotL = dot(finalNormal, lightDirection);\n" +
                        "   if(NdotL > 0.0){\n" +
                        "       vec3 H = normalize(V + lightDirection);\n" +
                        "       if(hasSpecular) specularLight += lightColor * computeSpecularBRDF(\n" +
                        "           specularColor, finalRoughness, V,\n" +
                        "           finalNormal, NdotL, H\n" +
                        "       );\n" +
                        "       diffuseLight  += lightColor * NdotL;\n" +
                        "   }\n" +
                        "}\n" +
                        "finalColor = reinhard(diffuseColor * diffuseLight + specularColor * specularLight);\n" +
                        // "finalColor *= 0.6 - 0.4 * normalize(finalNormal).x;\n" +
                        "finalColor += finalEmissive;\n" +
                        "finalColor -= random(uv) * ${1.0 / 255.0};\n"
            ).apply {
                val src = PBRLibraryGLTF.specularBRDFv2NoDivInlined +
                        Scene.reinhardToneMapping +
                        Scene.noiseFunc
                functions.add(Function(src))
            }
        }
    }

    val uiRenderer = object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage(
                "uiRenderer", listOf(
                    Variable("vec3", "finalColor", VariableMode.INOUT),
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
            object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
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


}