package me.anno.engine.ui.render

import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.*
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.assimp.AnimGameItem.Companion.maxBones

open class ECSMeshShader(name: String) : BaseShader(name, "", emptyList(), "") {

    open fun createBuilder(): ShaderBuilder {
        val builder = ShaderBuilder(name, null)
        builder.ignored += Array(8) { "shadowMapPlanar$it" }
        builder.ignored += Array(8) { "shadowMapCubic$it" }
        return builder
    }

    open fun createRandomIdStage(): ShaderStage {
        return ShaderStage(
            "randomId", listOf(
                Variable(GLSLType.V2I, "randomIdData", VariableMode.IN), // vertices/instance, random offset
                Variable(GLSLType.V1I, "randomId", VariableMode.OUT)
            ), "randomId = (gl_VertexID + gl_InstanceID * randomIdData.x + randomIdData.y) & 0xffff;\n"
        )
    }

    open fun createBase(instanced: Boolean, colors: Boolean): ShaderBuilder {
        val builder = createBuilder()
        builder.addVertex(createVertexStage(instanced, colors))
        builder.addVertex(createRandomIdStage())
        builder.addFragment(createFragmentStage(instanced))
        return builder
    }

    open fun createVertexAttributes(instanced: Boolean, colors: Boolean): ArrayList<Variable> {

        val attributes = ArrayList<Variable>(32)
        attributes += Variable(GLSLType.V3F, "coords", VariableMode.ATTR)

        if (colors) {
            attributes += Variable(GLSLType.V2F, "uvs", VariableMode.ATTR)
            attributes += Variable(GLSLType.V3F, "normals", VariableMode.ATTR)
            attributes += Variable(GLSLType.V3F, "tangents", VariableMode.ATTR)
            attributes += Variable(GLSLType.V4F, "colors", VariableMode.ATTR)
        }

        // uniforms
        attributes += Variable(GLSLType.M4x4, "transform")
        if (colors) {
            attributes += Variable(GLSLType.BOOL, "hasVertexColors")
        }

        // outputs
        attributes += Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
        attributes += Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
        attributes += Variable(GLSLType.V1F, "zDistance", VariableMode.OUT)

        if (colors) {
            attributes += Variable(GLSLType.V2F, "uv", false)
            attributes += Variable(GLSLType.V3F, "normal", false)
            attributes += Variable(GLSLType.V3F, "tangent", false)
            attributes += Variable(GLSLType.V4F, "vertexColor", false)
        }

        if (instanced) {
            attributes += Variable(GLSLType.V4F, "instanceTrans0", VariableMode.ATTR)
            attributes += Variable(GLSLType.V4F, "instanceTrans1", VariableMode.ATTR)
            attributes += Variable(GLSLType.V4F, "instanceTrans2", VariableMode.ATTR)
            if (colors) {
                attributes += Variable(GLSLType.V4F, "instanceTint", VariableMode.ATTR)
                attributes += Variable(GLSLType.V4F, "tint", VariableMode.OUT)
            }
        } else {
            // attributes
            attributes += Variable(GLSLType.V4F, "weights", VariableMode.ATTR)
            attributes += Variable(GLSLType.V4I, "indices", VariableMode.ATTR)
            // not required for the instanced rendering, because this is instance specific
            attributes += Variable(GLSLType.M4x3, "jointTransforms", maxBones)
            attributes += Variable(GLSLType.M4x3, "localTransform")
            attributes += Variable(GLSLType.BOOL, "hasAnimation")
            // Variable(GLSLType.V4F, "weight", false),
        }

        return attributes
    }

    open fun createVertexStage(instanced: Boolean, colors: Boolean): ShaderStage {

        val defines = "" +
                (if (instanced) "#define INSTANCED\n" else "") +
                (if (colors) "#define COLORS\n" else "")

        return ShaderStage(
            "vertex",
            createVertexAttributes(instanced, colors),
            "" +
                    defines +
                    "#ifdef INSTANCED\n" +
                    "   mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2);\n" +
                    "   localPosition = coords;\n" +
                    "   finalPosition = localTransform * vec4(coords, 1.0);\n" +
                    "   #ifdef COLORS\n" +
                    "       normal = localTransform * vec4(normals, 0.0);\n" +
                    "       tangent = localTransform * vec4(tangents, 0.0);\n" +
                    "       tint = instanceTint;\n" +
                    "   #endif\n" +
                    "#else\n" +
                    "   if(hasAnimation){\n" +
                    "       mat4x3 jointMat;\n" +
                    "       jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                    "       jointMat += jointTransforms[indices.y] * weights.y;\n" +
                    "       jointMat += jointTransforms[indices.z] * weights.z;\n" +
                    "       jointMat += jointTransforms[indices.w] * weights.w;\n" +
                    "       localPosition = jointMat * vec4(coords, 1.0);\n" +
                    "       #ifdef COLORS\n" +
                    "           normal = jointMat * vec4(normals, 0.0);\n" +
                    "           tangent = jointMat * vec4(tangents, 0.0);\n" +
                    "       #endif\n" +
                    "   } else {\n" +
                    "       localPosition = coords;\n" +
                    "       #ifdef COLORS\n" +
                    "           normal = normals;\n" +
                    "           tangent = tangents;\n" +
                    "       #endif\n" +
                    "   }\n" +
                    "   finalPosition = localTransform * vec4(localPosition, 1.0);\n" +
                    "   #ifdef COLORS\n" +
                    "       normal = localTransform * vec4(normal, 0.0);\n" +
                    "       tangent = localTransform * vec4(tangent, 0.0);\n" +
                    "   #endif\n" +
                    "#endif\n" +
                    // normal only needs to be normalized, if we show the normal
                    // todo only activate on viewing it...
                    "#ifdef COLORS\n" +
                    "   normal = normalize(normal);\n" + // here? nah ^^
                    "   vertexColor = hasVertexColors ? colors : vec4(1.0);\n" +
                    "   uv = uvs;\n" +
                    "#endif\n" +
                    "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    ShaderLib.positionPostProcessing
        )

    }

    // just like the gltf pbr shader define all material properties
    open fun createFragmentStage(instanced: Boolean): ShaderStage {

        val fragmentVariables = listOf(
            // input textures
            Variable(GLSLType.S2D, "diffuseMap"),
            Variable(GLSLType.S2D, "normalMap"),
            Variable(GLSLType.S2D, "emissiveMap"),
            Variable(GLSLType.S2D, "roughnessMap"),
            Variable(GLSLType.S2D, "metallicMap"),
            Variable(GLSLType.S2D, "occlusionMap"),
            Variable(GLSLType.S2D, "sheenNormalMap"),
            // input varyings
            Variable(GLSLType.V2F, "uv"),
            Variable(GLSLType.V3F, "normal"),
            Variable(GLSLType.V3F, "tangent"),
            Variable(GLSLType.V4F, "vertexColor"),
            Variable(GLSLType.V3F, "finalPosition"),
            Variable(GLSLType.V2F, "normalStrength"),
            Variable(GLSLType.V2F, "roughnessMinMax"),
            Variable(GLSLType.V2F, "metallicMinMax"),
            Variable(GLSLType.V1F, "occlusionStrength"),
            Variable(GLSLType.V4F, "diffuseBase"),
            Variable(GLSLType.V3F, "emissiveBase"),
            Variable(GLSLType.V1F, "sheen"),
            // outputs
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalTangent", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalBitangent", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalMetallic", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalRoughness", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalOcclusion", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalSheen", VariableMode.OUT),
            // just passed from uniforms
            Variable(GLSLType.V1F, "finalTranslucency", VariableMode.INOUT),
            Variable(GLSLType.V4F, "finalClearCoat", VariableMode.INOUT),
            Variable(GLSLType.V2F, "finalClearCoatRoughMetallic", VariableMode.INOUT),
            // for reflections;
            // we could support multiple
            Variable(GLSLType.BOOL, "hasReflectionPlane"),
            Variable(GLSLType.V3F, "reflectionPlaneNormal"),
            Variable(GLSLType.S2D, "reflectionPlane"),
            Variable(GLSLType.V4F, "reflectionCullingPlane"),
            /* Variable(GLSLType.V1F, "translucency"),
             Variable(GLSLType.V1F, "sheen"),
             Variable(GLSLType.V4F, "clearCoat"),
             Variable(GLSLType.V2F, "clearCoatRoughMetallic"),*/
        )


        return ShaderStage(
            "material", fragmentVariables, "" +
                    "if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n" +

                    // step by step define all material properties
                    "vec4 color = vec4(vertexColor.rgb, 1.0) * diffuseBase * texture(diffuseMap, uv);\n" +
                    "if(color.a < ${1f / 255f}) discard;\n" +
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
                    "   float effect = dot(reflectionPlaneNormal,finalNormal) * (1.0 - finalRoughness);\n" +
                    "   float factor = clamp((effect-.3)/.7, 0.0, 1.0);\n" +
                    "   if(factor > 0.0){\n" +
                    "       vec3 newColor = vec3(0.0);\n" +
                    "       vec3 newEmissive = finalColor * texelFetch(reflectionPlane, ivec2(gl_FragCoord.xy), 0).rgb;\n" +
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
                    "if(sheen > 0.0){\n" +
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
                    "} else finalSheen = 0.0;\n" +

                    "if(finalClearCoat.w > 0.0){\n" +
                    // cheap clear coat effect
                    "   float fresnel = 1.0 - abs(dot(finalNormal,V0));\n" +
                    "   float clearCoatEffect = pow(fresnel, 3.0) * finalClearCoat.w;\n" +
                    "   finalRoughness = mix(finalRoughness, finalClearCoatRoughMetallic.x, clearCoatEffect);\n" +
                    "   finalMetallic = mix(finalMetallic, finalClearCoatRoughMetallic.y, clearCoatEffect);\n" +
                    "   finalColor = mix(finalColor, finalClearCoat.rgb, clearCoatEffect);\n" +
                    "}\n"

        )

    }

    override fun createDepthShader(instanced: Boolean): Shader {

        val builder = createBuilder()
        builder.addVertex(createVertexStage(instanced, false))
        // no random id required

        // for the future, we could respect transparency from textures :)
        // base.addFragment(ShaderStage("material", emptyList(), ""))

        GFX.check()
        val shader = builder.create()
        shader.glslVersion = glslVersion
        GFX.check()
        return shader

    }

    override fun createFlatShader(postProcessing: ShaderStage?, instanced: Boolean, geoShader: GeoShader?): Shader {

        val base = createBase(instanced, true)

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

        val base = createBase(isInstanced, true)
        base.outputs = deferred

        // build & finish
        val shader = base.create()
        finish(shader)
        return shader

    }

}