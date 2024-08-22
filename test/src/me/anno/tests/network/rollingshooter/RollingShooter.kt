package me.anno.tests.network.rollingshooter

import me.anno.Time
import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
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
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.Updatable
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testScene2
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.pow
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.network.Instance
import me.anno.tests.network.Player
import me.anno.tests.network.tcpProtocol
import me.anno.tests.network.udpProtocol
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.color.spaces.HSV
import me.anno.utils.Color.black
import me.anno.utils.Color.toRGB
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS.pictures
import me.anno.utils.OS.res
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LoggerImpl
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

// create an actual world, where we can walk around with other people
// make the game logic as simple as possible, so it's easy to follow

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
    Systems.registerSystem("bullet", BulletPhysics())
    scene.add(Skybox())
    val sun = DirectionalLight()
    sun.color.set(10f)
    sun.shadowMapCascades = 1
    sun.shadowMapResolution = 1024
    val sunE = Entity(scene)
    sunE.setScale(70.0) // covering the map
    sunE.setRotation((-45.0).toRadians(), 0.0, 0.0)
    sunE.add(sun)

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

    val selfName = LoggerImpl.getTimeStamp().toString()

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
        bullet.add(object : Component(), Updatable {
            var distance = 0f
            override fun update(instances: Collection<Component>) {
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
    val selfColor = HSV.toRGB(Vector3f(Maths.random().toFloat(), 1f, 1f)).toRGB()
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
            val query = RayQuery(newPosition, down, maxY)
            val hit = Raycast.raycastClosestHit(staticScene, query)
            if (hit) {
                newPosition.y += radius - query.result.distance
                entity.position = newPosition
                val rb = entity.getComponent(Rigidbody::class)!!
                rb.velocity = Vector3d()
                rb.angularVelocity = Vector3d()
                break
            }
        } while (true)
    }

    var rotX = -30.0
    var rotY = 0.0

    selfPlayerEntity.add(object : Component(), InputListener, OnPhysicsUpdate {

        val jumpTimeout = (0.1 * SECONDS_TO_NANOS).toLong()
        var lastJumpTime = 0L

        private fun findBulletDistance(pos: Vector3d, dir: Vector3d): Double {
            val maxDistance = 1e3
            val query = RayQuery(
                pos, dir, maxDistance, Raycast.COLLIDERS,
                -1, false, setOf(entity!!)
            )
            Raycast.raycastClosestHit(scene, query)
            return query.result.distance
        }

        var shotLeft = false
        fun shootBullet() {
            val entity = entity!!
            val pos = Vector3d().add(if (shotLeft) -1.05 else 1.05, 0.0, -0.15)
                .rotateX(rotX).rotateY(rotY).add(entity.position)
            val dir = Vector3d(0.0, 0.0, -1.0)
                .rotateX(rotX).rotateY(rotY)
            val distance = findBulletDistance(pos, dir)
            val packet = BulletPacket(onBulletPacket)
            packet.pos.set(pos)
            packet.dir.set(dir)
            packet.distance = distance.toFloat()
            instance.client?.sendUDP(packet, udpProtocol, false)
            onBulletPacket(packet)
            shotLeft = !shotLeft
        }

        private fun lockMouse() {
            RenderView.currentInstance?.uiParent?.lockMouse()
        }

        override fun onKeyDown(key: Key): Boolean {
            return when (key) {
                Key.BUTTON_LEFT -> {
                    if (Input.isMouseLocked) shootBullet()
                    else lockMouse()
                    true
                }
                Key.KEY_ESCAPE -> {
                    Input.unlockMouse()
                    true
                }
                else -> super.onKeyDown(key)
            }
        }

        override fun onPhysicsUpdate(dt: Double) {
            val entity = entity!!
            val rigidbody = entity.getComponent(Rigidbody::class)!!
            val strength = 12.0 * rigidbody.mass
            if (entity.position.y < -10.0 || Input.wasKeyPressed(Key.KEY_R)) {
                respawn()
            }
            val c = cos(rotY) * strength
            val s = sin(rotY) * strength
            if (Input.isKeyDown(Key.KEY_W)) rigidbody.applyTorque(-c, 0.0, +s)
            if (Input.isKeyDown(Key.KEY_S)) rigidbody.applyTorque(+c, 0.0, -s)
            if (Input.isKeyDown(Key.KEY_A)) rigidbody.applyTorque(+s, 0.0, +c)
            if (Input.isKeyDown(Key.KEY_D)) rigidbody.applyTorque(-s, 0.0, -c)
            if (Input.isKeyDown(Key.KEY_SPACE) && abs(Time.gameTimeN - lastJumpTime) > jumpTimeout) {
                // only jump if we are on something
                val query = RayQuery(entity.position, down, radius)
                if (Raycast.raycastAnyHit(staticScene, query)) {
                    lastJumpTime = Time.gameTimeN
                    rigidbody.applyImpulse(0.0, strength, 0.0)
                }
            }
        }
    })

    // add camera controller
    val camera = Camera()
    val selfPlayer = LocalPlayer()
    camera.use(selfPlayer)
    val cameraBase = Entity(scene)
    val cameraBase1 = Entity(cameraBase)
    val cameraArm = Entity(cameraBase1)
    cameraArm.setPosition(1.5, 1.0, 4.0)
    cameraArm.setRotation(0.0, 0.0, 0.0)
    cameraBase.add(object : Component(), InputListener, Updatable {

        override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
            cameraArm.position = cameraArm.position.mul(pow(0.98, dy.toDouble()))
            return true
        }

        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
            return if (Input.isMouseLocked) {
                // rotate camera
                val speed = 1.0 / RenderView.currentInstance!!.height
                rotX = clamp(rotX + dy * speed, -PI / 2, PI / 2)
                rotY = (rotY + dx * speed) % TAU
                true
            } else super.onMouseMoved(x, y, dx, dy)
        }

        override fun update(instances: Collection<Component>) {
            // update transforms
            val pos = selfPlayerEntity.position
            cameraBase.position = cameraBase.position.lerp(pos, dtTo01(5.0 * Time.deltaTime))
            cameraBase1.setRotation(rotX, rotY, 0.0)
            // send our data to the other players
            instance.client?.sendUDP(PlayerUpdatePacket {}.apply {
                val rot = selfPlayerEntity.rotation
                val rb = selfPlayerEntity.getComponent(Rigidbody::class)!!
                val vel = rb.velocity
                val ang = rb.angularVelocity
                position.set(pos)
                rotation.set(rot)
                linearVelocity.set(vel)
                angularVelocity.set(ang)
                color = selfColor
                // our name is set by the server, we don't have to set/send it ourselves
            }, udpProtocol, false)
        }
    })
    cameraArm.add(camera)

    staticScene.add(Rigidbody().apply {
        restitution = 0.0
        friction = 1.0
    })

    val betterScene = res.getChild("meshes/NavMesh.fbx")
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
            it.renderView.playMode = PlayMode.PLAYING
            it.renderView.localPlayer = selfPlayer
        }.apply { weight = 1f }
    }
}
