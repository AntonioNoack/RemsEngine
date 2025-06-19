package me.anno.games.roadcraft

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.Vehicle
import me.anno.bullet.VehicleWheel
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.Hierarchy.getInstanceAt
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets.plane
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
import me.anno.maths.Maths.dtTo10
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.OS.documents
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toFloat
import org.joml.Vector2d
import org.joml.Vector4f

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

class TrackVehicleControls : Component(), OnUpdate, OnPhysicsUpdate {

    var speed = 2f
    val trackPosition = Vector2d()

    var strength = 300.0
    var turnStrength = 300.0

    var targetTorque = 0.0
    var torque = 0.0

    override fun onUpdate() {

        val vehicle = getComponent(Vehicle::class) ?: return

        // pure track controls are weird
        val w = Input.isKeyDown(Key.KEY_U).toFloat()
        val a = Input.isKeyDown(Key.KEY_H).toFloat()
        val s = Input.isKeyDown(Key.KEY_J).toFloat()
        val d = Input.isKeyDown(Key.KEY_K).toFloat()

        val left = (w + a - s - d)
        val right = (w - a - s + d)

        val isTurning = left != right && w + a + s + d > 0f

        var actualRight = 0.0
        var actualLeft = 0.0
        for (wheel in vehicle.wheels) {
            val isRight = wheel.steeringMultiplier > 0.0
            val control = if (isRight) right else left
            wheel.steering = 0.0
            wheel.engineForce = strength * control
            wheel.brakeForce = if (control == 0f && !isTurning) strength else 0.0
            wheel.frictionSlip = if (isTurning) 0.01 else 0.5
            if (isRight) actualRight += wheel.rotation
            else actualLeft += wheel.rotation
        }

        // wheel aren't strong enough to turn the bulldozer :/
        // todo test whether additional invisible wheels help
        targetTorque = if (isTurning) {
            turnStrength * (left - right)
        } else 0.0

        trackPosition.x = actualLeft
        trackPosition.y = actualRight

        invalidateAABB()
    }

    override fun onPhysicsUpdate(dt: Double) {
        val vehicle = getComponent(Vehicle::class) ?: return
        val effectiveness = 1.0 / (1.0 + 5.0 * sq(vehicle.globalAngularVelocity.y))
        torque = mix(torque, targetTorque * effectiveness, dtTo10(dt))
        vehicle.applyTorque(0.0, torque, 0.0)
    }
}

fun main() {

    val project = documents.getChild("RemsEngine/Construction")
    workspace = project

    val physics = BulletPhysics()
    physics.updateInEditMode = true
    registerSystem(physics)

    val scene = Entity("Scene")
    if (true) {
        Entity("Floor", scene)
            .add(MeshComponent(plane))
            .add(InfinitePlaneCollider())
            .add(Rigidbody().apply { mass = 0.0 })
            .setScale(20f)
    } else {
        val perlin = PerlinNoise(
            1324, 5, 0.5f,
            -3f, 3f, Vector4f(0.1f)
        )
        createTerrain(100, 100, 1f, perlin, scene)
    }

    val meshFile = project.getChild("Vehicles/SM_Veh_Bulldozer_01.json")
    val bulldozer = PrefabCache[meshFile].waitFor()!!.createInstance() as Entity
    // todo add collision and rigidbody
    bulldozer.add(Vehicle().apply { mass = 100.0 })
    bulldozer.setPosition(0.0, 0.2, 0.0)
    scene.add(bulldozer)

    fun defineWheel(path: String, s: Double) {
        val wheel = getInstanceAt(bulldozer, path) as Entity
        wheel.add(VehicleWheel().apply {
            radius = 0.8
            steeringMultiplier = s
            engineForce = 1.0
            suspensionRestLength = 0.0
            maxSuspensionTravel = 0.5
            frictionSlip = 0.5 // max impulse / suspension force until the vehicle slips
            suspensionStiffness = 100.0
            // todo why does it start jumping when it's looking 90° rotated to the right???
            suspensionDampingRelaxation = 5.0
            suspensionDampingCompression = 5.0
            rollInfluence = 0.25
        })
    }

    defineWheel("e9,SM_Veh_Bulldozer_01_Track_Wheel_fl", 1.0)
    defineWheel("e10,SM_Veh_Bulldozer_01_Track_Wheel_fr", -1.0)
    defineWheel("e12,SM_Veh_Bulldozer_01_Track_Wheel_rl", 1.0)
    defineWheel("e13,SM_Veh_Bulldozer_01_Track_Wheel_rr", -1.0)


    val trackL = getInstanceAt(bulldozer, "e7,SM_Veh_Bulldozer_01_Track_l/e1,MeshFilter") as MeshComponent
    val trackR = getInstanceAt(bulldozer, "e8,SM_Veh_Bulldozer_01_Track_r/e1,MeshFilter") as MeshComponent

    fun createMaterial(trackPosition: () -> Float): FileReference {
        val baseMaterial = project.getChild("Vehicles/materials/PolygonConstruction_Mat_Track.json")
        val prefab = Prefab()
        prefab.parentPrefabFile = baseMaterial
        prefab["shader"] = TrackShader
        prefab["shaderOverrides"] = hashMapOf("trackScrolling" to TypeValue(GLSLType.V1F, trackPosition))
        return prefab.getTmpRef()
    }

    val controls = TrackVehicleControls()
    trackL.materials = listOf(createMaterial { fract(controls.trackPosition.x).toFloat() })
    trackR.materials = listOf(createMaterial { fract(controls.trackPosition.y).toFloat() })
    bulldozer.add(controls)

    testSceneWithUI("Tracks", scene)
}