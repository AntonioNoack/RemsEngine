package me.anno.engine.ui.render

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
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

    // todo just like the gltf shader define all material properties
    override fun createFlatShader(postProcessing: ShaderStage?, instanced: Boolean, geoShader: GeoShader?): Shader {

        val base = ShaderBuilder(name, null)

        val attributes = mutableListOf(
            Variable("vec3", "coords"),
            Variable("vec2", "uvs"),
            Variable("vec3", "normals"),
            Variable("vec3", "tangents"),
            Variable("vec4", "colors")
        )

        attributes += if (instanced) {
            listOf(
                Variable("vec4", "instanceTrans0"),
                Variable("vec4", "instanceTrans1"),
                Variable("vec4", "instanceTrans2"),
                Variable("vec4", "instanceTint")
            )
        } else {
            // not supported for instanced rendering (at the moment),
            // because having 100 skeletons with all the same animation is probably rare, and could be solved differently
            listOf(
                Variable("vec4", "weights"),
                Variable("ivec4", "indices"),
            )
        }

        base.vertex.attributes += attributes

        val params = attributes + if (instanced) {
            listOf(
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
                Variable("vec4", "tint", false),
            )
        } else {
            listOf(
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
                Variable("vec4", "weight", false),
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
                                    "   mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2);\n" +
                                    "   normal = localTransform * vec4(normals, 0.0);\n" +
                                    "   tangent = localTransform * vec4(tangents, 0.0);\n" +
                                    "   finalPosition = localTransform * vec4(coords, 1.0);\n" +
                                    "   tint = instanceTint;\n"
                        } else {
                            "" +
                                    "   if(hasAnimation){\n" +
                                    "       mat4x3 jointMat;\n" +
                                    "       jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                                    "       jointMat += jointTransforms[indices.y] * weights.y;\n" +
                                    "       jointMat += jointTransforms[indices.z] * weights.z;\n" +
                                    "       jointMat += jointTransforms[indices.w] * weights.w;\n" +
                                    "       localPosition = jointMat * vec4(coords, 1.0);\n" +
                                    "       normal = jointMat * vec4(normals, 0.0);\n" +
                                    "       tangent = jointMat * vec4(tangents, 0.0);\n" +
                                    "   } else {\n" +
                                    "       localPosition = coords;\n" +
                                    "       normal = normals;\n" +
                                    "       tangent = tangents;\n" +
                                    "   }\n" +
                                    "   normal = localTransform * vec4(normal, 0.0);\n" +
                                    "   tangent = localTransform * vec4(tangent, 0.0);\n" +
                                    "   finalPosition = localTransform * vec4(localPosition, 1.0);\n" +
                                    "   weight = weights;\n"
                        } +
                        // normal only needs to be normalized, if we show the normal
                        // todo only activate on viewing it...
                        "   normal = normalize(normal);\n" + // here? nah ^^
                        "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                        "   uv = uvs;\n" +
                        "   vertexColor = hasVertexColors ? colors : vec4(1);\n" +
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
                    // outputs
                    Variable("vec3", "finalColor", false),
                    Variable("float", "finalAlpha", false),
                    Variable("vec3", "finalPosition", false),
                    Variable("vec3", "finalNormal", false),
                    Variable("vec3", "finalTangent", false),
                    Variable("vec3", "finalBitangent", false),
                    Variable("vec3", "finalEmissive", false),
                    Variable("float", "finalMetallic", false),
                    Variable("float", "finalRoughness", false),
                    Variable("float", "finalOcclusion", false),
                    Variable("vec3", "finalSheenNormal", false),
                    // just passed from uniforms
                    Variable("float", "finalTranslucency", VariableMode.INOUT),
                    Variable("float", "finalSheen", VariableMode.INOUT),
                    Variable("vec4", "finalClearCoat", VariableMode.INOUT),
                    Variable("vec2", "finalClearCoatRoughMetallic", VariableMode.INOUT),
                    /* Variable("float", "translucency"),
                     Variable("float", "sheen"),
                     Variable("vec4", "clearCoat"),
                     Variable("vec2", "clearCoatRoughMetallic"),*/
                ),
                "" +
                        // step by step define all material properties
                        "vec4 color = vec4(vertexColor.rgb, 1) * diffuseBase * texture(diffuseMap, uv);\n" +
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
                        // clear coat normal calculation
                        "if(finalSheen * normalStrength.y > 0.0){\n" +
                        "   vec3 normalFromTex = texture(sheenNormalMap, uv).rgb * 2.0 - 1.0;\n" +
                        "        normalFromTex = tbn * normalFromTex;\n" +
                        // original or transformed "finalNormal"? mmh...
                        // transformed probably is better
                        "   finalSheenNormal = mix(finalNormal, normalFromTex, normalStrength.y);\n" +
                        "} else finalSheenNormal = finalNormal;\n"

            )
        )

        // <3, this is crazily easy
        base.addFragment(postProcessing)
        base.addFragment(ShaderPlus.createShaderStage())

        GFX.check()
        val shader = base.create()
        shader.glslVersion = glslVersion
        shader.setTextureIndices(textures)
        shader.ignoreUniformWarnings(ignoredUniforms)
        shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v4("tint", 1f, 1f, 1f, 1f)
        GFX.check()
        return shader

    }


}