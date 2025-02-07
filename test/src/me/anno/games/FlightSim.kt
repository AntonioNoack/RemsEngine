package me.anno.games

import me.anno.Time
import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.AutoTileableMaterial
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mix
import me.anno.maths.chunks.PlayerLocation
import me.anno.maths.chunks.cartesian.SingleChunkSystem
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.OS
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toRadians
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

// inspired by Vazgriz, https://www.youtube.com/watch?v=7vAHo2B1zLc
fun main() {

    RenderDoc.forceLoadRenderDoc()
    OfficialExtensions.initForTests()

    val scene = Entity("Scene")

    val player = LocalPlayer()

    for (e in createPlane(player)) {
        scene.add(e)
    }

    scene.add(createTerrain(player))

    val physics = BulletPhysics()
    physics.updateInEditMode = true
    Systems.registerSystem(physics)
    scene.add(Skybox())

    testSceneWithUI("FlightSim", scene) {
        it.renderView.localPlayer = player
        // it.renderer.playMode = PlayMode.PLAY_TESTING // to start playing, press the "Play" button
        // it.renderer.renderMode = RenderMode.MOTION_VECTORS
        // todo motion vectors/blur are incorrect
        //  - terrain is zero (moving in frame), moving plane (static in frame) is non-zero
    }
}

fun createTerrain(player: LocalPlayer): Entity {

    val terrain = Entity("Terrain")

    val grassMaterial = AutoTileableMaterial()
    grassMaterial.diffuseMap = OS.pictures.getChild("textures/grass.jpg")
    val grassMatList = listOf(grassMaterial.ref)

    val cellsPerChunkP1 = 64
    val cellsPerChunk = cellsPerChunkP1 - 1
    val cellSize = 50f
    val chunkSize = (cellSize * cellsPerChunk).toDouble()

    val terrainSystem = object : SingleChunkSystem<Entity>() {

        val noise = PerlinNoise(1234L, 7, 0.5f, -500f, 500f, Vector4f(0.025f))
        val empty = Entity()

        override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): Entity {
            if (chunkY != 0) return empty
            val mesh = Mesh()
            val dx = chunkX * cellsPerChunk
            val dz = chunkZ * cellsPerChunk
            TerrainUtils.generateRegularQuadHeightMesh(
                cellsPerChunkP1, cellsPerChunkP1, false, cellSize, mesh,
                { xi, zi ->
                    val x = (xi + dx - 32).toFloat()
                    val z = (zi + dz - 32).toFloat()
                    val amplitude = 1f - 1f / (1f + (x * x + z * z) * 1e-3f)
                    noise.getSmooth(x, z) * amplitude
                })
            mesh.materials = grassMatList
            val wrapper = Entity("E$chunkX/$chunkZ")
                .setPosition(chunkX * chunkSize, 0.0, chunkZ * chunkSize)
                .add(MeshComponent(mesh))
                .add(MeshCollider(mesh).apply { isConvex = false; margin = 0.5 })
                .add(Rigidbody())
            terrain.add(wrapper)
            terrain.invalidateAABBsCompletely()
            return wrapper
        }

        override fun onDestroyChunk(chunk: Entity, chunkX: Int, chunkY: Int, chunkZ: Int) {
            chunk.removeFromParent()
            chunk.destroy()
        }
    }

    // dynamic chunk loading based on plane/camera position
    terrain.add(object : Component(), OnUpdate {
        override fun onUpdate() {
            val pos = player.cameraState.currentCamera!!.transform!!.globalPosition
            terrainSystem.updateVisibility(
                5.0, 10.0, listOf(
                    PlayerLocation(pos.x / chunkSize, 0.0, pos.z / chunkSize)
                )
            )
        }
    })

    return terrain
}

fun createPlane(player: LocalPlayer): List<Entity> {

    val folder = "E:/Assets/Sources/POLYGON_War_Pack_Source_Files.zip"
    val meshFile = getReference("$folder/FBX/SM_Veh_Plane_American_01.fbx")
    val plane0 = PrefabCache[meshFile, false]!!.createInstance() as Entity // "RootNode"
    val plane = plane0.children.first()

    val meshTex = getReference("$folder/Textures/American_Fighter_Texture.png")
    val material = Material()
    material.diffuseMap = meshTex
    val matList = listOf(material.ref)
    plane.forAll {
        if (it is MeshComponent) {
            it.materials = matList
        }
    }

    val body = Rigidbody()
    val rotor = plane.children.first { it.name.startsWith("SM_Veh_Plane_American_01_Prop") }
    rotor.add(object : Component(), OnUpdate {
        var position = 0.0
        var speed = 0.0
        val tmp = Vector3d()
        override fun onUpdate() {
            val dt = Time.deltaTime
            val transform = transform!!
            val lv = body.localLinearVelocity
            val dir = (Input.isShiftDown || Input.isKeyDown(Key.KEY_W)).toInt() - (Input.isKeyDown(Key.KEY_S)).toInt()
            speed = mix(speed, dir * 50.0, dt)
            position += dt * speed
            transform.localRotation = transform.localRotation
                .rotationZ(position)
            // impulse is global, torque probably, too
            val l2g = body.transform!!.globalRotation
            // add lift based on velocity on Z axis
            // add opposing force against localVelocityZ
            val rudder = Input.isKeyDown(Key.KEY_ARROW_UP).toInt() - Input.isKeyDown(Key.KEY_ARROW_DOWN).toInt() // tilt
            val lift = 1.0 * length(lv.y, lv.z)
            val airFriction = 20.0 * dt
            val engineForce = 1000.0 * dt * (speed - 0.3 * lv.z)
            body.applyImpulse(
                l2g.transform(
                    tmp.set(
                        -airFriction * lv.x * abs(lv.x),
                        -airFriction * lv.y * abs(lv.y) + lift * (1f - 0.5f * abs(rudder)),
                        -airFriction * lv.z * abs(lv.z) + engineForce
                    )
                )
            )
            body.angularDamping = 0.9
            // rotation depends on speed along local z axis
            val steering = Input.isKeyDown(Key.KEY_ARROW_LEFT).toInt() - Input.isKeyDown(Key.KEY_ARROW_RIGHT).toInt()
            val rolling = Input.isKeyDown(Key.KEY_D).toInt() - Input.isKeyDown(Key.KEY_A).toInt()
            body.applyTorque(
                l2g.transform(
                    tmp.set(
                        -body.localVelocityX * 10.0 + 20000.0 * rudder,
                        steering * lv.z * 1000.0,
                        rolling * lv.z * 1000.0
                    )/*.add(
                        // rotate plane into straightness -> not working well
                        lv.cross(0.0, 0.0, -50000.0, Vector3d()) *
                                (1.0 - exp(-lv.length() * 0.01))
                    )*/
                )
            )
        }
    })

    // add friction
    body.friction = 0.5

    plane.setPosition(0.0, 2.25, 0.0)

    // add a series of box colliders
    fun addCollider(name: String, pos: Vector3f, halfExt: Vector3f, rx: Float = 0f) {
        val sub = Entity(name)
        sub.position = sub.position.set(pos)
        sub.rotation = sub.rotation.rotateX(rx.toDouble().toRadians())
        val box = BoxCollider()
        box.halfExtends.set(halfExt)
        sub.add(box)
        plane.add(sub)
    }

    addCollider("Wings", Vector3f(0f, -0.54f, 1.06f), Vector3f(5.49f, 0.08f, 0.64f))
    addCollider("Body", Vector3f(0f, +0.00f, 1.81f), Vector3f(0.22f, 0.46f, 2.88f))
    addCollider("Tail", Vector3f(0f, -0.05f, -2.88f), Vector3f(0.13f, 0.48f, 1.83f), 7f)

    addCollider("Left Wheel", Vector3f(-1.75f, -1.43f, 1.88f), Vector3f(0.11f, 0.81f, 0.26f))
    addCollider("Right Wheel", Vector3f(+1.75f, -1.43f, 1.88f), Vector3f(0.11f, 0.81f, 0.26f))
    addCollider("Back Wheel", Vector3f(0f, -0.83f, -2.58f), Vector3f(0.08f, 0.29f, 0.13f))

    body.mass = 1100.0 // taken from Republic P-47 Thunderbolt, which looks similar
    plane.add(body)

    // camera in the cockpit / around the plane
    val controller = OrbitControls()
    controller.needsClickToRotate = true
    controller.rotateRight = true
    controller.rotation.set(0f, PIf, 0f)
    controller.movementSpeed = 0.0
    val base2 = Entity()
    val camera = Camera()
    base2.add(camera)
    controller.camera = camera
    val base0 = Entity()
    val base1 = Entity()
    base1.add(object : Component(), OnUpdate {
        override fun onUpdate() {
            val dst = base0.transform
            val src = plane.transform
            dst.localPosition = dst.localPosition
                .mix(src.localPosition, dtTo01(10f * Time.deltaTime))
            dst.localRotation = dst.localRotation.slerp(
                Quaterniond().rotateY(src.localRotation.getEulerAnglesYXZ(Vector3d()).y),
                dtTo01(2f * Time.deltaTime)
            )
        }
    })
    base1.add(base2)
    base1.add(controller)
    base0.add(base1)
    camera.use(player)
    // EditorState.control = controller
    LocalPlayer.currentLocalPlayer = player

    return listOf(plane, base0)
}
