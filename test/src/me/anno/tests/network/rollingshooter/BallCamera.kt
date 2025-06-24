package me.anno.tests.network.rollingshooter

import me.anno.Time
import me.anno.bullet.bodies.DynamicBody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.pow
import me.anno.tests.network.Instance
import me.anno.tests.network.udpProtocol

class BallCamera(
    val cameraArm: Entity,
    val selfPlayerEntity: Entity,
    val cameraBase: Entity,
    val cameraBase1: Entity,
    val instance: Instance,
    val selfColor: Int
) : Component(), InputListener, OnUpdate {

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
        cameraArm.position = cameraArm.position.mul(pow(0.98, dy.toDouble()))
        return true
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        return if (Input.isMouseLocked) {
            // rotate camera
            val speed = 1f / RenderView.currentInstance!!.height
            rotX = clamp(rotX + dy * speed, -PIf / 2, PIf / 2)
            rotY = (rotY + dx * speed) % TAUf
            true
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onUpdate() {
        // update transforms
        val pos = selfPlayerEntity.position
        cameraBase.position = cameraBase.position.mix(pos, dtTo01(5.0 * Time.deltaTime))
        cameraBase1.setRotation(rotX, rotY, 0f)
        // send our data to the other players
        instance.client?.sendUDP(PlayerUpdatePacket {}.apply {
            val rot = selfPlayerEntity.rotation
            val rb = selfPlayerEntity.getComponent(DynamicBody::class)!!
            val vel = rb.globalLinearVelocity
            val ang = rb.globalAngularVelocity
            position.set(pos)
            rotation.set(rot)
            linearVelocity.set(vel)
            angularVelocity.set(ang)
            color = selfColor
            // our name is set by the server, we don't have to set/send it ourselves
        }, udpProtocol, false)
    }
}