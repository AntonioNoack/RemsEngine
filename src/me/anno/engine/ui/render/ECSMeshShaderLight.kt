package me.anno.engine.ui.render

import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.ecs.components.anim.BoneData.maxBones
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.ECSMeshShader.Companion.animCode0
import me.anno.engine.ui.render.ECSMeshShader.Companion.baseColorCalculation
import me.anno.engine.ui.render.ECSMeshShader.Companion.discardByCullingPlane
import me.anno.engine.ui.render.ECSMeshShader.Companion.finalMotionCalculation
import me.anno.engine.ui.render.ECSMeshShader.Companion.getAnimMatrix
import me.anno.engine.ui.render.ECSMeshShader.Companion.glPositionCode
import me.anno.engine.ui.render.ECSMeshShader.Companion.motionVectorCode
import me.anno.engine.ui.render.ECSMeshShader.Companion.normalCalculation
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.input.Input
import me.anno.io.files.InvalidRef
import me.anno.utils.types.Booleans.hasFlag

// todo test whether this delivers any performance gain on Android
//  if not, maybe remove it
open class ECSMeshShaderLight(name: String) : BaseShader(name, "", emptyList(), "") {

    companion object {
        fun Material.canUseLightShader(): Boolean {
            return shader == null && shaderOverrides.isEmpty() &&
                    emissiveBase.is000() && emissiveMap == InvalidRef &&
                    normalMap == InvalidRef && sheen == 0f && pipelineStage == PipelineStage.OPAQUE &&
                    roughnessMap == InvalidRef && roughnessMinMax.y > 0.7f &&
                    metallicMap == InvalidRef && metallicMinMax.x == 0f &&
                    occlusionMap == InvalidRef && !Input.isShiftDown
        }
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
        builder.ignored.addAll(
            ("worldScale,cameraPosition,cameraRotation,numberOfLights," +
                    "IOR,hasAnimation,prevTransform,applyToneMapping").split(',')
        )
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
            // input varyings
            Variable(GLSLType.V2F, "uv", VariableMode.INOUT),
            Variable(GLSLType.V3F, "normal"),
            Variable(GLSLType.V4F, "vertexColor0"),
            Variable(GLSLType.V3F, "finalPosition"),
            Variable(GLSLType.V4F, "diffuseBase"),
            Variable(GLSLType.V4F, "reflectionCullingPlane"),
            // outputs
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
            // just passed from uniforms
            Variable(GLSLType.V1F, "lodBias"),
            // for reflections;
            // we could support multiple
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

    // just like the gltf pbr shader define all material properties
    open fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        baseColorCalculation +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            normalCalculation
                        } else "") +
                        finalMotionCalculation
            )
        )
    }

    override fun createForwardShader(key: ShaderKey): Shader {
        val shader = createBase(key).create(key, "l-fwd${key.flags}-${key.renderer.nameDesc.englishName}")
        finish(shader)
        return shader
    }

    override fun createDeferredShader(key: ShaderKey): Shader {
        val base = createBase(key)
        base.settings = key.renderer.deferredSettings
        // build & finish
        val shader = base.create(key, "l-def${key.flags}-${key.renderer.nameDesc.englishName}")
        finish(shader)
        return shader
    }
}