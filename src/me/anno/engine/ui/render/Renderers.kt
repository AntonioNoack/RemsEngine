package me.anno.engine.ui.render

import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
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
import me.anno.utils.pooling.ByteBufferPool
import org.joml.Vector4f
import org.lwjgl.opengl.GL20

object Renderers {

    // and banding prevention
    val toneMapping =
        "vec3 toneMapping(vec3 color){ return (color)/(1.0+color) - random(gl_FragCoord.xy) * ${1f / 255f}; }\n"

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
                // rendering
                Variable("bool", "applyToneMapping"),
                // light data
                Variable("vec3", "ambientLight"),
                Variable("int", "numberOfLights"),
                Variable("mat4x3", "invLightMatrices", RenderView.MAX_FORWARD_LIGHTS),
                Variable("vec4", "lightData0", RenderView.MAX_FORWARD_LIGHTS),
                Variable("vec4", "lightData1", RenderView.MAX_FORWARD_LIGHTS),
                Variable("vec4", "shadowData", RenderView.MAX_FORWARD_LIGHTS),
                // light maps for shadows
                // - spot lights, directional lights
                Variable("sampler2D", "shadowMapPlanar", MAX_PLANAR_LIGHTS),
                // - point lights
                Variable("samplerCube", "shadowMapCubic", MAX_CUBEMAP_LIGHTS),
                // reflection plane for rivers or perfect mirrors
                Variable("bool", "hasReflectionPlane"),
                Variable("sampler2D", "reflectionPlane"),
                // reflection cubemap or irradiance map
                Variable("samplerCube", "reflectionMap"),
                // material properties
                Variable("vec3", "finalEmissive"),
                Variable("float", "finalMetallic"),
                Variable("float", "finalRoughness"),
                Variable("float", "finalOcclusion"),
                Variable("float", "finalSheen"),
                // Variable("vec3", "finalSheenNormal"),
                // Variable("vec4", "finalClearCoat"),
                // Variable("vec2", "finalClearCoatRoughMetallic"),
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
                    "   float NdotV = abs(dot(finalNormal,V));\n" +
                    "   vec3 diffuseColor = finalColor * (1.0 - finalMetallic);\n" +
                    "   vec3 specularColor = finalColor * finalMetallic;\n" +
                    "   vec3 diffuseLight = ambientLight, specularLight = vec3(0);\n" +
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
                    "           vec3 lightPosition, lightDirWS, localNormal, effectiveSpecular, effectiveDiffuse;\n" +
                    "           localNormal = normalize(WStoLightSpace * vec4(finalNormal,0.0));\n" +
                    "           float NdotL;\n" + // normal dot light
                    "           int shadowMapIdx0 = int(data2.r);\n" +
                    "           int shadowMapIdx1 = int(data2.g);\n" +
                    // local coordinates of the point in the light "cone"
                    "           switch(lightType){\n" +
                    LightType.values().joinToString("") {
                        val core = when (it) {
                            LightType.DIRECTIONAL -> DirectionalLight
                                .getShaderCode("continue", true)
                            LightType.POINT -> PointLight
                                .getShaderCode("continue", true, hasLightRadius = true)
                            LightType.SPOT -> SpotLight
                                .getShaderCode("continue", true)
                        }
                        "case ${it.id}:\n${core}break;\n"
                    } +
                    "           }\n" +
                    "           if(hasSpecular && dot(effectiveSpecular, vec3(NdotL)) > ${0.5 / 255.0}){\n" +
                    "               vec3 H = normalize(V + lightDirWS);\n" +
                    specularBRDFv2NoDivInlined2 +
                    "               specularLight += effectiveSpecular * computeSpecularBRDF;\n" +
                    "           }\n" +
                    // translucency; looks good and approximately correct
                    // sheen is a fresnel effect, which adds light
                    "           NdotL = mix(NdotL, 0.23, finalTranslucency) + finalSheen;\n" +
                    "           if(NdotL > 0.0){\n" +
                    "               diffuseLight += effectiveDiffuse * min(NdotL, 1.0);\n" +
                    "           }\n" +
                    "       }\n" +
                    specularBRDFv2NoDivInlined2End +
                    "   }\n" +
                    "   finalColor = diffuseColor * diffuseLight + specularLight;\n" +
                    "   finalColor = finalColor * finalOcclusion + finalEmissive;\n" +
                    "   if(applyToneMapping){\n" +
                    "       finalColor = toneMapping(finalColor);\n" +
                    "   }\n"
            ).apply {
                val src = Scene.noiseFunc + toneMapping
                functions.add(Function(src))
            }
        }
    }

    val frontBackRenderer = object : Renderer("front-back", true, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): ShaderStage {
            return ShaderStage(
                "front-back", listOf(
                    Variable("vec3", "finalPosition"),
                    Variable("vec3", "finalNormal"),
                    Variable("vec3", "finalColor", VariableMode.INOUT),
                ), "" +
                        "finalColor = dot(finalNormal,finalPosition)>0.0 ? vec3(1,0,0) : vec3(0,.3,1);\n" +
                        "finalColor *= finalNormal.x * 0.4 + 0.6;\n" // some simple shading
            )
        }
    }

    // pbr rendering with a few fake lights (which have no falloff)
    val previewRenderer = object : Renderer("preview", false, ShaderPlus.DrawMode.COLOR) {

        val previewLights = listOf(
            // direction, strength
            Vector4f(-.5f, +1f, .5f, 5f),
            Vector4f(1f, 1f, 0f, 2f),
            Vector4f(0f, 0f, 1f, 1f)
        )

        val tmpDefaultUniforms = ByteBufferPool
            .allocateDirect(previewLights.size * 4 * 4)
            .asFloatBuffer()

        override fun uploadDefaultUniforms(shader: Shader) {
            super.uploadDefaultUniforms(shader)
            GFX.check()
            shader.use()
            val uniform = shader["lightData"]
            if (uniform >= 0) {
                tmpDefaultUniforms.position(0)
                for (data in previewLights) {
                    val f = length(data.x, data.y, data.z)
                    tmpDefaultUniforms.put(data.x / f)
                    tmpDefaultUniforms.put(data.y / f)
                    tmpDefaultUniforms.put(data.z / f)
                    tmpDefaultUniforms.put(data.w)
                }
                tmpDefaultUniforms.flip()
                GL20.glUniform4fv(uniform, tmpDefaultUniforms)
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
                        "vec3 ambientLight = vec3(0.1);\n" +
                        "vec3 diffuseLight = ambientLight, specularLight = vec3(0);\n" +
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
                        "finalColor = diffuseColor * diffuseLight + specularLight;\n" +
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

    val attributeRenderers: Map<DeferredLayerType, Renderer> = DeferredLayerType.values()
        .associateWith { type ->
            object : Renderer(type.name, false, ShaderPlus.DrawMode.COLOR) {
                override fun getPostProcessing(): ShaderStage {
                    return ShaderStage(
                        type.name, if (type == DeferredLayerType.COLOR) {
                            listOf(Variable("vec3", "finalColor", VariableMode.INOUT))
                        } else {
                            listOf(
                                Variable(DeferredSettingsV2.glslTypes[type.dimensions - 1], type.glslName, true),
                                Variable("vec3", "finalColor", false)
                            )
                        },
                        if (type == DeferredLayerType.COLOR) {
                            ""
                        } else {
                            "finalColor = ${
                                when (type.dimensions) {
                                    1 -> "vec3(${type.glslName}${type.map01})"
                                    2 -> "vec3(${type.glslName}${type.map01},1)"
                                    3 -> "(${type.glslName}${type.map01})"
                                    4 -> "(${type.glslName}${type.map01}).rgb"
                                    else -> ""
                                }
                            };\n" + if (type.highDynamicRange) {
                                "finalColor = finalColor / (1+abs(finalColor));\n"
                            } else ""
                        }
                    )
                }
            }
        }

    val MAX_PLANAR_LIGHTS = 8
    val MAX_CUBEMAP_LIGHTS = 8

}