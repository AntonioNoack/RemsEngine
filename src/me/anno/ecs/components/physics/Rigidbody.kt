package me.anno.ecs.components.physics

import com.bulletphysics.dynamics.RigidBody
import me.anno.ecs.Component
import me.anno.engine.ui.RenderView
import me.anno.engine.ui.RenderView.Companion.scale
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.objects.Transform
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.abs

open class Rigidbody : Component() {

    @SerializedProperty
    var mass = 1.0

    // todo lock an axis of the object

    // todo extra gravity settings

    // todo getters for all information like velocity and such

    // todo functions to add impulses and forces

    init {
        // todo for getting things to rest
        // bulletInstance?.setSleepingThresholds()
    }

    // todo set when creating the instance
    var friction = 0.1
        set(value) {
            field = value
            bulletInstance?.friction = friction
        }

    @SerializedProperty
    var centerOfMass = Vector3d()
    /*set(value) {// todo
        field = value
        bulletInstance?.setCenterOfMassTransform(com.bulletphysics.linearmath.Transform())
    }*/

    @NotSerializedProperty
    var isStatic
        get() = mass <= 0.0
        set(value) {
            if (value) {
                // static: the negative value or null
                mass = -abs(mass)
            } else {
                // non-static: positive value
                mass = abs(mass)
                if (mass < 1e-16) mass = 1.0
            }
        }

    fun updatePhysics() {
        // todo remove old physics
        // todo create new bullet body

    }

    @NotSerializedProperty
    var bulletInstance: RigidBody? = null

    override fun onDrawGUI() {
        super.onDrawGUI()
        val stack = RenderView.stack
        stack.pushMatrix()
        stack.translate(centerOfMass.x.toFloat(), centerOfMass.y.toFloat(), centerOfMass.z.toFloat())
        Transform.drawUICircle(stack, 0.2f / scale.toFloat(), 0.7f, centerOfMassColor)
        stack.popMatrix()
    }

    override val className get() = "Rigidbody"

    companion object {
        val centerOfMassColor = Vector4f(1f, 0f, 0f, 1f)
    }

}