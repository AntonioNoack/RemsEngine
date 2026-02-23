package me.anno.tests.network.rollingshooter

import me.anno.Time
import me.anno.bullet.bodies.DynamicBody
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
import me.anno.input.MouseLock.isMouseLocked
import me.anno.input.MouseLock.unlockMouse
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.dtTo01
import me.anno.maths.bvh.HitType
import me.anno.tests.network.Instance
import me.anno.tests.network.udpProtocol
import org.joml.Vector3d
import org.joml.Vector3f
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

    private fun findBulletDistance(pos: Vector3d, dir: Vector3f): Double {
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
            .rotateX(rotX.toDouble()).rotateY(rotY.toDouble()).add(entity.position)
        val dir = Vector3f(0f, 0f, -1f)
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
                if (isMouseLocked) shootBullet() else lockMouse()
                true
            }
            Key.KEY_ESCAPE -> {
                unlockMouse()
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
        val dynamicBody = entity.getComponent(DynamicBody::class)!!
        val strength = 12f * dynamicBody.mass
        if (entity.position.y < -10.0 || Input.wasKeyPressed(Key.KEY_R)) {
            respawn(selfPlayerEntity, staticScene)
        }

        val c = cos(rotY) * strength
        val s = sin(rotY) * strength

        if (Input.isKeyDown(Key.KEY_W)) dynamicBody.applyTorque(-c, 0f, +s)
        if (Input.isKeyDown(Key.KEY_S)) dynamicBody.applyTorque(+c, 0f, -s)
        if (Input.isKeyDown(Key.KEY_A)) dynamicBody.applyTorque(+s, 0f, +c)
        if (Input.isKeyDown(Key.KEY_D)) dynamicBody.applyTorque(-s, 0f, -c)

        if (jumpForce > 0f && abs(Time.gameTimeN - lastJumpTime) > jumpTimeout) {
            // only jump if we are on something
            val query = RayQuery(entity.position, down, radius)
            query.result.hitType = HitType.ANY
            if (Raycast.raycast(staticScene, query)) {
                lastJumpTime = Time.gameTimeN
                val iy = -10000f * strength * jumpForce
                println("jumping with force $iy")
                dynamicBody.applyImpulse(0f, iy, 0f)
            } else println("floating in air")
            jumpForce = 0f
        }
    }
}
