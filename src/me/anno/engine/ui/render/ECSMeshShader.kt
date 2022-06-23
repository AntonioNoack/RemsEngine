package me.anno.engine.ui.render

import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.*
import me.anno.gpu.shader.builder.Function
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.bvh.RayTracing.loadMat4x3
import me.anno.mesh.assimp.AnimGameItem.Companion.maxBones

open class ECSMeshShader(name: String) : BaseShader(name, "", emptyList(), "") {

    companion object {
        const val getAnimMatrix =
            "" + loadMat4x3 +
                    "mat4x3 getAnimMatrix(int index, float time){\n" +
                    "   int timeI = int(time); float timeF = fract(time);\n" +
                    "   vec4 a = mix(texelFetch(animTexture, ivec2(index,  timeI), 0), texelFetch(animTexture, ivec2(index,  timeI+1), 0), timeF);\n" +
                    "   vec4 b = mix(texelFetch(animTexture, ivec2(index+1,timeI), 0), texelFetch(animTexture, ivec2(index+1,timeI+1), 0), timeF);\n" +
                    "   vec4 c = mix(texelFetch(animTexture, ivec2(index+2,timeI), 0), texelFetch(animTexture, ivec2(index+2,timeI+1), 0), timeF);\n" +
                    "   return loadMat4x3(a,b,c);\n" +
                    "}\n" +
                    "mat4x3 getAnimMatrix(int boneIndex, vec4 animIndices, vec4 animWeights){\n" +
                    "   int index = boneIndex * 3;\n" + // every matrix uses 3 pixels
                    "   mat4x3 t;\n" +
                    "   t  = getAnimMatrix(index,animIndices.x)*animWeights.x;\n" +
                    "   t += getAnimMatrix(index,animIndices.y)*animWeights.y;\n" +
                    "   t += getAnimMatrix(index,animIndices.z)*animWeights.z;\n" +
                    "   t += getAnimMatrix(index,animIndices.w)*animWeights.w;\n" +
                    "   return t;\n" +
                    "}\n" +
                    "mat4x3 getAnimMatrix(int boneIndex){ return getAnimMatrix(boneIndex, animIndices, animWeights); }\n"
    }

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

    open fun createBase(
        isInstanced: Boolean, isAnimated: Boolean, colors: Boolean, motionVectors: Boolean
    ): ShaderBuilder {
        val builder = createBuilder()
        builder.addVertex(createVertexStage(isInstanced, isAnimated, colors, motionVectors))
        builder.addVertex(createRandomIdStage())
        builder.addFragment(createFragmentStage(isInstanced, isAnimated, motionVectors))
        return builder
    }

    open fun createVertexVariables(
        isInstanced: Boolean, isAnimated: Boolean, colors: Boolean, motionVectors: Boolean
    ): ArrayList<Variable> {

        val variables = ArrayList<Variable>(32)
        variables += Variable(GLSLType.V3F, "coords", VariableMode.ATTR)

        if (colors) {
            variables += Variable(GLSLType.V2F, "uvs", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "normals", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "tangents", VariableMode.ATTR)
            variables += Variable(GLSLType.V4F, "colors", VariableMode.ATTR)
        }

        // uniforms
        variables += Variable(GLSLType.M4x4, "transform")
        if (colors) {
            variables += Variable(GLSLType.V1B, "hasVertexColors")
        }

        // outputs
        variables += Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
        variables += Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
        variables += Variable(GLSLType.V1F, "zDistance", VariableMode.OUT)

        if (colors) {
            variables += Variable(GLSLType.V2F, "uv", false)
            variables += Variable(GLSLType.V3F, "normal", false)
            variables += Variable(GLSLType.V3F, "tangent", false)
            variables += Variable(GLSLType.V4F, "vertexColor", false)
        }

        if (isInstanced) {
            variables += Variable(GLSLType.V3F, "instanceTrans0", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "instanceTrans1", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "instanceTrans2", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "instanceTrans3", VariableMode.ATTR)
            if (colors) {
                variables += Variable(GLSLType.V4F, "instanceTint", VariableMode.ATTR)
                variables += Variable(GLSLType.V4F, "tint", VariableMode.OUT)
            }
            if (isAnimated && useAnimTextures) {
                variables += Variable(GLSLType.V4F, "weights", VariableMode.ATTR)
                variables += Variable(GLSLType.V4I, "indices", VariableMode.ATTR)
                variables += Variable(GLSLType.V4F, "animWeights", VariableMode.ATTR)
                variables += Variable(GLSLType.V4F, "animIndices", VariableMode.ATTR)
                variables += Variable(GLSLType.S2D, "animTexture")
                variables += Variable(GLSLType.V1B, "hasAnimation")
            }
        } else {

            //        A
            // frames |
            //        V
            //                  <---------->
            //          bones x 3 rows for matrix

            // attributes
            if (isAnimated) {
                variables += Variable(GLSLType.V4F, "weights", VariableMode.ATTR)
                variables += Variable(GLSLType.V4I, "indices", VariableMode.ATTR)
                if (useAnimTextures) {
                    variables += Variable(GLSLType.V4F, "animWeights")
                    variables += Variable(GLSLType.V4F, "animIndices")
                    variables += Variable(GLSLType.S2D, "animTexture")
                    variables += Variable(GLSLType.V1B, "hasAnimation")
                } else {
                    // not required for the instanced rendering, because this is instance specific,
                    // and therefore not supported for instanced rendering
                    variables += Variable(GLSLType.M4x3, "jointTransforms", maxBones)
                    variables += Variable(GLSLType.V1B, "hasAnimation")
                }
            }
            variables += Variable(GLSLType.M4x3, "localTransform")
            // Variable(GLSLType.V4F, "weight", false),
        }

        if (motionVectors) {
            variables += Variable(GLSLType.M4x4, "prevTransform")
            if (isInstanced) {
                variables += Variable(GLSLType.V3F, "prevInstanceTrans0", VariableMode.ATTR)
                variables += Variable(GLSLType.V3F, "prevInstanceTrans1", VariableMode.ATTR)
                variables += Variable(GLSLType.V3F, "prevInstanceTrans2", VariableMode.ATTR)
                variables += Variable(GLSLType.V3F, "prevInstanceTrans3", VariableMode.ATTR)
            } else {
                variables += Variable(GLSLType.M4x3, "prevLocalTransform")
            }
            variables += Variable(GLSLType.V4F, "currPosition", VariableMode.OUT)
            variables += Variable(GLSLType.V4F, "prevPosition", VariableMode.OUT)
        }

        return variables
    }

    open fun createVertexStage(
        isInstanced: Boolean, isAnimated: Boolean, colors: Boolean, motionVectors: Boolean
    ): ShaderStage {

        val defines =
            "" +
                    (if (isInstanced) "#define INSTANCED\n" else "") +
                    (if (isAnimated) "#define ANIMATED\n" else "") +
                    (if (colors) "#define COLORS\n" else "") +
                    (if (motionVectors) "#define MOTION_VECTORS\n" else "")

        val animationCode = if (useAnimTextures) {
            "" +
                    "jointMat  = getAnimMatrix(indices.x) * weights.x;\n" +
                    "jointMat += getAnimMatrix(indices.y) * weights.y;\n" +
                    "jointMat += getAnimMatrix(indices.z) * weights.z;\n" +
                    "jointMat += getAnimMatrix(indices.w) * weights.w;\n"
        } else {
            "" +
                    "jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                    "jointMat += jointTransforms[indices.y] * weights.y;\n" +
                    "jointMat += jointTransforms[indices.z] * weights.z;\n" +
                    "jointMat += jointTransforms[indices.w] * weights.w;\n"
        }

        val variables = createVertexVariables(isInstanced, isAnimated, colors, motionVectors)
        val stage = ShaderStage(
            "vertex",
            variables,
            "" + defines + "#ifdef INSTANCED\n" +
                    "   mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2,instanceTrans3);\n" +
                    "   #ifdef COLORS\n" +
                    "       tint = instanceTint;\n" +
                    "   #endif\n" + // colors
                    "#endif\n" + // instanced
                    "   #ifdef ANIMATED\n" +
                    "   if(hasAnimation){\n" +
                    "       mat4x3 jointMat;\n" +
                    animationCode +
                    // todo if motion vectors, use previous jointMat
                    "       localPosition = jointMat * vec4(coords, 1.0);\n" +
                    "       #ifdef COLORS\n" +
                    "           normal = jointMat * vec4(normals, 0.0);\n" +
                    "           tangent = jointMat * vec4(tangents, 0.0);\n" +
                    "       #endif\n" +
                    "   } else {\n" +
                    "   #endif\n" + // animated
                    "       localPosition = coords;\n" +
                    "       #ifdef COLORS\n" +
                    "           normal = normals;\n" +
                    "           tangent = tangents;\n" +
                    "       #endif\n" +
                    "   #ifdef ANIMATED\n" +
                    "   }\n" +
                    "   #endif\n" + // animated
                    "   finalPosition = localTransform * vec4(localPosition, 1.0);\n" +
                    "   #ifdef COLORS\n" +
                    "       normal = localTransform * vec4(normal, 0.0);\n" +
                    "       tangent = localTransform * vec4(tangent, 0.0);\n" +
                    "   #endif\n" + // colors
                    "#ifdef COLORS\n" +
                    "   normal = normalize(normal);\n" + // here? nah ^^
                    "   vertexColor = hasVertexColors ? colors : vec4(1.0);\n" +
                    "   uv = uvs;\n" +
                    "#endif\n" +
                    "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    "#ifdef MOTION_VECTORS\n" +
                    "   currPosition = gl_Position;\n" +
                    "   prevPosition = prevTransform * vec4(prevLocalTransform * vec4(localPosition, 1.0), 1.0);\n" +
                    "#endif\n" +
                    ShaderLib.positionPostProcessing
        )

        if (isAnimated && useAnimTextures) {
            stage.functions.add(Function(getAnimMatrix))
        }

        return stage

    }

    open fun createFragmentVariables(
        isInstanced: Boolean, isAnimated: Boolean, motionVectors: Boolean
    ): ArrayList<Variable> {
        val list = arrayListOf(
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
            Variable(GLSLType.V1B, "hasReflectionPlane"),
            Variable(GLSLType.V3F, "reflectionPlaneNormal"),
            Variable(GLSLType.S2D, "reflectionPlane"),
            Variable(GLSLType.V4F, "reflectionCullingPlane"),
            Variable(GLSLType.V1F, "translucency"),
            Variable(GLSLType.V1F, "sheen"),
            Variable(GLSLType.V4F, "clearCoat"),
            Variable(GLSLType.V2F, "clearCoatRoughMetallic"),
        )
        if (motionVectors) {
            list += Variable(GLSLType.V4F, "currPosition")
            list += Variable(GLSLType.V4F, "prevPosition")
            list += Variable(GLSLType.V3F, "finalMotion", VariableMode.OUT)
        }
        return list
    }

    // just like the gltf pbr shader define all material properties
    open fun createFragmentStage(isInstanced: Boolean, isAnimated: Boolean, motionVectors: Boolean): ShaderStage {

        return ShaderStage(
            "material",
            createFragmentVariables(isInstanced, isAnimated, motionVectors),
            "" +
                    "if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n" +

                    // step by step define all material properties
                    "vec4 color = vec4(vertexColor.rgb, 1.0) * diffuseBase * texture(diffuseMap, uv);\n" +
                    "if(color.a < ${1f / 255f}) discard;\n" +
                    "finalColor = color.rgb;\n" +
                    "finalAlpha = color.a;\n" +
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
                    "finalOcclusion = (1.0 - texture(occlusionMap, uv).r) * occlusionStrength;\n" +
                    "finalMetallic  = clamp(mix(metallicMinMax.x,  metallicMinMax.y,  texture(metallicMap,  uv).r), 0.0, 1.0);\n" +
                    "finalRoughness = clamp(mix(roughnessMinMax.x, roughnessMinMax.y, texture(roughnessMap, uv).r), 0.0, 1.0);\n" +

                    // reflections
                    // use roughness instead?
                    // "   if(finalMetallic > 0.0) finalColor = mix(finalColor, texture(reflectionPlane,uv).rgb, finalMetallic);\n" +
                    "if(hasReflectionPlane){\n" +
                    "   float effect = dot(reflectionPlaneNormal,finalNormal) * (1.0 - finalRoughness);\n" +
                    "   float factor = clamp((effect-.3)*1.4, 0.0, 1.0);\n" +
                    "   if(factor > 0.0){\n" +
                    "       vec3 newColor = vec3(0.0);\n" +
                    "       vec3 newEmissive = finalColor * texelFetch(reflectionPlane, ivec2(gl_FragCoord.xy), 0).rgb;\n" +
                    // also multiply for mirror color <3
                    "       finalEmissive = mix(finalEmissive, newEmissive, factor);\n" +
                    // "       finalEmissive /= (1-finalEmissive);\n" + // only required, if tone mapping is applied
                    "       finalColor = mix(finalColor, newColor, factor);\n" +
                    // "       finalRoughness = 0;\n" +
                    // "       finalMetallic = 0;\n" +
                    "   }\n" + "};\n" +

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
                    "}\n" +
                    (if (motionVectors) "finalMotion = currPosition.xyz/currPosition.w - prevPosition.xyz/prevPosition.w;\n" else "") +
                    ""

        )

    }

    override fun createDepthShader(isInstanced: Boolean, isAnimated: Boolean, motionVectors: Boolean): Shader {

        val builder = createBuilder()
        builder.addVertex(createVertexStage(isInstanced, isAnimated, false, motionVectors))
        // no random id required

        // for the future, we could respect transparency from textures :)
        // base.addFragment(ShaderStage("material", emptyList(), ""))

        GFX.check()
        val shader = builder.create()
        shader.glslVersion = glslVersion
        GFX.check()
        return shader

    }

    override fun createForwardShader(
        postProcessing: ShaderStage?,
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): Shader {

        val base = createBase(isInstanced, isAnimated, true, motionVectors)

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
        isAnimated: Boolean,
        motionVectors: Boolean
    ): Shader {

        val base = createBase(isInstanced, isAnimated, colors = true, motionVectors)
        base.outputs = deferred

        // build & finish
        val shader = base.create()
        finish(shader)
        return shader

    }

}