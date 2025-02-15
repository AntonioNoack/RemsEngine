package me.anno.tests.network.rollingshooter

import me.anno.Time
import me.anno.bullet.Rigidbody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.dtTo01
import me.anno.maths.bvh.HitType
import me.anno.tests.network.Instance
import me.anno.tests.network.udpProtocol
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class BallPhysics(
    val scene: Entity,
    val staticScene: Entity,
    val selfPlayerEntity: Entity,
    val instance: Instance,
    val onBulletPacket: (BulletPacket) -> Unit
) : Component(), InputListener, OnPhysicsUpdate {

    val jumpTimeout = (0.1 * SECONDS_TO_NANOS).toLong()
    var lastJumpTime = 0L

    private fun findBulletDistance(pos: Vector3d, dir: Vector3d): Double {
        val maxDistance = 1e3
        val query = RayQuery(
            pos, dir, maxDistance, Raycast.COLLIDERS,
            -1, false, setOf(entity!!)
        )
        Raycast.raycast(scene, query)
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
        playBulletSound()
    }

    fun playBulletSound() {
        val audio = ShootingSound.audios.random()
        scene.add(audio)
        audio.start()
    }

    private fun lockMouse() {
        RenderView.currentInstance?.uiParent?.lockMouse()
    }

    override fun onKeyDown(key: Key): Boolean {
        return when (key) {
            Key.BUTTON_LEFT -> {
                if (Input.isMouseLocked) shootBullet() else lockMouse()
                true
            }
            Key.KEY_ESCAPE -> {
                Input.unlockMouse()
                true
            }
            else -> super.onKeyDown(key)
        }
    }

    override fun onKeyUp(key: Key): Boolean {
        return when (key) {
            Key.KEY_SPACE -> {
                val jumpTime = Input.getDownTimeNanos(key) / 1e9f
                jumpForce = dtTo01(jumpTime * 5f)
                true
            }
            else -> super.onKeyUp(key)
        }
    }

    var jumpForce = 0f

    override fun onPhysicsUpdate(dt: Double) {

        val entity = entity!!
        val rigidbody = entity.getComponent(Rigidbody::class)!!
        val strength = 12.0 * rigidbody.mass
        if (entity.position.y < -10.0 || Input.wasKeyPressed(Key.KEY_R)) {
            respawn(selfPlayerEntity, staticScene)
        }

        val c = cos(rotY) * strength
        val s = sin(rotY) * strength

        if (Input.isKeyDown(Key.KEY_W)) rigidbody.applyTorque(-c, 0.0, +s)
        if (Input.isKeyDown(Key.KEY_S)) rigidbody.applyTorque(+c, 0.0, -s)
        if (Input.isKeyDown(Key.KEY_A)) rigidbody.applyTorque(+s, 0.0, +c)
        if (Input.isKeyDown(Key.KEY_D)) rigidbody.applyTorque(-s, 0.0, -c)

        if (jumpForce > 0f && abs(Time.gameTimeN - lastJumpTime) > jumpTimeout) {
            // only jump if we are on something
            val query = RayQuery(entity.position, down, radius)
            query.result.hitType = HitType.ANY
            if (Raycast.raycast(staticScene, query)) {
                lastJumpTime = Time.gameTimeN
                val iy = -10000.0 * strength * jumpForce
                println("jumping with force $iy")
                rigidbody.applyImpulse(0.0, iy, 0.0)
            } else println("floating in air")
            jumpForce = 0f
        }
    }
}
