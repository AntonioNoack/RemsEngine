package me.anno.ecs.components.physics

import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import me.anno.ecs.Component
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.scale
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.objects.Transform
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.abs

open class Rigidbody : Component() {

    // todo lock an axis of the object

    // todo extra gravity settings

    // todo getters for all information like velocity and such

    // todo functions to add impulses and forces

    init {
        // todo for getting things to rest
        // bulletInstance?.setSleepingThresholds()
    }

    override var isEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidatePhysics()
            }
        }

    @SerializedProperty
    var mass = 1.0
        set(value) {
            if (field != value) {
                if ((field > 0.0) != (value > 0.0)) {
                    invalidatePhysics()
                } else {
                    val bulletInstance = bulletInstance
                    if (bulletInstance != null) {
                        val inertia = javax.vecmath.Vector3d()
                        bulletInstance.collisionShape.calculateLocalInertia(value, inertia)
                        bulletInstance.setMassProps(mass, inertia)
                    }
                }
                field = value
            }
        }

    /**
     * friction when moving through air / water
     * */
    var linearDamping = 0.1
        set(value) {
            if (field != value) {
                field = value
                invalidatePhysics()
            }
        }

    /**
     * friction when moving through air / water
     * */
    var angularDamping = 0.1
        set(value) {
            if (field != value) {
                field = value
                invalidatePhysics()
            }
        }

    /**
     * how much energy is absorbed in a collision; relative,
     * 0 = perfectly bouncy, 1 = all energy absorbed (knead)
     * */
    var restitution = 0.1
        set(value) {
            field = value
            bulletInstance?.restitution = value
        }

    var linearSleepingThreshold = 1.0
        set(value) {
            field = value
            bulletInstance?.setSleepingThresholds(value, angularSleepingThreshold)
        }

    var angularSleepingThreshold = 0.8
        set(value) {
            field = value
            bulletInstance?.setSleepingThresholds(linearSleepingThreshold, value)
        }

    var sleepingTimeThreshold = 40.0
        set(value) {
            field = value
            bulletInstance?.deactivationTime = value
        }

    fun invalidatePhysics() {
        val entity = entity ?: return
        entity.physics?.invalidate(entity)
    }

    /**
     * slowing down on contact
     * */
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

    /**
     * applies "rotation force"
     * must be called onPhysicsUpdate()
     * */
    fun applyTorque(x: Double, y: Double, z: Double) {
        val v = Stack.borrowVec()
        v.set(x, y, z)
        bulletInstance?.applyTorque(v)
    }

    fun applyTorque(v: Vector3d) = applyTorque(v.x, v.y, v.z)

    /**
     * applies a force centrally, so it won't rotate the object
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(x: Double, y: Double, z: Double) {
        val v = Stack.borrowVec()
        v.set(x, y, z)
        bulletInstance?.applyCentralForce(v)
    }

    /**
     * applies a force centrally, so it won't rotate the object
     * must be called on physics update
     * */
    fun applyForce(force: Vector3d) = applyForce(force.x, force.y, force.z)

    /**
     * applies a force and torque
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(px: Double, py: Double, pz: Double, x: Double, y: Double, z: Double) {
        val relPos = Stack.newVec()
        relPos.set(px, py, pz)
        val force = Stack.newVec()
        force.set(x, y, z)
        bulletInstance?.applyForce(force, relPos)
        Stack.subVec(2)
    }

    /**
     * applies a force and torque
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(relativePosition: Vector3d, force: Vector3d) =
        applyForce(relativePosition.x, relativePosition.y, relativePosition.z, force.x, force.y, force.z)

    /**
     * applies an impulse, like a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(x: Double, y: Double, z: Double) {
        val impulse = Stack.borrowVec()
        impulse.set(x, y, z)
        bulletInstance?.applyCentralImpulse(impulse)
    }

    /**
     * applies an impulse, like a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(impulse: Vector3d) = applyImpulse(impulse.x, impulse.y, impulse.z)

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(px: Double, py: Double, pz: Double, x: Double, y: Double, z: Double) {
        val relPos = Stack.newVec()
        relPos.set(px, py, pz)
        val impulse = Stack.newVec()
        impulse.set(x, y, z)
        bulletInstance?.applyImpulse(impulse, relPos)
        Stack.subVec(2)
    }

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(relativePosition: Vector3d, impulse: Vector3d) =
        applyImpulse(relativePosition.x, relativePosition.y, relativePosition.z, impulse.x, impulse.y, impulse.z)

    /**
     * applies an impulse of torque, e.g. rotating something with a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyTorqueImpulse(x: Double, y: Double, z: Double) {
        val impulse = Stack.newVec()
        impulse.set(x, y, z)
        bulletInstance?.applyTorqueImpulse(impulse)
        Stack.subVec(1)
    }

    /**
     * applies an impulse of torque, e.g. rotating something with a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyTorqueImpulse(impulse: Vector3d) = applyTorqueImpulse(impulse.x, impulse.y, impulse.z)

    /**
     * should be called automatically by bullet, so idk why it's here
     * must be called onPhysicsUpdate()
     * */
    fun applyGravity() {
        bulletInstance?.applyGravity()
    }

    @NotSerializedProperty
    var bulletInstance: RigidBody? = null

    override fun onDrawGUI(view: RenderView) {// center of mass circle
        super.onDrawGUI(view)
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