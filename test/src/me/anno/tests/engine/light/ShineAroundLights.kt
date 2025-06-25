package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.posMod
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.toRadians
import kotlin.math.cos
import kotlin.math.sin

/**
 * light shine around lamps;
 *  particles in air, and viewing ray really close to light source
 * todo stray particles, which float through the air
 * */
fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity()
    Entity("Floor", scene)
        .add(MeshComponent(plane))
    Entity("Light", scene)
        .add(SpotLight().apply { color.set(5f) })
        .add(MeshComponent(pyramidZMesh[1.0f], spotLightMaterial))
        .add(MeshComponent(pyramidZMesh[0.5f], spotLightMaterial))
        .setPosition(0.0, 0.5, 0.0)
        .setRotation((-90f).toRadians(), 0f, 0f)
    testSceneWithUI("Shine around Light", scene) {
        it.renderView.pipeline.stages
    }
}

object ShineAroundLightsShader : ECSMeshShader("ShineAroundLights") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key) + listOf(
                    Variable(GLSLType.V3F, "localPosition"),
                ), concatDefines(key).toString() +
                        discardByCullingPlane + // is this needed?
                        // todo blending depending on angle to center, using inner and outer angle like the cone itself
                        //  then we can reduce the number of polygons in the Mesh massively, too

                        "finalColor = diffuseBase.rgb;\n" +
                        "float alpha = diffuseBase.a * pow(1.0 + localPosition.z, 2.0);\n" +
                        "finalAlpha = alpha;\n" +

                        "#define MODULATE_ALPHA\n" +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            "" +
                                    "finalNormal = vec3(0.577);\n" +
                                    "finalEmissive = alpha * emissiveBase;\n" +
                                    "finalMetallic = 0.0;\n" +
                                    "finalRoughness = 1.0;\n"
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot)
        )
    }
}

val spotLightMaterial = Material().apply {
    pipelineStage = PipelineStage.GLASS
    diffuseBase.set(1f, 1f, 1f, 0.5f)
    emissiveBase.set(1f)
    indexOfRefraction = 1f
    shader = ShineAroundLightsShader
}

val pyramidZMesh = LazyMap { tanAngle: Float ->
    Mesh().apply {
        val n = 64
        val positions = FloatArray((n + 1) * 3)
        for (i in 0 until n) {
            val angle = i * TAUf / n
            positions[i * 3] = cos(angle) * tanAngle
            positions[i * 3 + 1] = sin(angle) * tanAngle
            positions[i * 3 + 2] = -1f
        }
        this.positions = positions
        val indices = IntArray(n * 3)
        for (i in 0 until n) {
            indices[i * 3] = i
            indices[i * 3 + 1] = n
            indices[i * 3 + 2] = posMod(i + 1, n)
        }
        this.indices = indices
    }
}