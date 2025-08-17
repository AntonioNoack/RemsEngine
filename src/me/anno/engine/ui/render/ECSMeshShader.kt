package me.anno.engine.ui.render

import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.ecs.components.anim.BoneData.maxBones
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.gamma
import me.anno.gpu.shader.ShaderLib.gammaInv
import me.anno.gpu.shader.ShaderLib.loadMat4x3
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.utils.types.Booleans.hasFlag
import kotlin.math.max

open class ECSMeshShader(name: String) : BaseShader(name, "", emptyList(), "") {

    companion object {

        // the following values could be const, but I don't want them to be,
        // because they might change; and then they change, the compiler doesn't always update them

        const val getAnimMatrix =
            loadMat4x3 +
                    "mat4x3 getAnimMatrix(int index, float time){\n" +
                    "   if(index < 0 || index >= textureSize(animTexture,0).x) return loadMat4x3(vec4(1,0,0,0),vec4(1,0,0,0),vec4(1,0,0,0));\n" +
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
                "   finalColor = pow(max(finalColor,vec3(0.0)),vec3($gammaInv));\n" +
                "   finalEmissive = pow(max(finalEmissive,vec3(0.0)),vec3($gammaInv));\n" +
                "   #undef IS_LINEAR\n" +
                "#endif\n"

        val colorToLinear = "" +
                "#ifndef IS_LINEAR\n" +
                "   finalColor = pow(max(finalColor,vec3(0.0)),vec3($gamma));\n" +
                "   finalEmissive = pow(max(finalEmissive,vec3(0.0)),vec3($gamma));\n" +
                "   #define IS_LINEAR\n" +
                "#endif\n"

        val discardByCullingPlane = "if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n"

        // todo we should add a flag for using TAA...
        val jitterUVCorrection = "// uv -= jitterInPixels.x * dFdx(uv) + jitterInPixels.y * dFdy(uv);\n"

        val v0 = "vec3 V0 = normalize(-finalPosition);\n"
        val sheenCalculation = "" +
                // sheen calculation
                "if (sheen > 0.0) {\n" +
                "   vec3 sheenNormal = finalNormal;\n" +
                "   if(finalSheen * normalStrength.y > 0.0){\n" +
                "      vec3 normalFromTex = texture(sheenNormalMap, uv).rgb * 2.0 - 1.0;\n" +
                "           normalFromTex = normalize(normalFromTex);\n" +
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
                "if (finalClearCoat.w > 0.0) {\n" +
                // cheap clear coat effect
                "   float fresnel = 1.0 - abs(dot(finalNormal,V0));\n" +
                "   float clearCoatEffect = pow(fresnel, 3.0) * finalClearCoat.w;\n" +
                "   finalRoughness = mix(finalRoughness, finalClearCoatRoughMetallic.x, clearCoatEffect);\n" +
                "   finalMetallic = mix(finalMetallic, finalClearCoatRoughMetallic.y, clearCoatEffect);\n" +
                "   finalColor = mix(finalColor, finalClearCoat.rgb, clearCoatEffect);\n" +
                "   finalAlpha = mix(finalAlpha, 1.0, clearCoatEffect);\n" +
                "}\n"

        val reflectivityCalculation = "" +
                "finalReflectivity = getReflectivity(finalRoughness,finalMetallic);\n"

        val reflectionPlaneCalculation = "" +
                // reflections
                "if (hasReflectionPlane) {\n" +
                "   float effect0 = dot(reflectionPlaneNormal,finalNormal);\n" +
                "   float effect = effect0 * finalReflectivity;\n" +
                "   if (effect > 0.0) {\n" +
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
                "           textureLod(reflectionPlane, uv7 - duv1, lod).rgb)) * 0.06125, vec3($gamma));\n" +
                "       finalRoughness = mix(finalRoughness,  1.0, effect);\n" +
                "       finalMetallic  = mix(finalMetallic,   0.0, effect);\n" +
                reflectivityCalculation +
                // new reflectivity might not be zero -> only subtract, what we're allowed to remove
                "       effect -= effect0 * finalReflectivity;\n" +
                "       finalEmissive += skyEmissive * effect;\n" +
                "       finalColor *= 1.0 - effect;\n" +
                // prevents reflection map and SSR from being applied
                "   }\n" +
                "}\n"

        val reflectionMapCalculation = "" +
                "#ifdef DEFERRED\n" +
                "   float reflectivity = finalReflectivity;\n" +
                "   if(reflectivity > 0.0){\n" +
                "       vec3 dir = $cubemapsAreLeftHanded * reflect(V0, finalNormal);\n" +
                "       vec3 newColor = vec3(0.0);\n" +
                // texture is SRGB -> convert to linear
                // todo like planar reflections, blur LODs (?)
                "       float lod = finalRoughness * 5.0;\n" +
                "       vec3 skyEmissive = pow(textureLod(reflectionMap, dir, lod).rgb, vec3($gamma));\n" +
                "       finalEmissive += finalColor * skyEmissive * reflectivity;\n" +
                // doing this would make SSR reflect the incorrect color
                // "       finalColor    *= 1.0 - reflectivity;\n" +
                // doing this would disable SSR
                //"       finalRoughness = mix(finalRoughness,  1.0, reflectivity);\n" +
                //"       finalMetallic  = mix(finalMetallic,   0.0, reflectivity);\n" +
                "   }\n" +
                "#endif\n" +
                ""

        val reflectionCalculation = "" +
                colorToLinear +
                reflectivityCalculation +
                reflectionPlaneCalculation +
                reflectionMapCalculation

        /**
         * support for bump maps: if grayscale or only red, calculate gradient
         * */
        val normalMapBumpMapSupport = "" +
                "   if((normalColor.x == normalColor.y && normalColor.y == normalColor.z) ||" +
                "      (normalColor.y == 0.0 && normalColor.z == 0.0)){\n" +
                "       vec2 suv = uv * vec2(textureSize(normalMap,0));\n" +
                "       float divisor = (length(dFdx(suv)) + length(dFdy(suv)))*0.25;\n" +
                "       normalFromTex = normalize(vec3(dFdx(normalColor.x), dFdy(normalColor.x), max(divisor, 1e-6)));\n" +
                "   } else "

        val normalMapMixing = "" +
                "   normalFromTex = matMul(tbn, normalFromTex);\n" +
                "   finalNormal = mix(finalNormal, normalFromTex, normalStrength.x);\n" +  // normalize?
                "   finalNormal *= 1.0 / (1e-38 + length(finalNormal));\n"

        val normalMapCalculation = "" +
                // bitangent: checked, correct transform
                // can be checked with a lot of rotated objects in all orientations,
                // and a shader with light from top/bottom
                "mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                "if(abs(normalStrength.x) > 0.0){\n" +
                "   vec3 normalColor = texture(normalMap, uv, lodBias).rgb;\n" +
                "   vec3 normalFromTex;\n" +
                normalMapBumpMapSupport + "normalFromTex = normalColor * 2.0 - 1.0;\n" + // after else
                normalMapMixing +
                "}\n"

        val baseColorCalculation = "" +
                "vec4 texDiffuseMap = texture(diffuseMap, uv, lodBias);\n" +
                "vec4 color = vec4(vertexColor0.rgb, 1.0) * diffuseBase * texDiffuseMap;\n" +
                "if(color.a < ${1f / 255f}) discard;\n" +
                "finalColor = color.rgb;\n" +
                "finalAlpha = color.a;\n"

        val normalCalculation = "" +
                "finalNormal    = normalize(gl_FrontFacing ? normal : -normal);\n"

        val normalTanBitanCalculation = "" +
                normalCalculation +
                "finalTangent   = normalize(tangent.xyz);\n" +
                "finalBitangent = cross(finalNormal, finalTangent) * tangent.w;\n" +
                "if(dot(finalBitangent,finalBitangent) > 0.0) finalBitangent = normalize(finalBitangent);\n"

        val emissiveCalculation = "finalEmissive  = texture(emissiveMap, uv, lodBias).rgb * emissiveBase;\n"
        val occlusionCalculation =
            "finalOcclusion = (1.0 - texture(occlusionMap, uv, lodBias).r) * occlusionStrength;\n"
        val metallicCalculation =
            "finalMetallic  = clamp(mix(metallicMinMax.x, metallicMinMax.y, texture(metallicMap, uv, lodBias).r), 0.0, 1.0);\n"
        val roughnessCalculation =
            "#define HAS_ROUGHNESS\n" +
                    "finalRoughness = clamp(mix(roughnessMinMax.x, roughnessMinMax.y, texture(roughnessMap, uv, lodBias).r), 0.0, 1.0);\n"
        val finalMotionCalculation = "" +
                "#ifdef MOTION_VECTORS\n" +
                "   finalMotion = currPosition.xyz/currPosition.w - prevPosition.xyz/prevPosition.w;\n" +
                "#endif\n"

        val glPositionCode = "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n"

        val motionVectorCode = "" +
                "#ifdef MOTION_VECTORS\n" +
                "   currPosition = gl_Position;\n" +
                "   prevPosition = matMul(prevTransform, prevPosition);\n" +
                "#endif\n"

        fun animCode0() = "" +
                "#ifdef ANIMATED\n" +
                "   if(hasAnimation){\n" +
                "       mat4x3 jointMat;\n" +
                animationCode() +
                "       localPosition = matMul(jointMat, vec4(positions, 1.0));\n" +
                "       #ifdef MOTION_VECTORS\n" +
                animationCode2() +
                "       #endif\n" +
                "       #ifdef COLORS\n" +
                "           normal = matMul(jointMat, vec4(normal,0.0));\n" +
                "           tangent = vec4(matMul(jointMat, vec4(tangent.xyz,0.0)), tangent.w);\n" +
                "       #endif\n" +
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
                    "prevLocalPosition = matMul(jointMat2, vec4(positions, 1.0));\n"
        } else {
            "prevLocalPosition = localPosition;\n"
        }
    }

    init {
        glslVersion = max(glslVersion, 330)
    }

    open fun createRandomIdStage(): ShaderStage {
        return ShaderStage(
            "randomId", listOf(
                Variable(GLSLType.V2I, "randomIdData", VariableMode.IN), // vertices/instance, random offset
                Variable(GLSLType.V1I, "randomId", VariableMode.OUT)
            ), "randomId = (gl_VertexID + gl_InstanceID * randomIdData.x + randomIdData.y) & 0xffff;\n"
        )
    }

    open fun createBase(key: ShaderKey): ShaderBuilder {
        val builder = ShaderBuilder(name, null, key.ditherMode)
        val flags = key.flags
        builder.addVertex(createVertexStages(key))
        builder.addVertex(createRandomIdStage())
        builder.addVertex(key.renderer.getVertexPostProcessing(flags))
        builder.addFragment(createFragmentStages(key))
        builder.addFragment(key.renderer.getPixelPostProcessing(flags))
        return builder
    }

    open fun createAnimVariables(key: ShaderKey): ArrayList<Variable> {

        val flags = key.flags
        val variables = ArrayList<Variable>(32)

        val isInstanced = flags.hasFlag(IS_INSTANCED)
        if (isInstanced) {
            if (useAnimTextures) {
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

        if (flags.hasFlag(NEEDS_MOTION_VECTORS)) {
            val type = if (isInstanced) VariableMode.ATTR else VariableMode.IN
            variables += Variable(GLSLType.V4F, "prevAnimWeights", type)
            variables += Variable(GLSLType.V4F, "prevAnimIndices", type)
        }

        return variables
    }

    /**
     * loads localPosition, localNormal, localTangent and such from vertex data
     * */
    fun loadVertex(key: ShaderKey): List<ShaderStage> {
        return loadVertex(key, key.flags)
    }

    fun loadVertex(key: ShaderKey, flags: Int): List<ShaderStage> {
        val vertexData = key.vertexData
        return vertexData.loadPosition +
                f(vertexData.loadNorTan, flags.hasFlag(NEEDS_COLORS)) +
                f(vertexData.loadColors, flags.hasFlag(NEEDS_COLORS)) +
                f(vertexData.loadMotionVec, flags.hasFlag(NEEDS_MOTION_VECTORS))
    }

    /**
     * creates pre-processor defines, that may be needed for optimization,
     * or to detect whether some variables are truly available
     * */
    fun createDefines(key: ShaderKey): ShaderStage {
        return ShaderStage("v-def", emptyList(), concatDefines(key).toString())
    }

    /**
     * transforms the vertex from local space into camera-space,
     * based on instanced rendering if applicable
     * */
    fun transformVertex(key: ShaderKey): List<ShaderStage> {
        val flags = key.flags
        val instanceData = key.instanceData
        return instanceData.transformPosition +
                f(instanceData.transformNorTan, flags.hasFlag(NEEDS_COLORS)) +
                f(instanceData.transformColors, flags.hasFlag(NEEDS_COLORS)) +
                f(instanceData.transformMotionVec, flags.hasFlag(NEEDS_MOTION_VECTORS))
    }

    /**
     * calculates gl_Position, and currPosition/prevPosition for motion vectors if needed
     * */
    fun finishVertex(key: ShaderKey): ShaderStage {
        return if (!key.flags.hasFlag(NEEDS_MOTION_VECTORS)) {
            // easy default
            ShaderStage(
                "v-finish", listOf(
                    Variable(GLSLType.M4x4, "transform"),
                    Variable(GLSLType.V3F, "finalPosition"),
                ), glPositionCode
            )
        } else {
            // default plus pass motion vector data to fragment stages
            ShaderStage(
                "v-vec-finish", listOf(
                    Variable(GLSLType.M4x4, "transform"),
                    Variable(GLSLType.M4x4, "prevTransform"),
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V4F, "currPosition", VariableMode.OUT),
                    Variable(GLSLType.V4F, "prevPosition", VariableMode.INOUT)
                ), glPositionCode + motionVectorCode
            )
        }
    }

    /**
     * applies skeletal animation onto the vertex, if needed
     * */
    fun animateVertex(key: ShaderKey): List<ShaderStage> {
        val flags = key.flags
        if (!flags.hasFlag(IS_ANIMATED)) return emptyList()
        val stage = ShaderStage("v-anim", createAnimVariables(key), animCode0())
        if (useAnimTextures) stage.add(getAnimMatrix)
        return listOf(stage)
    }

    open fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        return createDefines(key) +
                loadVertex(key) +
                animateVertex(key) +
                transformVertex(key) +
                finishVertex(key)
    }

    fun f(stage: ShaderStage, condition: Boolean): List<ShaderStage> {
        return if (condition) listOf(stage)
        else emptyList()
    }

    fun f(list: List<ShaderStage>, condition: Boolean): List<ShaderStage> {
        return if (condition) list
        else emptyList()
    }

    open fun createFragmentVariables(key: ShaderKey): ArrayList<Variable> {
        val flags = key.flags
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
            Variable(GLSLType.V1F, "finalReflectivity", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalOcclusion", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalSheen", VariableMode.OUT),
            // just passed from uniforms
            Variable(GLSLType.V1F, "finalTranslucency", VariableMode.INOUT),
            Variable(GLSLType.V4F, "finalClearCoat", VariableMode.INOUT),
            Variable(GLSLType.V2F, "finalClearCoatRoughMetallic", VariableMode.INOUT),
            Variable(GLSLType.V1F, "lodBias"),
            Variable(GLSLType.V2F, "jitterInPixels"),
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
            Variable(GLSLType.V4F, "cameraRotation")
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

    // Parallax-Try:
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

    // just like the gltf pbr shader define all material properties
    open fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + key.instanceData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        jitterUVCorrection +
                        // step by step define all material properties
                        baseColorCalculation +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            createColorFragmentStage()
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity)
        )
    }

    fun createColorFragmentStage(): String {
        return normalTanBitanCalculation +
                normalMapCalculation +
                emissiveCalculation +
                occlusionCalculation +
                metallicCalculation +
                roughnessCalculation +
                v0 + sheenCalculation +
                clearCoatCalculation +
                reflectionCalculation
    }

    override fun createForwardShader(key: ShaderKey): Shader {
        val shader = createBase(key).create(key, "fwd${key.flags}-${key.renderer.nameDesc.englishName}")
        return finish(shader, key)
    }

    override fun createDeferredShader(key: ShaderKey): Shader {
        val base = createBase(key)
        base.settings = key.renderer.deferredSettings
        // build & finish
        val shader = base.create(key, "def${key.flags}-${key.renderer.nameDesc.englishName}")
        return finish(shader, key)
    }
}