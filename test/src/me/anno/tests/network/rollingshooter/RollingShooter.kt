package me.anno.tests.network.rollingshooter

import me.anno.Time
import me.anno.bullet.BulletPhysics
import me.anno.bullet.DynamicBody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView.Companion.testScene2
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.maths.Maths.mix
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.network.Instance
import me.anno.tests.network.Player
import me.anno.tests.network.tcpProtocol
import me.anno.tests.network.udpProtocol
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.color.spaces.HSV
import me.anno.utils.Color.black
import me.anno.utils.Color.toRGB
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS.pictures
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LoggerImpl
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Random
import kotlin.math.atan2
import kotlin.math.exp

// create an actual world, where we can walk around with other people
// make the game logic as simple as possible, so it's easy to follow

fun createLighting(scene: Entity) {
    scene.add(Skybox())
    val sun = DirectionalLight()
    sun.color.set(10f)
    sun.shadowMapCascades = 1
    sun.shadowMapResolution = 1024
    sun.autoUpdate = 1
    val sunE = Entity(scene)
    sunE.setScale(70f) // covering the map
    sunE.setRotation((-45f).toRadians(), 0f, 0f)
    sunE.add(sun)
}

fun main() {

    OfficialExtensions.initForTests()

    // todo usable controls 😅
    // done moving
    // todo shooting
    //  - add gun model
    //  - add bullet animation
    // done: - trace bullet path
    // done: - send bullet packet
    // todo hitting
    // todo lives or scores

    disableRenderDoc()

    val scene = Entity()
    Systems.registerSystem(BulletPhysics())
    createLighting(scene)

    val players = Entity("Players", scene)
    val bullets = Entity("Bullet", scene)
    val staticScene = Entity("Static", scene)
    val sphereMesh = UVSphereModel.createUVSphere(32, 16)

    fun createPlayerBase(color: Int): Entity {
        val player = Entity("Player", players)
        player.add(DynamicBody().apply {
            mass = 5.0
            friction = 1.0
            angularDamping = 0.9
            restitution = 0.0
        })
        player.add(SphereCollider().apply { radius = 1f })
        player.add(MeshComponent(sphereMesh).apply {
            materials = listOf(Material().apply {
                (color or black).toVecRGBA(diffuseBase)
                normalMap = pictures.getChild("BricksNormal.png")
            }.ref)
        })
        return player
    }

    val selfName = LoggerImpl.getTimeStamp().toString()

    val playerMap = HashMap<String, Entity>()
    val onPlayerUpdate: (PlayerUpdatePacket) -> Unit = { p ->
        val player = playerMap[p.name]
        if (player != null && p.name != selfName) {
            val mesh = player.getComponent(MeshComponent::class)!!
            (p.color or black).toVecRGBA(MaterialCache.getEntry(mesh.materials[0]).waitFor()!!.diffuseBase)
            val rb = player.getComponent(DynamicBody::class)!!
            player.position = player.position.set(p.position)
            player.rotation = player.rotation.set(p.rotation)
            rb.globalLinearVelocity = rb.globalLinearVelocity.set(p.linearVelocity)
            rb.globalAngularVelocity = rb.globalAngularVelocity.set(p.angularVelocity)
        }
        Unit
    }

    // todo sound and fire (light)
    val bulletMesh = IcosahedronModel.createIcosphere(2)
    bulletMesh.materials = listOf(Material().apply {
        metallicMinMax.set(1f)
        roughnessMinMax.set(0f)
        diffuseBase.set(0.2f, 0.2f, 0.3f, 1f)
    }.ref)

    val onBulletPacket: (BulletPacket) -> Unit = { p ->
        // spawn bullet
        val bullet = Entity()
        bullet.position = bullet.position.set(p.pos)
        bullet.add(MeshComponent(bulletMesh).apply {
            isInstanced = true
        })
        bullet.setRotation(0f, atan2(p.dir.z, p.dir.x), 0f)
        bullet.setScale(0.1f)
        val flash = PointLight()
        bullet.add(object : Component(), OnUpdate {
            var distance = 0f
            override fun onUpdate() {
                val step = (90 * Time.deltaTime).toFloat()
                if (step != 0f) {
                    distance += step
                    flash.color.mul(exp(-(20 * Time.deltaTime).toFloat()))
                    if (distance > p.distance) {
                        bullet.removeFromParent()
                    } else {
                        p.dir.normalize(step)
                        bullet.position = bullet.position.add(p.dir)
                    }
                }
            }
        })
        val lightE = Entity(bullet)
        lightE.setScale(1000f)
        flash.color.set(30f, 10f, 0f)
        lightE.add(flash)
        bullets.add(bullet)
    }

    udpProtocol.register { PlayerUpdatePacket(onPlayerUpdate) }
    tcpProtocol.register { PlayerUpdatePacket(onPlayerUpdate) }
    udpProtocol.register { BulletPacket(onBulletPacket) }
    tcpProtocol.register { BulletPacket(onBulletPacket) }

    val instance = object : Instance(selfName) {
        override fun onPlayerJoin(player: Player) {
            if (player.name !in playerMap && player.name != selfName) {
                playerMap[player.name] = createPlayerBase(-1)
            }
        }

        override fun onPlayerExit(player: Player) {
            if (player.name != selfName) {
                playerMap[player.name]?.removeFromParent()
            }
        }
    }
    instance.start()

    // choose random color for player
    val selfColor = HSV.toRGB(Vector3f(Maths.random().toFloat(), 1f, 1f)).toRGB()
    val selfPlayerEntity = createPlayerBase(selfColor)
    selfPlayerEntity.add(BallPhysics(scene, staticScene, selfPlayerEntity, instance, onBulletPacket))

    // add camera controller
    val camera = Camera()
    val selfPlayer = LocalPlayer()
    camera.use(selfPlayer)
    val cameraBase = Entity(scene)
    val cameraBase1 = Entity(cameraBase)
    val cameraArm = Entity(cameraBase1)
    cameraArm.setPosition(1.5, 1.0, 4.0)
    cameraArm.setRotation(0f, 0f, 0f)
    cameraBase.add(
        BallCamera(
            cameraArm, selfPlayerEntity,
            cameraBase, cameraBase1,
            instance, selfColor
        )
    )
    cameraArm.add(camera)

    staticScene.add(DynamicBody().apply {
        restitution = 0.0
        friction = 1.0
    })

    val betterScene = InvalidRef // res.getChild("meshes/NavMesh.fbx")
    if (betterScene.exists) {
        staticScene.add(MeshComponent(betterScene))
        staticScene.add(MeshCollider(betterScene).apply {
            isConvex = false
        })
    } else {
        val plane = Entity(staticScene)
        plane.add(MeshComponent(flatCube.front))
        plane.add(BoxCollider())
        plane.setPosition(0.0, 0.0, 0.0)
        plane.setScale(30f, 1f, 30f)
    }

    respawn(selfPlayerEntity, staticScene)

    Systems.world = scene
    testUI3(selfName) {
        testScene2(scene) {
            it.renderView.playMode = PlayMode.PLAYING
            it.renderView.localPlayer = selfPlayer
        }
    }
}


val radius = 1.0
val down = Vector3f(0f, -1f, 0f)
fun respawn(
    entity: Entity /*= selfPlayerEntity*/,
    staticScene: Entity
) {
    val random = Random()
    // find save position to land via raycast
    val newPosition = Vector3d()
    val pos0 = entity.position
    val maxY = 1e3
    var iter = 0
    do {
        val f = 1.0 / iter++
        newPosition.set(
            mix(random.nextGaussian() * 10.0, pos0.x, f), maxY,
            mix(random.nextGaussian() * 10.0, pos0.z, f)
        )
        val query = RayQuery(newPosition, down, maxY)
        val hit = Raycast.raycast(staticScene, query)
        if (hit) {
            newPosition.y += radius - query.result.distance
            entity.position = newPosition
            val rb = entity.getComponent(DynamicBody::class)!!
            rb.globalLinearVelocity = Vector3d()
            rb.globalAngularVelocity = Vector3d()
            break
        }
    } while (true)
}

var rotX = 0f
var rotY = 0f


