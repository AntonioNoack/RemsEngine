package me.anno.games.carchase

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.KinematicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.bodies.Vehicle
import me.anno.bullet.bodies.VehicleWheel
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.render.SceneView.Companion.createSceneUI
import me.anno.gpu.CullMode
import me.anno.gpu.pipeline.PipelineStage
import me.anno.input.Input
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.PIf
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS
import me.anno.utils.OS.documents
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Quaternionf
import org.joml.Vector3d

// todo simple car chase game
// done - car
// done - streets
// done - proper car-centric camera
// done - drive
// todo - ai drives
// done - minimap
// todo - navigation
// todo - police cars chasing you
// todo - goals, like destroying things
// todo - shooting to destroy stuff :)
// todo - health bars
// todo - exiting the car, and walking

val map = documents.getChild("CarChase.glb")

val assets =
    if (OS.isWindows) getReference("E:/Assets")
    else getReference("/media/antonio/4TB WDRed/Assets")

// todo: this probably should be created in the editor; there, we have much better control
val heistPackage = assets.getChild("Unity/Polygon/Heist.unitypackage/Assets/PolygonHeist")

val carModel = heistPackage.getChild("Model/SM_Veh_Car_Police_Heist_01.fbx")
val carModelMain = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_01-002.json")
val carModelFL = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_Wheel_fl-002.json")
val carModelSteer = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_SteeringW-002.json")
val carModelGlass = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_Glass-002.json")
val carModelPlates = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_Plates-002.json")
val carModelMat = heistPackage.getChild("Materials/PolygonHeist_Character_Material_01.mat")

fun sign(x: Int): Int {
    return if (x > 0) 1 else -1
}

fun createUI(): Panel {

    val physics = BulletPhysics().apply {
        // updateInEditMode = true
        synchronousPhysics = true
        enableDebugRendering = true
    }
    Systems.registerSystem(physics)

    val scene = Entity()
    Entity("Floor", scene)
        .add(MeshComponent(map))
        .add(MeshCollider(map).apply {
            maxNumVertices = 0
            isConvex = false
        })
        .add(InfinitePlaneCollider())
        .add(StaticBody())

    val glassMaterial = Material().apply {
        diffuseBase.w = 0.7f
        cullMode = CullMode.BOTH
        pipelineStage = PipelineStage.GLASS
        metallicMinMax.set(1f)
        roughnessMinMax.set(0.1f)
    }

    val carScale = 100f
    val carVisuals = Entity("CarVisuals")
        .setPosition(0.0, -0.8, 0.0)
        .setScale(carScale) // the car somehow is in cm size... even in Blender, so not a bug on our side
        .add(MeshComponent(carModelMain, carModelMat))
        .add(MeshComponent(carModelPlates, carModelMat))
        .add(MeshComponent(carModelGlass, glassMaterial))

    val vehicle = Vehicle().apply {
        centerOfMass.set(0.0, -0.3, 0.0)
    }

    val controller = VehicleController()
    val carEntity = Entity("PoliceCar", scene)
        // vehicle.deleteWhenKilledByDepth = true
        .setPosition(0.0, 0.7, 0.0) // move car into air
        .add(vehicle)
        .add(controller)
        .add(carVisuals)

    Entity("CarCollider", carEntity)
        .setPosition(carVisuals.position)
        .setScale(carVisuals.scale)
        .add(MeshCollider(carModelMain))

    val player = LocalPlayer()
    LocalPlayer.currentLocalPlayer = player

    class SteeringWheelVisuals : Component(), OnUpdate {
        val q = Quaternionf()
        val c = Vector3d()
        override fun onUpdate() {
            val tr = transform ?: return
            val meshComp = getComponent(MeshComponent::class)
            val mesh = meshComp?.getMeshOrNull()?.getBounds() ?: return
            q.rotationZ(-5f * controller.lastSteering.toFloat())
            c.set(mesh.centerX.toDouble(), mesh.centerY.toDouble(), mesh.centerZ.toDouble())
            tr.setOffsetForLocalRotation(q, c)
        }
    }

    Entity("Steering Wheel", carVisuals)
        .add(MeshComponent(carModelSteer, carModelMat))
        .add(SteeringWheelVisuals())

    // add four wheels :)
    for (x in listOf(-1, +1)) {
        for (z in listOf(-1, +1)) {
            val wheel = VehicleWheel().apply {
                suspensionRestLength = -0.15f
                suspensionStiffness = 50.0f
                suspensionDampingRelaxation = 0.95f
                maxSuspensionTravel = 0.2f
                brakeForceMultiplier = 0.02f // what unit is this value??? why has it to be that low???
                radius = 0.42678f
            }
            val wheelObj = Entity("Wheel[$x,$z]")
                .add(wheel)
                .setPosition(x * 0.9, -0.3, if (z > 0) 1.58 else -1.48)
            val wheelMesh = Entity("WheelMesh", wheelObj)
                .setScale(carScale)
                .add(MeshComponent(carModelFL, carModelMat))
                .setPosition(sign(x) * 0.86348, -0.42678, sign(x) * 1.5698)
            if (x > 0) {
                wheelMesh.setRotation(0f, PIf, 0f)
            }
            if (z < 0) wheel.steeringMultiplier = 0.0f
            carEntity.add(wheelObj)
        }
    }

    Systems.world = scene
    val sceneView = SceneView(PlayMode.PLAYING, style)
    val renderView = sceneView.renderView
    renderView.localPlayer = player
    sceneView.weight = 1f

    val list = NineTilePanel(style)
    list.add(sceneView)

    val camera = Camera()
    val camEntity = Entity()
        .setRotation(0f, PIf, 0f)
        .add(camera)

    camera.use()
    camera.use()

    // orbit controls for the camera around the car :)
    val orbitControls = object : OrbitControls(), OnUpdate {
        init {
            // movement is disabled
            movementSpeed = 0.0
            this.camera = camera
            useGlobalSpace = true // less nauseating, when car is rotated
        }

        override fun onUpdate() {
            if (Input.isKeyDown('R')) {
                // reset car
                val tr = vehicle.transform!!
                tr.localRotation = tr.localRotation.identity()
                tr.localPosition = tr.localPosition.set(0.0, 1.0, 0.0)
                vehicle.invalidatePhysics()
            }
        }
    }

    // todo before moving the camera with mouse movement, capture the mouse :)

    Entity("Controls & Camera", carEntity)
        .add(orbitControls)
        .setPosition(0.4, 0.57, 0.15)
        .add(camEntity)

    Entity("Ramp", scene)
        .add(MeshComponent(flatCube))
        .add(BoxCollider())
        .add(KinematicBody())
        .setPosition(1.0, -0.7, 5.0)
        .setRotationDegrees(0f, 0f, 15f)

    // speedometer
    list.add(UpdatingTextPanel(100, style) {
        // calculate velocity along forward axis
        "${(vehicle.localLinearVelocityZ * 3.6).roundToIntOr()} km/h"
    }.apply {
        alignmentX = AxisAlignment.CENTER
        alignmentY = AxisAlignment.MAX
    })

    list.add(MinimapPanel().apply {
        alignmentX = AxisAlignment.MIN
        alignmentY = AxisAlignment.MAX
    })

    scene.validateTransform()

    if (true) {
        return createSceneUI(scene)
    }

    return list
}

fun main() {
    OfficialExtensions.initForTests()
    testUI3("CarChase") { createUI() }
}