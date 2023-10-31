package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.mesh.*
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.hasFlag
import me.anno.utils.OS.downloads
import org.joml.Vector3f

/**
 * implement shell texturing like in https://www.youtube.com/watch?v=9dr-tRQzij4 by Acerola
 * */
fun main() {
    val mesh = MeshCache[downloads.getChild("3d/bunny.obj")]!!.clone() as Mesh
    mesh.calculateNormals(true) // clone and recalculating normals, because the bunny file, I have, has flat normals
    val scene = Entity()
    scene.add(FurMeshRenderer(mesh))
    testSceneWithUI("Shell Textures", scene)
}

object FurShader : ECSMeshShader("Fur") {
    override fun createVertexStages(flags: Int): List<ShaderStage> {
        // return super.createVertexStages(flags)
        // extrude by hair length along normals
        val defines = createDefines(flags)
        val variables = createVertexVariables(flags) + listOf(
            Variable(GLSLType.V1F, "hairLength"),
            Variable(GLSLType.V1F, "relativeHairLength"),
            Variable(GLSLType.V3F, "hairGravity"),
            Variable(GLSLType.V1I, "instanceId", VariableMode.OUT)
        )
        val stage = ShaderStage(
            "vertex",
            variables, defines.toString() +
                    "instanceId = gl_InstanceID;\n" +
                    "float instanceIdF = float(instanceId);\n" +
                    // normalization is better to keep the hair length unaffected by gravity
                    // this square dependency on the relative height makes nice curves
                    "localPosition = coords + normalize(normals + hairGravity * (instanceIdF * relativeHairLength)) * instanceIdF * hairLength;\n" +
                    motionVectorInit +

                    instancedInitCode +

                    animCode0() +
                    normalInitCode +
                    animCode1 +

                    applyTransformCode +
                    colorInitCode +
                    "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                    motionVectorCode +
                    ShaderLib.positionPostProcessing +
                    "localPosition = coords;\n" // restore for seed finding
        )
        if (flags.hasFlag(IS_ANIMATED) && AnimTexture.useAnimTextures) stage.add(getAnimMatrix)
        if (flags.hasFlag(USES_PRS_TRANSFORM)) stage.add(ShaderLib.quatRot)
        return listOf(stage)
    }

    override fun createFragmentStages(flags: Int): List<ShaderStage> {
        // discards pixels that don't belong to the currently processed hair
        val furDiscardStage = ShaderStage(
            "furStage", listOf(
                Variable(GLSLType.V1F, "hairDensity"),
                Variable(GLSLType.V1F, "relativeHairLength"),
                Variable(GLSLType.V1F, "hairSharpness"),
                Variable(GLSLType.V3F, "localPosition"),
                Variable(GLSLType.V3F, "normal"),
                Variable(GLSLType.V1I, "instanceId")
            ), "" +
                    // discard if not hair
                    "if(instanceId > 0) {\n" +
                    "   vec3 hairSeed0 = localPosition * hairDensity;\n" +
                    "   vec3 hairSeed = round(hairSeed0);\n" +
                    "   vec3 hairSeedFract = (hairSeed0 - hairSeed) * hairSharpness;\n" +
                    "   vec3 normal1 = gl_FrontFacing ? normal : -normal;\n" +
                    // orthogonalize to normal
                    "   hairSeedFract -= dot(normalize(normal1), hairSeedFract);\n" +
                    "   float hairRandom = fract(sin(dot(hairSeed, vec3(1.29898, 0.41414, 0.95153))) * 43758.5453);\n" +
                    "   if(hairRandom * (1.0 - length(hairSeedFract)) < float(instanceId) * relativeHairLength) discard;\n" +
                    "}\n"
        )
        // increase natural occlusion at the bottom
        val furOcclusionStage = ShaderStage(
            "furOcclusion", listOf(
                Variable(GLSLType.V1F, "relativeHairLength"),
                Variable(GLSLType.V1F, "hairSharpness"),
                Variable(GLSLType.V1I, "instanceId"),
                Variable(GLSLType.V1F, "finalTranslucency"),
                Variable(GLSLType.V1F, "finalOcclusion", VariableMode.OUT)
            ), "" +
                    "float hairHeight = float(instanceId) * relativeHairLength;\n" +
                    "finalOcclusion = (1.0 - finalTranslucency) * (1.0 - hairHeight) / (1.2 + 0.7 * hairSharpness);\n"
        )
        return listOf(furDiscardStage) + super.createFragmentStages(flags) + furOcclusionStage
    }
}

class FurMeshRenderer(var meshInstance: Mesh) : MeshComponentBase() {

    @Range(1.0, 1024.0)
    var numShells = 64

    @Range(0.0, 1e38)
    var hairLength = 0.01f

    @Range(0.0, 1e38)
    var hairDensity = 3000f

    @Range(0.0, 100.0)
    var hairSharpness = 1.5f

    @Range(-1.0, 1.0)
    var hairGravity = Vector3f(0f, -0.5f, 0f)
        set(value) {
            field.set(value)
        }

    @Suppress("SetterBackingFieldAssignment")
    var material = Material()
        set(value) {
            value.copyInto(field)
        }

    init {
        meshInstance.materials = meshInstance.materials.map { material.ref }
        material.shaderOverrides["relativeHairLength"] = TypeValue(GLSLType.V1F) { 1f / numShells }
        material.shaderOverrides["hairLength"] = TypeValue(GLSLType.V1F) { hairLength / numShells }
        material.shaderOverrides["hairGravity"] = TypeValue(GLSLType.V3F, hairGravity)
        material.shaderOverrides["hairDensity"] = TypeValue(GLSLType.V1F) { hairDensity }
        material.shaderOverrides["hairSharpness"] = TypeValue(GLSLType.V1F) { hairSharpness }
        material.shader = FurShader
    }

    override fun getMeshOrNull(): Mesh {
        meshInstance.proceduralLength = numShells
        return meshInstance
    }
}