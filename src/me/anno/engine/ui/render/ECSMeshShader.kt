package me.anno.engine.ui.render

import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.ecs.components.anim.BoneData.maxBones
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.maths.Maths.hasFlag
import me.anno.maths.bvh.RayTracing.loadMat4x3
import kotlin.math.max

open class ECSMeshShader(name: String) : BaseShader(name, "", emptyList(), "") {

    companion object {

        // the following values could be const, but I don't want them to be,
        // because they might change; and then they change, the compiler doesn't always update them

        const val getAnimMatrix =
            loadMat4x3 +
                    "mat4x3 getAnimMatrix(int index, float time){\n" +
                    "   if(index < 0 || index >= textureSize(animTexture,0).x) return mat4x3(vec4(1,0,0,0),vec4(1,0,0,0),vec4(1,0,0,0));\n" +
                    "   int timeI = int(time); float timeF = fract(time);\n" +
                    "   vec4 a = mix(texelFetch(animTexture, ivec2(index,  timeI), 0), texelFetch(animTexture, ivec2(index,  timeI+1), 0), timeF);\n" +
                    "   vec4 b = mix(texelFetch(animTexture, ivec2(index+1,timeI), 0), texelFetch(animTexture, ivec2(index+1,timeI+1), 0), timeF);\n" +
                    "   vec4 c = mix(texelFetch(animTexture, ivec2(index+2,timeI), 0), texelFetch(animTexture, ivec2(index+2,timeI+1), 0), timeF);\n" +
                    "   return loadMat4x3(a,b,c);\n" +
                    "}\n" +
                    "mat4x3 getAnimMatrix(int boneIndex, vec4 animIndices, vec4 animWeights){\n" +
                    "   int index = boneIndex * 3;\n" + // every matrix uses 3 pixels
                    "   mat4x3 t;\n" +
                    "   t = getAnimMatrix(index,animIndices.x)*animWeights.x;\n" +
                    "   if(animWeights.y != 0.0) t += getAnimMatrix(index,animIndices.y)*animWeights.y;\n" +
                    "   if(animWeights.z != 0.0) t += getAnimMatrix(index,animIndices.z)*animWeights.z;\n" +
                    "   if(animWeights.w != 0.0) t += getAnimMatrix(index,animIndices.w)*animWeights.w;\n" +
                    // remove unitFactor * unitMatrix from t
                    "   float unitFactor = 1.0 - (animWeights.x + animWeights.y + animWeights.z + animWeights.w);\n" +
                    "   t[0][0] += unitFactor;\n" +
                    "   t[1][1] += unitFactor;\n" +
                    "   t[2][2] += unitFactor;\n" +
                    "   return t;\n" +
                    "}\n" +
                    "mat4x3 getAnimMatrix(int boneIndex){ return getAnimMatrix(boneIndex, animIndices, animWeights); }\n"

        val colorToSRGB = "" +
                "#ifdef IS_LINEAR\n" +
                "   finalColor = pow(finalColor,vec3(1.0/2.2));\n" +
                "   finalEmissive = pow(finalEmissive,vec3(1.0/2.2));\n" +
                "   #undef IS_LINEAR\n" +
                "#endif\n"

        val colorToLinear = "" +
                "#ifndef IS_LINEAR\n" +
                "   finalColor = pow(finalColor,vec3(2.2));\n" +
                "   finalEmissive = pow(finalEmissive,vec3(2.2));\n" +
                "   #define IS_LINEAR\n" +
                "#endif\n"

        val discardByCullingPlane = "if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n"

        val v0 = "vec3 V0 = normalize(-finalPosition);\n"
        val sheenCalculation = "" +
                // sheen calculation
                "if(sheen > 0.0){\n" +
                "   vec3 sheenNormal = finalNormal;\n" +
                "   if(finalSheen * normalStrength.y > 0.0){\n" +
                "      vec3 normalFromTex = texture(sheenNormalMap, uv).rgb * 2.0 - 1.0;\n" +
                "           normalFromTex = matMul(tbn, normalFromTex);\n" +
                // original or transformed "finalNormal"? mmh...
                // transformed probably is better
                "      sheenNormal = mix(finalNormal, normalFromTex, normalStrength.y);\n" +
                "   }\n" +
                // calculate sheen
                "   float sheenFresnel = 1.0 - abs(dot(sheenNormal,V0));\n" +
                "   finalSheen = sheen * pow(sheenFresnel, 3.0);\n" +
                "} else finalSheen = 0.0;\n"

        val clearCoatCalculation = "" +
                colorToSRGB +
                "if(finalClearCoat.w > 0.0){\n" +
                // cheap clear coat effect
                "   float fresnel = 1.0 - abs(dot(finalNormal,V0));\n" +
                "   float clearCoatEffect = pow(fresnel, 3.0) * finalClearCoat.w;\n" +
                "   finalRoughness = mix(finalRoughness, finalClearCoatRoughMetallic.x, clearCoatEffect);\n" +
                "   finalMetallic = mix(finalMetallic, finalClearCoatRoughMetallic.y, clearCoatEffect);\n" +
                "   finalColor = mix(finalColor, finalClearCoat.rgb, clearCoatEffect);\n" +
                "   finalAlpha = mix(finalAlpha, 1.0, clearCoatEffect);\n" +
                "}\n"

        val reflectionPlaneCalculation = "" +
                // reflections
                "if(hasReflectionPlane){\n" +
                "   float effect = dot(reflectionPlaneNormal,finalNormal) * sqrt((1.0 - finalRoughness) * finalMetallic);\n" +
                "   float factor = min(effect, 1.0);\n" +
                "   if(factor > 0.0){\n" +
                // todo distance to plane, and fading
                // todo use normal for pseudo-refractive offset
                "       vec2 uv7 = gl_FragCoord.xy/renderSize;\n" +
                "       uv7.y = 1.0-uv7.y;\n" + // flipped on y-axis to save reprogramming of culling
                "       vec3 specularColor = finalColor;\n" + // could be changed
                "       float lod = finalRoughness * 10.0;\n" +
                "       vec2 duv0 = 1.0 / vec2(textureSize(reflectionPlane,int(lod))), duv1 = vec2(duv0.x,-duv0.y),\n" +
                "           du0 = vec2(duv0.x,0.0), dv0 = vec2(0.0,duv0.y);\n" +
                // todo don't blur for sharpest reflections
                "       vec3 skyEmissive = specularColor * pow((\n" +
                "           textureLod(reflectionPlane, uv7, lod).rgb * 4.0 +\n" +
                "          (textureLod(reflectionPlane, uv7 + du0, lod).rgb +\n" +
                "           textureLod(reflectionPlane, uv7 - du0, lod).rgb +\n" +
                "           textureLod(reflectionPlane, uv7 + dv0, lod).rgb +\n" +
                "           textureLod(reflectionPlane, uv7 - dv0, lod).rgb) * 2.0 +\n" +
                "          (textureLod(reflectionPlane, uv7 + duv0, lod).rgb +\n" +
                "           textureLod(reflectionPlane, uv7 - duv0, lod).rgb +\n" +
                "           textureLod(reflectionPlane, uv7 + duv1, lod).rgb +\n" +
                "           textureLod(reflectionPlane, uv7 - duv1, lod).rgb)) * 0.06125, vec3(2.2));\n" +
                "       finalEmissive += skyEmissive * factor;\n" +
                "       finalColor    *= 1.0 - factor;\n" +
                // prevents reflection map and SSR from being applied
                "       finalRoughness = mix(finalRoughness,  1.0, factor);\n" +
                "       finalMetallic  = mix(finalMetallic,   0.0, factor);\n" +
                "   }\n" +
                "}\n"

        val reflectionMapCalculation = "" +
                "#ifdef DEFERRED\n" +
                "   float factor = finalMetallic * (1.0 - finalRoughness);\n" +
                "   if(factor > 0.0){\n" +
                "       vec3 dir = $cubemapsAreLeftHanded * reflect(V0, finalNormal);\n" +
                "       vec3 newColor = vec3(0.0);\n" +
                // texture is SRGB -> convert to linear
                // todo like planar reflections, blur LODs (?)
                "       float lod = finalRoughness * 5.0;\n" +
                "       vec3 skyEmissive = pow(textureLod(reflectionMap, dir, lod).rgb, vec3(2.2));\n" +
                "       finalEmissive += finalColor * skyEmissive * factor;\n" +
                // doing this would make SSR reflect the incorrect color
                // "       finalColor    *= 1.0 - factor;\n" +
                // doing this would disable SSR
                //"       finalRoughness = mix(finalRoughness,  1.0, factor);\n" +
                //"       finalMetallic  = mix(finalMetallic,   0.0, factor);\n" +
                "   }\n" +
                "#endif\n" +
                ""

        val reflectionCalculation = "" +
                colorToLinear +
                reflectionPlaneCalculation +
                reflectionMapCalculation

        val normalMapCalculation = "" +
                // bitangent: checked, correct transform
                // can be checked with a lot of rotated objects in all orientations,
                // and a shader with light from top/bottom
                "mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                "if(abs(normalStrength.x) > 0.0){\n" +
                "   vec3 rawColor = texture(normalMap, uv).rgb;\n" +
                // support for bump maps: if grayscale, calculate gradient
                "   if(rawColor.x == rawColor.y && rawColor.y == rawColor.z){\n" +
                "       vec2 suv = uv * vec2(textureSize(normalMap,0));\n" +
                "       float divisor = (length(dFdx(suv)) + length(dFdy(suv)))*0.25;\n" +
                "       rawColor = normalize(vec3(dFdx(rawColor.x), dFdy(rawColor.x), max(divisor, 1e-6)));\n" +
                "   } else rawColor = rawColor * 2.0 - 1.0;\n" +
                "   vec3 normalFromTex = rawColor;\n" +
                "        normalFromTex = matMul(tbn, normalFromTex);\n" +
                // normalize?
                "   finalNormal = mix(finalNormal, normalFromTex, normalStrength.x);\n" +
                // for debugging
                // "   finalColor = rawColor*.5+.5;\n" +
                "}\n"

        val baseColorCalculation = "" +
                "vec4 texDiffuseMap = texture(diffuseMap, uv);\n" +
                "vec4 color = vec4(vertexColor0.rgb, 1.0) * diffuseBase * texDiffuseMap;\n" +
                "if(color.a < ${1f / 255f}) discard;\n" +
                "finalColor = color.rgb;\n" +
                "finalAlpha = color.a;\n"

        val normalTanBitanCalculation = "" +
                "finalTangent   = normalize(tangent.xyz);\n" + // for debugging
                "finalNormal    = normalize(gl_FrontFacing ? normal : -normal);\n" +
                "finalBitangent = cross(finalNormal, finalTangent) * tangent.w;\n" +
                "if(dot(finalBitangent,finalBitangent) > 0.0) finalBitangent = normalize(finalBitangent);\n"

        val emissiveCalculation = "finalEmissive  = texture(emissiveMap, uv).rgb * emissiveBase;\n"
        val occlusionCalculation = "finalOcclusion = (1.0 - texture(occlusionMap, uv).r) * occlusionStrength;\n"
        val metallicCalculation =
            "finalMetallic  = clamp(mix(metallicMinMax.x,  metallicMinMax.y,  texture(metallicMap,  uv).r), 0.0, 1.0);\n"
        val roughnessCalculation =
            "finalRoughness = clamp(mix(roughnessMinMax.x, roughnessMinMax.y, texture(roughnessMap, uv).r), 0.0, 1.0);\n"
        val finalMotionCalculation = "" +
                "#ifdef MOTION_VECTORS\n" +
                "   finalMotion = currPosition.xyz/currPosition.w - prevPosition.xyz/prevPosition.w;\n" +
                "#endif\n"

        val applyTransformCode = "" +
                "#ifdef PRS_TRANSFORM\n" +
                "   finalPosition = quatRot(localPosition, instanceRot) * instancePosSize.w + instancePosSize.xyz;\n" +
                "   #ifdef COLORS\n" +
                // scale not needed, because scale is scalar in this case
                "       normal = quatRot(normal, instanceRot);\n" +
                "       tangent.xyz = quatRot(tangent.xyz, instanceRot);\n" +
                "   #endif\n" + // colors
                "#else\n" +
                "   finalPosition = matMul(localTransform, vec4(localPosition, 1.0));\n" +
                "   #ifdef COLORS\n" +
                "       normal = normalize(matMul(localTransform, vec4(normal,0.0)));\n" +
                "       tangent.xyz = normalize(matMul(localTransform, vec4(tangent.xyz,0.0)));\n" +
                "   #endif\n" + // colors
                "#endif\n"

        val normalInitCode = "" +
                "       #ifdef COLORS\n" +
                "           normal = normals;\n" +
                "           tangent = tangents;\n" +
                "       #endif\n"

        val colorInitCode = "" +
                "#ifdef COLORS\n" +
                "   vertexColor0 = (hasVertexColors & 1) != 0 ? colors0 : vec4(1.0);\n" +
                "   vertexColor1 = (hasVertexColors & 2) != 0 ? colors1 : vec4(1.0);\n" +
                "   vertexColor2 = (hasVertexColors & 4) != 0 ? colors2 : vec4(1.0);\n" +
                "   vertexColor3 = (hasVertexColors & 8) != 0 ? colors3 : vec4(1.0);\n" +
                "   uv = uvs;\n" +
                "#endif\n"

        val motionVectorInit = "" +
                "#ifdef MOTION_VECTORS\n" +
                "   vec3 prevLocalPosition = localPosition;\n" +
                "#endif\n"

        val motionVectorCode = "" +
                "#ifdef MOTION_VECTORS\n" +
                "   currPosition = gl_Position;\n" +
                "   #ifdef PRS_TRANSFORM\n" +
                "       prevPosition = matMul(prevTransform, vec4(finalPosition, 1.0));\n" +
                "   #else\n" +
                "       prevPosition = matMul(prevTransform, vec4(matMul(prevLocalTransform, vec4(prevLocalPosition, 1.0)), 1.0));\n" +
                "   #endif\n" +
                "#endif\n"

        val instancedInitCode = "" +
                "#ifdef INSTANCED\n" +
                "   gfxId = instanceGfxId;\n" +
                "   mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2,instanceTrans3);\n" +
                "   #ifdef MOTION_VECTORS\n" +
                "       mat4x3 prevLocalTransform = mat4x3(instancePrevTrans0,instancePrevTrans1,instancePrevTrans2,instancePrevTrans3);\n" +
                "   #endif\n" +
                "#endif\n" // instanced

        fun animCode0() = "" +
                "#ifdef ANIMATED\n" +
                "   if(hasAnimation){\n" +
                "       mat4x3 jointMat;\n" +
                animationCode() +
                "       localPosition = matMul(jointMat, vec4(coords, 1.0));\n" +
                "       #ifdef MOTION_VECTORS\n" +
                animationCode2() +
                "       #endif\n" +
                "       #ifdef COLORS\n" +
                "           normal = matMul(jointMat, vec4(normals,0.0));\n" +
                "           tangent = vec4(matMul(jointMat, vec4(tangents.xyz,0.0)), tangents.w);\n" +
                "       #endif\n" +
                "   } else {\n" +
                "#endif\n" // animated

        val animCode1 = "" +
                "#ifdef ANIMATED\n" +
                "   }\n" +
                "#endif\n" // animated

        fun animationCode() = if (useAnimTextures) {
            "" +
                    "jointMat  = getAnimMatrix(boneIndices.x) * boneWeights.x;\n" +
                    "jointMat += getAnimMatrix(boneIndices.y) * boneWeights.y;\n" +
                    "jointMat += getAnimMatrix(boneIndices.z) * boneWeights.z;\n" +
                    "jointMat += getAnimMatrix(boneIndices.w) * boneWeights.w;\n"
        } else {
            "" +
                    "jointMat  = jointTransforms[boneIndices.x] * boneWeights.x;\n" +
                    "jointMat += jointTransforms[boneIndices.y] * boneWeights.y;\n" +
                    "jointMat += jointTransforms[boneIndices.z] * boneWeights.z;\n" +
                    "jointMat += jointTransforms[boneIndices.w] * boneWeights.w;\n"
        }

        fun animationCode2() = if (useAnimTextures) {
            "" +
                    "mat4x3 jointMat2;\n" +
                    "jointMat2  = getAnimMatrix(boneIndices.x,prevAnimIndices,prevAnimWeights) * boneWeights.x;\n" +
                    "jointMat2 += getAnimMatrix(boneIndices.y,prevAnimIndices,prevAnimWeights) * boneWeights.y;\n" +
                    "jointMat2 += getAnimMatrix(boneIndices.z,prevAnimIndices,prevAnimWeights) * boneWeights.z;\n" +
                    "jointMat2 += getAnimMatrix(boneIndices.w,prevAnimIndices,prevAnimWeights) * boneWeights.w;\n" +
                    "prevLocalPosition = matMul(jointMat2, vec4(coords, 1.0));\n"
        } else {
            "prevLocalPosition = localPosition;\n"
        }
    }

    init {
        glslVersion = max(glslVersion, 330)
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

    open fun createBase(flags: Int, postProcessing: List<ShaderStage>): ShaderBuilder {
        val builder = createBuilder()
        builder.addVertex(
            createVertexStages(flags)
        )
        builder.addVertex(createRandomIdStage())
        builder.addFragment(createFragmentStages(flags))
        builder.addFragment(postProcessing)
        return builder
    }

    open fun createVertexVariables(flags: Int): ArrayList<Variable> {

        val variables = ArrayList<Variable>(32)
        variables += Variable(GLSLType.V3F, "coords", VariableMode.ATTR)

        // uniforms
        variables += Variable(GLSLType.M4x4, "transform")
        if (flags.hasFlag(NEEDS_COLORS)) {
            variables += Variable(GLSLType.V1I, "hasVertexColors")
        }

        // outputs
        variables += Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
        variables += Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
        variables += Variable(GLSLType.V1F, "zDistance", VariableMode.OUT)

        if (flags.hasFlag(NEEDS_COLORS)) {
            variables += Variable(GLSLType.V2F, "uvs", VariableMode.ATTR)
            variables += Variable(GLSLType.V2F, "uv", VariableMode.OUT)

            variables += Variable(GLSLType.V3F, "normals", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "normal", VariableMode.OUT)

            variables += Variable(GLSLType.V4F, "tangents", VariableMode.ATTR)
            variables += Variable(GLSLType.V4F, "tangent", VariableMode.OUT)

            variables += Variable(GLSLType.V4F, "colors0", VariableMode.ATTR)
            if ((flags.hasFlag(IS_ANIMATED) || flags.hasFlag(NEEDS_MOTION_VECTORS) && flags.hasFlag(IS_INSTANCED))) {
                // too many attributes, only 16 are supported in OpenGL and DirectX
                variables += Variable(GLSLType.V4F, "colors1")
                variables += Variable(GLSLType.V4F, "colors2")
                variables += Variable(GLSLType.V4F, "colors3")
            } else {
                variables += Variable(GLSLType.V4F, "colors1", VariableMode.ATTR)
                variables += Variable(GLSLType.V4F, "colors2", VariableMode.ATTR)
                variables += Variable(GLSLType.V4F, "colors3", VariableMode.ATTR)
            }
            variables += Variable(GLSLType.V4F, "vertexColor0", VariableMode.OUT)
            variables += Variable(GLSLType.V4F, "vertexColor1", VariableMode.OUT)
            variables += Variable(GLSLType.V4F, "vertexColor2", VariableMode.OUT)
            variables += Variable(GLSLType.V4F, "vertexColor3", VariableMode.OUT)
        }

        val isInstanced = flags.hasFlag(IS_INSTANCED)
        val isAnimated = flags.hasFlag(IS_ANIMATED)
        if (flags.hasFlag(USES_PRS_TRANSFORM)) {
            variables += Variable(GLSLType.V4F, "instancePosSize", VariableMode.ATTR)
            variables += Variable(GLSLType.V4F, "instanceRot", VariableMode.ATTR)
        } else if (isInstanced) {
            variables += Variable(GLSLType.V3F, "instanceTrans0", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "instanceTrans1", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "instanceTrans2", VariableMode.ATTR)
            variables += Variable(GLSLType.V3F, "instanceTrans3", VariableMode.ATTR)
            variables += Variable(GLSLType.V4F, "instanceGfxId", VariableMode.ATTR)
            variables += Variable(GLSLType.V4F, "gfxId", VariableMode.OUT)
            if (isAnimated && useAnimTextures) {
                variables += Variable(GLSLType.V4F, "boneWeights", VariableMode.ATTR)
                variables += Variable(GLSLType.V4I, "boneIndices", VariableMode.ATTR)
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
                variables += Variable(GLSLType.V4F, "boneWeights", VariableMode.ATTR)
                variables += Variable(GLSLType.V4I, "boneIndices", VariableMode.ATTR)
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

        if (flags.hasFlag(NEEDS_MOTION_VECTORS)) {
            variables += Variable(GLSLType.M4x4, "prevTransform")
            if (isInstanced) {
                variables += Variable(GLSLType.V3F, "instancePrevTrans0", VariableMode.ATTR)
                variables += Variable(GLSLType.V3F, "instancePrevTrans1", VariableMode.ATTR)
                variables += Variable(GLSLType.V3F, "instancePrevTrans2", VariableMode.ATTR)
                variables += Variable(GLSLType.V3F, "instancePrevTrans3", VariableMode.ATTR)
            } else {
                variables += Variable(GLSLType.M4x3, "prevLocalTransform")
            }
            if (isAnimated) {
                val type = if (isInstanced) VariableMode.ATTR else VariableMode.IN
                variables += Variable(GLSLType.V4F, "prevAnimWeights", type)
                variables += Variable(GLSLType.V4F, "prevAnimIndices", type)
            }
            variables += Variable(GLSLType.V4F, "currPosition", VariableMode.OUT)
            variables += Variable(GLSLType.V4F, "prevPosition", VariableMode.OUT)
        }

        return variables
    }

    open fun createVertexStages(flags: Int): List<ShaderStage> {
        val defines = createDefines(flags)
        val variables = createVertexVariables(flags)
        val stage = ShaderStage(
            "vertex",
            variables, defines.toString() +
                    "localPosition = coords;\n" +
                    motionVectorInit +

                    instancedInitCode +

                    animCode0() +
                    normalInitCode +
                    animCode1 +

                    applyTransformCode +
                    colorInitCode +
                    "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                    motionVectorCode +
                    ShaderLib.positionPostProcessing
        )
        if (flags.hasFlag(IS_ANIMATED) && useAnimTextures) stage.add(getAnimMatrix)
        if (flags.hasFlag(USES_PRS_TRANSFORM)) stage.add(quatRot)
        return listOf(stage)
    }

    open fun createFragmentVariables(flags: Int): ArrayList<Variable> {
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
            Variable(GLSLType.V2F, "uv", VariableMode.INOUT),
            Variable(GLSLType.V3F, "normal"),
            Variable(GLSLType.V4F, "tangent"),
            Variable(GLSLType.V4F, "vertexColor0"),
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
            Variable(GLSLType.V2F, "renderSize"),
        )
        if (flags.hasFlag(IS_DEFERRED)) {
            list += Variable(GLSLType.SCube, "reflectionMap")
        }
        if (flags.hasFlag(NEEDS_MOTION_VECTORS)) {
            list += Variable(GLSLType.V4F, "currPosition")
            list += Variable(GLSLType.V4F, "prevPosition")
            list += Variable(GLSLType.V3F, "finalMotion", VariableMode.OUT)
        }
        return list
    }

    // just like the gltf pbr shader define all material properties
    open fun createFragmentStages(flags: Int): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(flags) + listOf(Variable(GLSLType.V4F, "cameraRotation")),
                createDefines(flags).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        // to do weak devices like phones shouldn't use this
                        // to do this only should be enabled for when the normal map is grayscale
                        // todo doesn't look right yet (movement too extreme... why??)
                        /*"#define parallaxMap normalMap\n" +
                        "if(textureSize(parallaxMap,0).x > 1){\n" +
                        "   finalTangent   = normalize(tangent.xyz);\n" + // for debugging
                        "   finalNormal    = normalize(gl_FrontFacing ? normal : -normal);\n" +
                        "   finalBitangent = cross(finalNormal, finalTangent) * tangent.w;\n" +
                        "   if(dot(finalBitangent,finalBitangent) > 0.0) finalBitangent = normalize(finalBitangent);\n" +
                        "   mat3 TBN = transpose(mat3(finalTangent.xyz,finalBitangent,finalNormal));\n" +
                        "   vec3 viewDir = TBN * normalize(quatRot(finalPosition, cameraRotation));\n" + // is this right???
                        "   uv = parallaxMapUVs(parallaxMap, uv, viewDir * vec3(-1,-1,1), 0.05);\n" +
                        "}\n" +*/
                        baseColorCalculation +
                        normalTanBitanCalculation +
                        normalMapCalculation +
                        emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionCalculation +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping)
        )
    }

    override fun createDepthShader(flags: Int): Shader {

        val builder = createBuilder()
        builder.addVertex(createVertexStages(flags))
        builder.addFragment(
            ShaderStage(
                "depth", listOf(
                    Variable(GLSLType.V1F, "finalDepth", VariableMode.OUT)
                ), "finalDepth = gl_FragCoord.z;\n" // use gl_FragDepth instead?
            )
        )

        GFX.check()
        val shader = builder.create("depth$flags")
        shader.ignoreNameWarnings(
            "applyToneMapping", "worldScale", "cameraPosition",
            "cameraRotation", "invLocalTransform", "diffuseBase", "normalStrength",
            "emissiveBase", "roughnessMinMax", "metallicMinMax", "occlusionStrength",
            "finalTranslucency", "finalSheen", "sheen", "finalClearCoat", "randomIdData",
            "renderSize", "reflectionCullingPlane", "hasReflectionPlane", "numberOfLights"
        )
        shader.glslVersion = glslVersion
        GFX.check()
        return shader
    }

    override fun createForwardShader(flags: Int, postProcessing: List<ShaderStage>): Shader {
        val shader = createBase(flags, postProcessing).create("fwd$flags")
        finish(shader)
        return shader
    }

    override fun createDeferredShader(
        deferred: DeferredSettings,
        flags: Int,
        postProcessing: List<ShaderStage>
    ): Shader {
        val base = createBase(flags, postProcessing)
        base.outputs = deferred
        // build & finish
        val shader = base.create("def$flags")
        finish(shader)
        return shader
    }
}