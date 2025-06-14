package me.anno.games.roadcraft

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.Hierarchy.getInstanceAt
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.maths.Maths.fract
import me.anno.utils.OS.documents
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toFloat
import org.joml.Vector2f
import org.joml.Vector3f

object TrackShader : ECSMeshShader("Track") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key) +
                        Variable(GLSLType.V1F, "trackScrolling"),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        "uv.y -= trackScrolling;\n" +
                        // step by step define all material properties
                        baseColorCalculation +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            createColorFragmentStage()
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity)
        )
    }
}

class TrackVehicleControls : Component(), OnUpdate {

    var speed = 2f
    var visualScale = 1.95f * 1.3f // 1.95 is about perfect, *1.3 is for friction-loss
    val trackPosition = Vector2f()

    override fun onUpdate() {
        val transform = transform ?: return

        val dt = Time.deltaTime.toFloat()

        // pure track controls are weird
        val w = Input.isKeyDown(Key.KEY_U).toFloat()
        val a = Input.isKeyDown(Key.KEY_H).toFloat()
        val s = Input.isKeyDown(Key.KEY_J).toFloat()
        val d = Input.isKeyDown(Key.KEY_K).toFloat()

        val left = (w + a - s - d) * dt
        val right = (w - a - s + d) * dt

        val trackDistance = 2f

        trackPosition.x = fract(trackPosition.x + left * visualScale)
        trackPosition.y = fract(trackPosition.y + right * visualScale)

        transform.localRotation = transform.localRotation
            .rotateY((left - right) * speed / trackDistance)

        val forward = transform.getLocalForward(1f, Vector3f())
        transform.localPosition = transform.localPosition
            .add(forward * ((left + right) * speed))

        invalidateAABB()
    }
}

fun main() {

    val project = documents.getChild("RemsEngine/Construction")
    workspace = project

    val meshFile = project.getChild("Vehicles/SM_Veh_Bulldozer_01.json")
    val instance = PrefabCache[meshFile]!!.createInstance() as Entity

    val trackL = getInstanceAt(instance, "e7,SM_Veh_Bulldozer_01_Track_l/e1,MeshFilter") as MeshComponent
    val trackR = getInstanceAt(instance, "e8,SM_Veh_Bulldozer_01_Track_r/e1,MeshFilter") as MeshComponent

    fun createMaterial(trackPosition: () -> Float): FileReference {
        val baseMaterial = project.getChild("Vehicles/materials/PolygonConstruction_Mat_Track.json")
        val prefab = Prefab()
        prefab.parentPrefabFile = baseMaterial
        prefab["shader"] = TrackShader
        prefab["shaderOverrides"] = hashMapOf("trackScrolling" to TypeValue(GLSLType.V1F, trackPosition))
        return prefab.getTmpRef()
    }

    val controls = TrackVehicleControls()
    trackL.materials = listOf(createMaterial { controls.trackPosition.x })
    trackR.materials = listOf(createMaterial { controls.trackPosition.y })
    instance.add(controls)

    testSceneWithUI("Tracks", instance)
}