package me.anno.engine.ui.render

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GeoShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.assimp.AnimGameItem.Companion.maxBones

class ECSMeshShader(name: String) : BaseShader(name, "", emptyList(), "") {

    override fun createDepthShader(instanced: Boolean): Shader {

        val base = ShaderBuilder(name, null)
        base.ignored += Array(8) { "shadowMapPlanar$it" }
        base.ignored += Array(8) { "shadowMapCubic$it" }

        val params = if (instanced) {
            listOf(
                // base
                Variable("vec3", "coords", VariableMode.ATTR),
                // other
                Variable("vec4", "instanceTrans0", VariableMode.ATTR),
                Variable("vec4", "instanceTrans1", VariableMode.ATTR),
                Variable("vec4", "instanceTrans2", VariableMode.ATTR),
                // Variable("vec4", "instanceTint", VariableMode.ATTR),
                // uniforms
                Variable("mat4", "transform")
            )
        } else {
            // weights & indices are not supported for instanced rendering at the moment,
            // because having 100 skeletons with all the same animation is probably rare, and could be solved differently
            listOf(
                // base
                Variable("vec3", "coords", VariableMode.ATTR),
                // other
                Variable("vec4", "weights", VariableMode.ATTR),
                Variable("ivec4", "indices", VariableMode.ATTR),
                // uniforms
                Variable("mat4", "transform"),
                // not required for the instanced rendering, because this is instance specific
                Variable("mat4x3", "jointTransforms", maxBones),
                Variable("mat4x3", "localTransform"),
                Variable("bool", "hasAnimation")
            )
        }

        base.addVertex(
            ShaderStage(
                "vertex",
                params,
                "" +
                        if (instanced) {
                            "" +
                                    "mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2);\n" +
                                    "vec3 finalPosition = localTransform * vec4(coords, 1.0);\n"
                        } else {
                            "" +
                                    "vec3 localPosition;\n" +
                                    "if(hasAnimation){\n" +
                                    "    mat4x3 jointMat;\n" +
                                    "    jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                                    "    jointMat += jointTransforms[indices.y] * weights.y;\n" +
                                    "    jointMat += jointTransforms[indices.z] * weights.z;\n" +
                                    "    jointMat += jointTransforms[indices.w] * weights.w;\n" +
                                    "    localPosition = jointMat * vec4(coords, 1.0);\n" +
                                    "} else {\n" +
                                    "    localPosition = coords;\n" +
                                    "}\n" +
                                    "vec3 finalPosition = localTransform * vec4(localPosition, 1.0);\n"
                        } +
                        "gl_Position = transform * vec4(finalPosition, 1.0);\n"
            )
        )

        // for the future, we could respect transparency from textures :)
        // base.addFragment(ShaderStage("material", emptyList(), ""))

        GFX.check()
        val shader = base.create()
        shader.glslVersion = glslVersion
        GFX.check()
        return shader

    }

    // just like the gltf pbr shader define all material properties
    private fun createBase(instanced: Boolean): ShaderBuilder {

        val base = ShaderBuilder(name, null)
        base.ignored += Array(8) { "shadowMapPlanar$it" }
        base.ignored += Array(8) { "shadowMapCubic$it" }

        val baseAttributes = listOf(
            Variable("vec3", "coords", VariableMode.ATTR),
            Variable("vec2", "uvs", VariableMode.ATTR),
            Variable("vec3", "normals", VariableMode.ATTR),
            Variable("vec3", "tangents", VariableMode.ATTR),
            Variable("vec4", "colors", VariableMode.ATTR)
        )

        val params = baseAttributes + if (instanced) {
            listOf(
                // attributes
                Variable("vec4", "instanceTrans0", VariableMode.ATTR),
                Variable("vec4", "instanceTrans1", VariableMode.ATTR),
                Variable("vec4", "instanceTrans2", VariableMode.ATTR),
                Variable("vec4", "instanceTint", VariableMode.ATTR),
                // uniforms
                Variable("mat4", "transform"),
                Variable("bool", "hasVertexColors"),
                // outputs
                Variable("float", "zDistance", false),
                Variable("vec2", "uv", false),
                Variable("vec3", "normal", false),
                Variable("vec3", "tangent", false),
                Variable("vec4", "vertexColor", false),
                Variable("vec3", "localPosition", false),
                Variable("vec3", "finalPosition", false),
                Variable("vec4", "tint", VariableMode.OUT),
            )
        } else {
            listOf(
                // attributes
                Variable("vec4", "weights", VariableMode.ATTR),
                Variable("ivec4", "indices", VariableMode.ATTR),
                // uniforms
                Variable("mat4", "transform"),
                Variable("bool", "hasVertexColors"),
                // not required for the instanced rendering, because this is instance specific
                Variable("mat4x3", "jointTransforms", maxBones),
                Variable("mat4x3", "localTransform"),
                Variable("bool", "hasAnimation"),
                // outputs
                Variable("float", "zDistance", false),
                Variable("vec2", "uv", false),
                Variable("vec3", "normal", false),
                Variable("vec3", "tangent", false),
                Variable("vec4", "vertexColor", false),
                // Variable("vec4", "weight", false),
                Variable("vec3", "localPosition", false),
                Variable("vec3", "finalPosition", false),
            )
        }

        base.addVertex(
            ShaderStage(
                "vertex",
                params,
                "" +
                        if (instanced) {
                            "" +
                                    "mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2);\n" +
                                    "normal = localTransform * vec4(normals, 0.0);\n" +
                                    "tangent = localTransform * vec4(tangents, 0.0);\n" +
                                    "finalPosition = localTransform * vec4(coords, 1.0);\n" +
                                    "tint = instanceTint;\n"
                        } else {
                            "" +
                                    "if(hasAnimation){\n" +
                                    "    mat4x3 jointMat;\n" +
                                    "    jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                                    "    jointMat += jointTransforms[indices.y] * weights.y;\n" +
                                    "    jointMat += jointTransforms[indices.z] * weights.z;\n" +
                                    "    jointMat += jointTransforms[indices.w] * weights.w;\n" +
                                    "    localPosition = jointMat * vec4(coords, 1.0);\n" +
                                    "    normal = jointMat * vec4(normals, 0.0);\n" +
                                    "    tangent = jointMat * vec4(tangents, 0.0);\n" +
                                    "} else {\n" +
                                    "    localPosition = coords;\n" +
                                    "    normal = normals;\n" +
                                    "    tangent = tangents;\n" +
                                    "}\n" +
                                    "normal = localTransform * vec4(normal, 0.0);\n" +
                                    "tangent = localTransform * vec4(tangent, 0.0);\n" +
                                    "finalPosition = localTransform * vec4(localPosition, 1.0);\n" +
                                    // "weight = weights;\n" +
                                    ""
                        } +
                        // normal only needs to be normalized, if we show the normal
                        // todo only activate on viewing it...
                        "normal = normalize(normal);\n" + // here? nah ^^
                        "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                        "uv = uvs;\n" +
                        "vertexColor = hasVertexColors ? colors : vec4(1);\n" +
                        ShaderLib.positionPostProcessing
            )
        )

        base.addFragment(
            ShaderStage(
                "material", listOf(
                    // input textures
                    Variable("sampler2D", "diffuseMap"),
                    Variable("sampler2D", "normalMap"),
                    Variable("sampler2D", "emissiveMap"),
                    Variable("sampler2D", "roughnessMap"),
                    Variable("sampler2D", "metallicMap"),
                    Variable("sampler2D", "occlusionMap"),
                    Variable("sampler2D", "sheenNormalMap"),
                    // input varyings
                    Variable("vec2", "uv"),
                    Variable("vec3", "normal"),
                    Variable("vec3", "tangent"),
                    Variable("vec4", "vertexColor"),
                    Variable("vec3", "finalPosition"),
                    Variable("vec2", "normalStrength"),
                    Variable("vec2", "roughnessMinMax"),
                    Variable("vec2", "metallicMinMax"),
                    Variable("float", "occlusionStrength"),
                    Variable("vec4", "diffuseBase"),
                    Variable("vec3", "emissiveBase"),
                    Variable("float", "sheen"),
                    // outputs
                    Variable("vec3", "finalColor", VariableMode.OUT),
                    Variable("float", "finalAlpha", VariableMode.OUT),
                    Variable("vec3", "finalPosition", VariableMode.OUT),
                    Variable("vec3", "finalNormal", VariableMode.OUT),
                    Variable("vec3", "finalTangent", VariableMode.OUT),
                    Variable("vec3", "finalBitangent", VariableMode.OUT),
                    Variable("vec3", "finalEmissive", VariableMode.OUT),
                    Variable("float", "finalMetallic", VariableMode.OUT),
                    Variable("float", "finalRoughness", VariableMode.OUT),
                    Variable("float", "finalOcclusion", VariableMode.OUT),
                    Variable("float", "finalSheen", VariableMode.OUT),
                    // just passed from uniforms
                    Variable("float", "finalTranslucency", VariableMode.INOUT),
                    Variable("vec4", "finalClearCoat", VariableMode.INOUT),
                    Variable("vec2", "finalClearCoatRoughMetallic", VariableMode.INOUT),
                    // for reflections;
                    // we could support multiple
                    Variable("bool", "hasReflectionPlane"),
                    Variable("vec3", "reflectionPlaneNormal"),
                    Variable("sampler2D", "reflectionPlane"),
                    Variable("vec4", "reflectionCullingPlane"),
                    /* Variable("float", "translucency"),
                     Variable("float", "sheen"),
                     Variable("vec4", "clearCoat"),
                     Variable("vec2", "clearCoatRoughMetallic"),*/
                ),
                "" +
                        "" +
                        "if(dot(vec4(finalPosition,1),reflectionCullingPlane) < 0) discard;\n" +

                        // step by step define all material properties
                        "vec4 color = vec4(vertexColor.rgb, 1) * diffuseBase * texture(diffuseMap, uv);\n" +
                        "if(color.a < ${1f/255f}) discard;\n" +
                        "finalColor = color.rgb;\n" +
                        "finalAlpha = color.a;\n" +
                        // "   vec3 finalNormal = normal;\n" +
                        "finalTangent   = normalize(tangent);\n" + // for debugging
                        "finalNormal    = normalize(normal);\n" +
                        "finalBitangent = normalize(cross(finalNormal, finalTangent));\n" +
                        // bitangent: checked, correct transform
                        // can be checked with a lot of rotated objects in all orientations,
                        // and a shader with light from top/bottom
                        "mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                        "if(normalStrength.x > 0.0){\n" +
                        "   vec3 normalFromTex = texture(normalMap, uv).rgb * 2.0 - 1.0;\n" +
                        "        normalFromTex = tbn * normalFromTex;\n" +
                        "   finalNormal = mix(finalNormal, normalFromTex, normalStrength.x);\n" +
                        "}\n" +
                        "finalEmissive  = texture(emissiveMap, uv).rgb * emissiveBase;\n" +
                        "finalOcclusion = 1.0 - (1.0 - texture(occlusionMap, uv).r) * occlusionStrength;\n" +
                        "finalMetallic  = mix(metallicMinMax.x,  metallicMinMax.y,  texture(metallicMap,  uv).r);\n" +
                        "finalRoughness = mix(roughnessMinMax.x, roughnessMinMax.y, texture(roughnessMap, uv).r);\n" +

                        // reflections
                        // use roughness instead?
                        // "   if(finalMetallic > 0.0) finalColor = mix(finalColor, texture(reflectionPlane,uv).rgb, finalMetallic);\n" +
                        "if(hasReflectionPlane){\n" +
                        "   float effect = dot(reflectionPlaneNormal,finalNormal) * (1-finalRoughness);\n" +
                        "   float factor = clamp((effect-.3)/.7,0,1);\n" +
                        "   if(factor > 0){\n" +
                        "       vec3 newColor = vec3(0);\n" +
                        "       vec3 newEmissive = finalColor * texelFetch(reflectionPlane,ivec2(gl_FragCoord.xy),0).rgb;\n" +
                        // also multiply for mirror color <3
                        "       finalEmissive = mix(finalEmissive, newEmissive, factor);\n" +
                        // "       finalEmissive /= (1-finalEmissive);\n" + // only required, if tone mapping is applied
                        "       finalColor = mix(finalColor, newColor, factor);\n" +
                        // "       finalRoughness = 0;\n" +
                        // "       finalMetallic = 0;\n" +
                        "   }\n" +
                        "};\n" +

                        // sheen calculation
                        "vec3 V0 = normalize(-finalPosition);\n" +
                        "if(sheen > 0){\n" +
                        "   vec3 sheenNormal = finalNormal;\n" +
                        "   if(finalSheen * normalStrength.y > 0.0){\n" +
                        "      vec3 normalFromTex = texture(sheenNormalMap, uv).rgb * 2.0 - 1.0;\n" +
                        "           normalFromTex = tbn * normalFromTex;\n" +
                        // original or transformed "finalNormal"? mmh...
                        // transformed probably is better
                        "      sheenNormal = mix(finalNormal, normalFromTex, normalStrength.y);\n" +
                        "   }\n" +
                        // calculate sheen
                        "   float sheenFresnel = 1.0 - abs(dot(sheenNormal,V0));\n" +
                        "   finalSheen = sheen * pow(sheenFresnel, 3.0);\n" +
                        "} else finalSheen = 0;\n" +

                        "if(finalClearCoat.w > 0){\n" +
                        // cheap clear coat effect
                        "   float fresnel = 1.0 - abs(dot(finalNormal,V0));\n" +
                        "   float clearCoatEffect = pow(fresnel, 3.0) * finalClearCoat.w;\n" +
                        "   finalRoughness = mix(finalRoughness, finalClearCoatRoughMetallic.x, clearCoatEffect);\n" +
                        "   finalMetallic = mix(finalMetallic, finalClearCoatRoughMetallic.y, clearCoatEffect);\n" +
                        "   finalColor = mix(finalColor, finalClearCoat.rgb, clearCoatEffect);\n" +
                        "}\n"

            )
        )

        return base

    }

    override fun createFlatShader(postProcessing: ShaderStage?, instanced: Boolean, geoShader: GeoShader?): Shader {

        val base = createBase(instanced)

        // <3, this is crazily easy
        base.addFragment(postProcessing)
        base.addFragment(ShaderPlus.createShaderStage())

        val shader = base.create()
        finish(shader)
        return shader

    }

    override fun createDeferredShader(
        deferred: DeferredSettingsV2,
        isInstanced: Boolean,
        geoShader: GeoShader?
    ): Shader {

        val base = createBase(isInstanced)
        base.outputs = deferred

        // build & finish
        val shader = base.create()
        finish(shader)
        return shader

    }

}