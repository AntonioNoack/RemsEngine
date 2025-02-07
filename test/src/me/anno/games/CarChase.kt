package me.anno.games

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.Vehicle
import me.anno.bullet.VehicleWheel
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.Updatable
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.CullMode
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.pipeline.PipelineStage
import me.anno.input.Input
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.tests.physics.TestVehicleController
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.documents
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.PI

// todo simple car chase game
// done - car
// done - streets
// done - proper car-centric camera
// done - drive
// todo - ai drives
// todo - minimap
// todo - navigation
// todo - police cars chasing you
// todo - goals, like destroying things
// todo - shooting to destroy stuff :)
// todo - health bars
// todo - exiting the car, and walking

// controls: TFGH, space, just like in the test vehicle controller

val map = documents.getChild("CarChase.glb")

// todo: this probably should be created in the editor; there, we have much better control
val heistPackage = getReference("E:/Assets/Unity/POLYGON_Heist_Unity_Package_2017_1.unitypackage/Assets/PolygonHeist")
val carModel = heistPackage.getChild("Model/SM_Veh_Car_Police_Heist_01.fbx")
val carModelMain = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_01.json")
val carModelFL = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_Wheel_fl.json")
val carModelSteer = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_SteeringW.json")
val carModelGlass = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_Glass.json")
val carModelPlates = carModel.getChild("meshes/SM_Veh_Car_Police_Heist_Plates.json")
val carModelMat = heistPackage.getChild("Materials/PolygonHeist_Character_Material_01.mat")

fun createUI(): Panel {

    ECSRegistry.init()

    val list = NineTilePanel(style)

    val world = Entity()
    world.add(Skybox())
    val physics = BulletPhysics()
    physics.updateInEditMode = true
    physics.synchronousPhysics = true
    Systems.registerSystem(physics)

    val floor = Entity()
    floor.add(MeshComponent(map))
    floor.add(MeshCollider(map).apply { isConvex = false })
    floor.add(Rigidbody().apply { mass = 0.0 })
    world.add(floor)

    val car0 = Entity()
    val car1 = Entity()
    car1.setPosition(0.0, -0.8, 0.0)
    car1.setScale(100.0) // the car somehow is in cm size... even in Blender, so not a bug on our side
    val materialList = listOf(carModelMat)
    fun add(entity: Entity, source: FileReference, matList: List<FileReference> = materialList) {
        val comp = MeshComponent(source)
        comp.materials = matList
        entity.add(comp)
    }
    add(car1, carModelMain)
    add(car1, carModelPlates)

    val glassMaterial = Material()
    glassMaterial.diffuseBase.w = 0.7f
    glassMaterial.cullMode = CullMode.BOTH
    glassMaterial.pipelineStage = PipelineStage.TRANSPARENT
    glassMaterial.metallicMinMax.set(1f)
    glassMaterial.roughnessMinMax.set(0.1f)
    add(car1, carModelGlass, listOf(glassMaterial.ref))

    car0.add(car1)
    world.add(car0)

    val player = LocalPlayer()
    LocalPlayer.currentLocalPlayer = player

    val vehicle = Vehicle()
    // vehicle.deleteWhenKilledByDepth = true
    car0.setPosition(0.0, 3.0, 0.0) // move car into air
    vehicle.centerOfMass.set(0.0, -0.3, 0.0)
    car0.add(vehicle)
    car0.add(
        Entity("CarCollider")
            .setPosition(car1.position)
            .setScale(car1.scale)
            .add(MeshCollider(carModelMain))
    )
    val controller = TestVehicleController()
    controller.controls = "wasd"
    car0.add(controller)

    val steeringWheel = Entity()
    val steeringWheelMesh = MeshComponent(carModelSteer)
    steeringWheelMesh.materials = materialList
    steeringWheel.add(steeringWheelMesh)
    steeringWheel.add(object : Component(), OnUpdate {
        val q = Quaterniond()
        val c = Vector3d()
        override fun onUpdate() {
            val tr = steeringWheel.transform
            val mesh = steeringWheelMesh.getMeshOrNull()?.getBounds() ?: return
            q.rotationZ(-5.0 * controller.lastSteering)
            c.set(mesh.centerX.toDouble(), mesh.centerY.toDouble(), mesh.centerZ.toDouble())
            tr.setOffsetForLocalRotation(q, c)
        }
    })
    car1.add(steeringWheel)


    // add four wheels :)
    for (x in listOf(-1, +1)) {
        for (z in listOf(-1, +1)) {
            val wheelObj = Entity()
            val wheel = VehicleWheel()
            wheel.suspensionRestLength = 0.1
            wheel.suspensionStiffness = 50.0
            wheel.suspensionDampingRelaxation = 0.95
            wheel.maxSuspensionTravelCm = 20.0
            wheel.brakeForceMultiplier = 0.02 // what unit is this value??? why has it to be that low???
            wheelObj.add(wheel)
            wheel.radius = 0.42678
            wheelObj.setPosition(x * 0.9, -0.3, if (z > 0) 1.58 else -1.48)
            val wheelMesh = Entity()
            wheelMesh.add(MeshComponent(carModelFL).apply { materials = materialList })
            if (x > 0) {
                wheelMesh.setPosition(0.86348, -0.42678, 1.5698)
                wheelMesh.setRotation(0.0, PI, 0.0)
            } else {
                wheelMesh.setPosition(-0.86348, -0.42678, -1.5698)
            }
            wheelMesh.scale = wheelMesh.scale.set(100.0)
            wheelObj.add(wheelMesh)
            if (z < 0) wheel.steeringMultiplier = 0.0
            car0.add(wheelObj)
        }
    }

    EditorState.prefabSource = world.ref
    val sceneView = SceneView(PlayMode.PLAYING, style)
    val renderView = sceneView.renderView
    renderView.renderMode = RenderMode.PHYSICS
    renderView.localPlayer = player
    sceneView.weight = 1f
    list.add(sceneView)

    val camEntity = Entity()
    val camBase = Entity()
    val camera = Camera()
    camera.use()
    camera.use()

    // orbit controls for the camera around the car :)
    camEntity.add(camera)
    val orbitControls = object : OrbitControls(), OnUpdate {
        override fun onUpdate() {
            if (Input.isKeyDown('R')) {
                // reset car
                // todo not working: can we rotate the car towards upwards? :)
                /*val tr = vehicle.transform!!
                tr.globalRotation = tr.globalRotation.identity()
                tr.globalPosition = tr.globalPosition.set(0.0, 1.0, 0.0)
                vehicle.invalidatePhysics()*/
            }
        }
    }
    // todo before moving the camera with mouse movement, capture the mouse :)
    // movement is disabled
    orbitControls.movementSpeed = 0.0
    orbitControls.camera = camera
    orbitControls.useGlobalSpace = true // less nauseating, when car is rotated
    camBase.add(orbitControls)
    camEntity.setRotation(0.0, PI, 0.0)
    camBase.setPosition(0.4, 0.57, 0.15)
    camBase.add(camEntity)
    car0.add(camBase)

    val speedometer = UpdatingTextPanel(100, style) {
        // calculate velocity along forward axis
        "${(vehicle.localVelocityZ * 3.6).roundToIntOr(-42)} km/h"
    }
    speedometer.alignmentX = AxisAlignment.CENTER
    speedometer.alignmentY = AxisAlignment.MAX
    list.add(speedometer)

    world.validateTransform()

    return list
}

// todo this is currently broken
fun main() {
    // todo bug: why are meshes and materials not automatically reloading?
    disableRenderDoc()
    OfficialExtensions.initForTests()
    testUI3("CarChase") { createUI() }
}