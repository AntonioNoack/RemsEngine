package me.anno.bullet.bodies

import com.bulletphysics.collision.dispatch.ActivationState.ACTIVE
import com.bulletphysics.collision.dispatch.ActivationState.ALWAYS_ACTIVE
import com.bulletphysics.collision.dispatch.ActivationState.DISABLE_SIMULATION
import com.bulletphysics.collision.dispatch.ActivationState.SLEEPING
import com.bulletphysics.collision.dispatch.ActivationState.WANTS_DEACTIVATION
import cz.advel.stack.Stack
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.pow
import org.joml.Vector3d
import org.joml.Vector3f

@Suppress("MemberVisibilityCanBePrivate", "unused")
@Docs("Dynamically simulated rigidbody")
open class DynamicBody : PhysicalBody(), OnDrawGUI {

    // todo when this is moved in the editor, activate() must be called!!!

    @DebugProperty
    @NotSerializedProperty
    val bulletState: String
        get() = when (val s = nativeInstance?.activationState ?: -1) {
            ACTIVE -> "Active"
            SLEEPING -> "Island Sleeping"
            WANTS_DEACTIVATION -> "Wants Deactivation"
            ALWAYS_ACTIVE -> "Disable Deactivation"
            DISABLE_SIMULATION -> "Disable Simulation"
            -1 -> "null"
            else -> s.toString()
        }

    @SerializedProperty
    var activeByDefault = true

    @DebugAction
    fun activate() {
        val bi = nativeInstance
        if (bi == null) invalidatePhysics()
        else {
            bi.activationState = ACTIVE
            bi.deactivationTime = 0f
        }
    }

    @Group("Movement")
    @SerializedProperty
    var deleteWhenKilledByDepth = false // mmh... depending on edit mode?

    @Group("Mass")
    @SerializedProperty
    var overrideGravity = false
        set(value) {
            field = value
            if (value) {
                nativeInstance?.setGravity(gravity)
            } else {
                invalidateRigidbody() // it's complicated ^^
            }
        }

    @Group("Mass")
    @SerializedProperty
    var gravity: Vector3f = Vector3f(gravity0)
        set(value) {
            field.set(value)
            if (overrideGravity)
                nativeInstance?.setGravity(value)
        }

    @Group("Mass")
    @Docs("How heavy it is")
    @SerializedProperty
    var mass = 1f
        set(value) {
            if (field != value && value > 0f) {
                val bi = nativeInstance
                if (bi != null) {
                    val inertia = Vector3f()
                    bi.collisionShape.calculateLocalInertia(value, inertia)
                    bi.setMassProps(mass, inertia)
                }
                field = value
            }
        }

    @Group("Movement")
    @Range(0.0, 1.0)
    @Docs("Friction against motion when moving through air / water")
    var linearDamping = 0f
        set(value) {
            field = value
            nativeInstance?.linearDamping = value
        }

    @Group("Rotation")
    @Range(0.0, 10.0)
    @Docs("How much each rotation component is affected by forced, e.g. set to 0/1/0 to only allow y-rotation")
    var angularFactor = Vector3f(1f)
        set(value) {
            field.set(value)
            nativeInstance?.angularFactor?.set(value)
        }

    @Group("Rotation")
    @Range(0.0, 1.0)
    @Docs("Friction against rotation when moving through air / water")
    var angularDamping = 0f
        set(value) {
            field = value
            nativeInstance?.angularDamping = value
        }

    @Group("Movement")
    @Docs("Minimum velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var linearSleepingThreshold = 0.0001f
        set(value) {
            field = value
            nativeInstance?.setSleepingThresholds(value, angularSleepingThreshold)
        }

    @Group("Rotation")
    @Docs("Minimum angular velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var angularSleepingThreshold = 0.0001f
        set(value) {
            field = value
            nativeInstance?.setSleepingThresholds(linearSleepingThreshold, value)
        }

    @Group("Mass")
    @SerializedProperty
    var centerOfMass: Vector3d = Vector3d()
        set(value) {
            field.set(value)
            invalidateRigidbody()
        }

    /**
     * applies "rotation force"
     * must be called onPhysicsUpdate()
     * */
    fun applyTorque(x: Float, y: Float, z: Float) {
        val torque = Stack.borrowVec3f().set(x, y, z)
        applyTorque(torque)
    }

    fun applyTorque(torque: Vector3f) {
        nativeInstance?.applyTorque(torque)
    }

    /**
     * applies a force centrally, so it won't rotate the object
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(forceX: Float, forceY: Float, forceZ: Float) {
        val force = Stack.borrowVec3f().set(forceX, forceY, forceZ)
        applyForce(force)
    }

    /**
     * applies a force centrally, so it won't rotate the object
     * must be called on physics update
     * */
    fun applyForce(force: Vector3f) {
        nativeInstance?.applyCentralForce(force)
    }

    /**
     * applies a force and torque
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(
        relPosX: Float, relPosY: Float, relPosZ: Float,
        forceX: Float, forceY: Float, forceZ: Float
    ) {
        val relPos = Stack.newVec3f().set(relPosX, relPosY, relPosZ)
        val force = Stack.newVec3f().set(forceX, forceY, forceZ)
        applyForce(relPos, force)
        Stack.subVec3f(2)
    }

    /**
     * applies a force and torque
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(relativePosition: Vector3f, force: Vector3f) {
        nativeInstance?.applyForce(force, relativePosition)
    }

    /**
     * applies an impulse, like a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(impulseX: Float, impulseY: Float, impulseZ: Float) {
        val impulse = Stack.borrowVec3f().set(impulseX, impulseY, impulseZ)
        applyImpulse(impulse)
    }

    /**
     * applies an impulse in global space, like a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(impulse: Vector3f) {
        nativeInstance?.applyCentralImpulse(impulse)
    }

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(
        relPosX: Float, relPosY: Float, relPosZ: Float,
        impulseX: Float, impulseY: Float, impulseZ: Float
    ) {
        val relPos = Stack.newVec3f().set(relPosX, relPosY, relPosZ)
        val impulse = Stack.newVec3f().set(impulseX, impulseY, impulseZ)
        applyImpulse(relPos, impulse)
        Stack.subVec3f(2)
    }

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     *
     * @param relativePosition globalHitPosition - rigidBody.globalPosition
     * @param impulse direction times strength in global space
     * */
    fun applyImpulse(relativePosition: Vector3f, impulse: Vector3f) {
        nativeInstance?.applyImpulse(impulse, relativePosition)
    }

    /**
     * applies an impulse of torque, e.g., rotating something with a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyTorqueImpulse(x: Float, y: Float, z: Float) {
        val impulse = Stack.borrowVec3f().set(x, y, z)
        applyTorqueImpulse(impulse)
    }

    /**
     * applies an impulse of torque in global space, e.g., rotating something with a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyTorqueImpulse(impulse: Vector3f) {
        nativeInstance?.applyTorqueImpulse(impulse)
    }

    /**
     * should be called automatically by bullet, so idk why it's here
     * must be called onPhysicsUpdate()
     * */
    fun applyGravity() {
        nativeInstance?.applyGravity()
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (mass > 0.0) {
            val radius = pow(mass.toDouble(), 1.0 / 3.0)
            LineShapes.drawCircle(entity, radius, 0, 1, 0.0)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is DynamicBody) return
        dst.centerOfMass = centerOfMass
        dst.activeByDefault = activeByDefault
        dst.linearDamping = linearDamping
        dst.angularDamping = angularDamping
        dst.mass = mass
        dst.angularSleepingThreshold = angularSleepingThreshold
        dst.linearSleepingThreshold = linearSleepingThreshold
        dst.overrideGravity = overrideGravity
        dst.gravity.set(gravity)
    }

    companion object {
        val gravity0 = Vector3f(0f, -9.81f, 0f)
    }
}