package me.anno.tests.network.rollingshooter

import me.anno.Time
import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.shaders.Skybox
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testScene2
import me.anno.gpu.GFXBase
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.network.Instance
import me.anno.tests.network.Player
import me.anno.tests.network.tcpProtocol
import me.anno.tests.network.udpProtocol
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.color.spaces.HSV
import me.anno.utils.Color.black
import me.anno.utils.Color.toRGB
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS
import me.anno.utils.OS.pictures
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LoggerImpl
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp

// create an actual world, where we can walk around with other people
// make the game logic as simple as possible, so it's easy to follow

fun main() {

    // done moving
    // todo shooting
    //  - add gun model
    //  - add bullet animation
    //  - trace bullet path
    //  - send bullet packet
    // todo hitting
    // todo lives

    GFXBase.disableRenderDoc()

    val scene = Entity()
    scene.add(BulletPhysics())
    scene.add(Skybox())
    val sun = DirectionalLight()
    sun.color.set(10f)
    sun.shadowMapCascades = 1
    sun.shadowMapResolution = 1024
    val sunE = Entity(scene)
    sunE.setScale(50.0) // covering the map
    sunE.setRotation((-45.0).toRadians(), 0.0, 0.0)
    sunE.add(sun)
    scene.add(AmbientLight().apply {
        color.set(0.1f)
    })

    val players = Entity("Players", scene)
    val bullets = Entity("Bullet", scene)
    val staticScene = Entity("Static", scene)
    val sphereMesh = UVSphereModel.createUVSphere(32, 16)

    fun createPlayerBase(color: Int): Entity {
        val player = Entity("Player", players)
        player.add(Rigidbody().apply {
            mass = 5.0
            friction = 1.0
            angularDamping = 0.9
            restitution = 0.0
        })
        player.add(SphereCollider().apply { radius = 1.0 })
        player.add(MeshComponent(sphereMesh).apply {
            materials = listOf(Material().apply {
                (color or black).toVecRGBA(diffuseBase)
                normalMap = pictures.getChild("BricksNormal.png")
            }.ref)
        })
        return player
    }

    val selfName = LoggerImpl.getTimeStamp()

    val playerMap = HashMap<String, Entity>()
    val onPlayerUpdate: (PlayerUpdatePacket) -> Unit = { p ->
        val player = playerMap[p.name]
        if (player != null && p.name != selfName) {
            val mesh = player.getComponent(MeshComponent::class)!!
            (p.color or black).toVecRGBA(MaterialCache[mesh.materials[0]]!!.diffuseBase)
            val rb = player.getComponent(Rigidbody::class)!!
            player.position = player.position.set(p.position)
            player.rotation = player.rotation.set(p.rotation)
            rb.velocity = rb.velocity.set(p.linearVelocity)
            rb.angularVelocity = rb.angularVelocity.set(p.angularVelocity)
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
        bullet.setRotation(0.0, atan2(p.dir.z, p.dir.x).toDouble(), 0.0)
        bullet.setScale(0.1)
        val flash = PointLight()
        bullet.add(object : Component() {
            var distance = 0f
            override fun onUpdate(): Int {
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
                return 1
            }
        })
        val lightE = Entity(bullet)
        lightE.setScale(1000.0)
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
    val selfColor = HSV.toRGB(Vector3f(Math.random().toFloat(), 1f, 1f)).toRGB()
    val selfPlayerEntity = createPlayerBase(selfColor)

    val radius = 1.0
    val down = Vector3d(0.0, -1.0, 0.0)
    fun respawn(entity: Entity = selfPlayerEntity) {
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
            val hit = Raycast.raycast(staticScene, newPosition, down, 0.0, 0.0, maxY, -1)
            if (hit != null) {
                newPosition.y += radius - hit.distance
                entity.position = newPosition
                val rb = entity.getComponent(Rigidbody::class)!!
                rb.velocity = Vector3d()
                rb.angularVelocity = Vector3d()
                break
            }
        } while (true)
    }

    selfPlayerEntity.add(object : Component(), ControlReceiver {

        val jumpTimeout = (0.1 * SECONDS_TO_NANOS).toLong()
        var lastJumpTime = 0L

        fun shoot() {
            // shoot bullet
            val entity = entity!!
            val pos = Vector3d(entity.position).add(1.05, 0.0, -0.15)
            val dir = Vector3d(0.0, 0.0, -1.0)
            val maxDistance = 1e3
            val hit = Raycast.raycast(
                scene, pos, dir,
                0.0, 0.0, maxDistance, Raycast.COLLIDERS,
                -1, setOf(entity)
            )
            val distance = hit?.distance ?: maxDistance
            val packet = BulletPacket(onBulletPacket)
            packet.pos.set(pos)
            packet.dir.set(dir)
            packet.distance = distance.toFloat()
            instance.client?.sendUDP(packet, udpProtocol, false)
            onBulletPacket(packet)
        }

        override fun onKeyDown(key: Key): Boolean {
           return when(key){
                Key.BUTTON_LEFT -> {
                    RenderView.currentInstance?.uiParent?.lockMouse()
                    shoot()
                    true
                }
               Key.KEY_ESCAPE -> {
                   Input.unlockMouse()
                   true
               }
                else -> super.onKeyDown(key)
            }
        }

        override fun onPhysicsUpdate(): Boolean {
            val entity = entity!!
            val rigidbody = entity.getComponent(Rigidbody::class)!!
            val strength = 12.0 * rigidbody.mass
            if (entity.position.y < -10.0 || Input.wasKeyPressed(Key.KEY_R)) {
                respawn()
            }
            if (Input.isKeyDown(Key.KEY_W)) rigidbody.applyTorque(-strength, 0.0, 0.0)
            if (Input.isKeyDown(Key.KEY_S)) rigidbody.applyTorque(+strength, 0.0, 0.0)
            if (Input.isKeyDown(Key.KEY_A)) rigidbody.applyTorque(0.0, 0.0, +strength)
            if (Input.isKeyDown(Key.KEY_D)) rigidbody.applyTorque(0.0, 0.0, -strength)
            if (Input.isKeyDown(Key.KEY_SPACE) && abs(Time.gameTimeN - lastJumpTime) > jumpTimeout) {
                // only jump if we are on something
                val hit = Raycast.raycast(staticScene, entity.position, down, 0.0, 0.0, radius, -1)
                if (hit != null) {
                    lastJumpTime = Time.gameTimeN
                    rigidbody.applyImpulse(0.0, strength, 0.0)
                }
            }
            return true
        }
    })

    // add camera controller
    val camera = Camera()
    val selfPlayer = LocalPlayer()
    camera.use(selfPlayer)
    val cameraBase = Entity(scene)
    val cameraArm = Entity(cameraBase)
    cameraArm.setPosition(0.0, 3.0, 4.0)
    cameraArm.setRotation((-30.0).toRadians(), 0.0, 0.0)
    cameraBase.add(object : Component(), ControlReceiver {
        // todo rotate camera
        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
            // todo on click/focus capture mouse
            return super.onMouseMoved(x, y, dx, dy)
        }

        var i = 0
        override fun onUpdate(): Int {
            val pos = selfPlayerEntity.position
            val rot = selfPlayerEntity.rotation
            val rb = selfPlayerEntity.getComponent(Rigidbody::class)!!
            val vel = rb.velocity
            val ang = rb.angularVelocity
            cameraBase.position = cameraBase.position
                .lerp(pos, dtTo01(5.0 * Time.deltaTime))
            // ideally, send this every frame; this is just for testing :)
            if (i++ > 0) {
                i = 0
                instance.client?.sendUDP(PlayerUpdatePacket {}.apply {
                    position.set(pos)
                    rotation.set(rot)
                    linearVelocity.set(vel)
                    angularVelocity.set(ang)
                    color = selfColor
                    // our name is set by the server, we don't have to set/send it ourselves
                }, udpProtocol, false)
            }
            return 1
        }
    })
    cameraArm.add(camera)

    staticScene.add(Rigidbody().apply {
        restitution = 0.0
        friction = 1.0
    })

    val betterScene = OS.documents.getChild("NavMeshTest2.obj")
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
        plane.setScale(30.0, 1.0, 30.0)
    }

    respawn()

    testUI(selfName) {
        testScene2(scene) {
            it.renderer.playMode = PlayMode.PLAYING
            it.renderer.localPlayer = selfPlayer
        }.apply { weight = 1f }
    }
}
