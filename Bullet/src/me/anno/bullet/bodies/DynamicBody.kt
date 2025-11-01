package me.anno.bullet.bodies

import com.bulletphysics.collision.dispatch.ActivationState.ACTIVE
import com.bulletphysics.collision.dispatch.ActivationState.ALWAYS_ACTIVE
import com.bulletphysics.collision.dispatch.ActivationState.DISABLE_SIMULATION
import com.bulletphysics.collision.dispatch.ActivationState.SLEEPING
import com.bulletphysics.collision.dispatch.ActivationState.WANTS_DEACTIVATION
import cz.advel.stack.Stack
import me.anno.ecs.EntityTransform.getLocalXAxis
import me.anno.ecs.EntityTransform.getLocalYAxis
import me.anno.ecs.EntityTransform.getLocalZAxis
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.pow
import org.joml.Vector3d

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
            bi.deactivationTime = 0.0
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
    var gravity: Vector3d = Vector3d(gravity0)
        set(value) {
            field.set(value)
            if (overrideGravity)
                nativeInstance?.setGravity(value)
        }

    @Group("Mass")
    @Docs("How heavy it is")
    @SerializedProperty
    var mass = 1.0
        set(value) {
            if (field != value && value > 0.0) {
                val bi = nativeInstance
                if (bi != null) {
                    val inertia = Vector3d()
                    bi.collisionShape!!.calculateLocalInertia(value, inertia)
                    bi.setMassProps(mass, inertia)
                }
                field = value
            }
        }

    @Group("Movement")
    @Range(0.0, 1.0)
    @Docs("Friction against motion when moving through air / water")
    var linearDamping = 0.0
        set(value) {
            field = value
            nativeInstance?.linearDamping = value
        }

    @Group("Rotation")
    @Range(0.0, 10.0)
    @Docs("How much each rotation component is affected by forced, e.g. set to 0/1/0 to only allow y-rotation")
    var angularFactor = Vector3d(1.0)
        set(value) {
            field.set(value)
            nativeInstance?.angularFactor?.set(value)
        }

    @Group("Rotation")
    @Range(0.0, 1.0)
    @Docs("Friction against rotation when moving through air / water")
    var angularDamping = 0.0
        set(value) {
            field = value
            nativeInstance?.angularDamping = value
        }

    @Group("Movement")
    @Docs("Minimum velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var linearSleepingThreshold = 0.0001
        set(value) {
            field = value
            nativeInstance?.setSleepingThresholds(value, angularSleepingThreshold)
        }

    @Group("Rotation")
    @Docs("Minimum angular velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var angularSleepingThreshold = 0.0001
        set(value) {
            field = value
            nativeInstance?.setSleepingThresholds(linearSleepingThreshold, value)
        }

    // getter not needed, is updated automatically from BulletPhysics.kt
    @Group("Movement")
    var globalLinearVelocity: Vector3d = Vector3d()
        set(value) {
            field.set(value)
            nativeInstance?.setLinearVelocity(value)
        }

    @Group("Movement")
    var localLinearVelocity: Vector3d = Vector3d()
        get() {
            transform?.globalTransform?.transformDirectionInverse(globalLinearVelocity, field)
            return field
        }
        set(value) {
            field.set(value)
            val bi = nativeInstance
            val tr = transform
            if (tr != null && mass > 0.0 && bi != null && !isStatic) {
                val global = tr.globalTransform.transformDirection(field, Vector3d())
                bi.setLinearVelocity(global)
            }
        }

    @DebugProperty
    @Order(0)
    @Group("Movement")
    val localLinearVelocityX: Double
        get() = globalLinearVelocity.dot(getLocalXAxis())

    @DebugProperty
    @Order(1)
    @Group("Movement")
    val localLinearVelocityY: Double
        get() = globalLinearVelocity.dot(getLocalYAxis())

    @DebugProperty
    @Order(2)
    @Group("Movement")
    val localLinearVelocityZ: Double
        get() = globalLinearVelocity.dot(getLocalZAxis())

    // getter not needed, is updated automatically from BulletPhysics.kt
    @Group("Rotation")
    var globalAngularVelocity: Vector3d = Vector3d()
        set(value) {
            field.set(value)
            nativeInstance?.setAngularVelocity(value)
        }

    @Group("Movement")
    var localAngularVelocity: Vector3d = Vector3d()
        get() {
            transform?.globalTransform?.transformDirectionInverse(globalAngularVelocity, field)
            return field
        }
        set(value) {
            field.set(value)
            val bi = nativeInstance
            val tr = transform
            if (tr != null && mass > 0.0 && bi != null && !isStatic) {
                val global = tr.globalTransform.transformDirection(field, Vector3d())
                bi.setAngularVelocity(global)
            }
        }

    @DebugProperty
    @Order(0)
    @Group("Rotation")
    val localAngularVelocityX: Double
        get() = globalAngularVelocity.dot(getLocalXAxis())

    @DebugProperty
    @Order(1)
    @Group("Rotation")
    val localAngularVelocityY: Double
        get() = globalAngularVelocity.dot(getLocalYAxis())

    @DebugProperty
    @Order(2)
    @Group("Rotation")
    val localAngularVelocityZ: Double
        get() = globalAngularVelocity.dot(getLocalZAxis())

    @Group("Mass")
    @SerializedProperty
    var centerOfMass: Vector3d = Vector3d()
        set(value) {
            field.set(value)
            invalidateRigidbody()
        }

    @Group("Mass")
    @Docs("If an object is static, it will never move, and has infinite mass")
    @NotSerializedProperty
    val isStatic
        get() = mass <= 0.0

    /**
     * applies "rotation force"
     * must be called onPhysicsUpdate()
     * */
    fun applyTorque(x: Double, y: Double, z: Double) {
        val v = Stack.borrowVec()
        v.set(x, y, z)
        nativeInstance?.applyTorque(v)
    }

    fun applyTorque(v: Vector3d) = applyTorque(v.x, v.y, v.z)

    /**
     * applies a force centrally, so it won't rotate the object
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(x: Double, y: Double, z: Double) {
        val v = Stack.borrowVec()
        v.set(x, y, z)
        nativeInstance?.applyCentralForce(v)
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
        nativeInstance?.applyForce(force, relPos)
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
    fun applyImpulse(strengthX: Double, strengthY: Double, strengthZ: Double) {
        val bi = nativeInstance ?: return
        val impulse = Stack.borrowVec()
        impulse.set(strengthX, strengthY, strengthZ)
        bi.applyCentralImpulse(impulse)
    }

    /**
     * applies an impulse in global space, like a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(strength: Vector3d) = applyImpulse(strength.x, strength.y, strength.z)

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(
        relPosX: Double, relPosY: Double, relPosZ: Double,
        strengthX: Double, strengthY: Double, strengthZ: Double
    ) {
        val bi = nativeInstance ?: return
        val relativePosition = Stack.newVec()
        relativePosition.set(relPosX, relPosY, relPosZ)
        val strength = Stack.newVec()
        strength.set(strengthX, strengthY, strengthZ)
        bi.applyImpulse(strength, relativePosition)
        Stack.subVec(2)
    }

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     *
     * @param relativePosition globalHitPosition - rigidBody.globalPosition
     * @param impulse direction times strength in global space
     * */
    fun applyImpulse(relativePosition: Vector3d, impulse: Vector3d) =
        applyImpulse(relativePosition.x, relativePosition.y, relativePosition.z, impulse.x, impulse.y, impulse.z)

    /**
     * applies an impulse of torque, e.g., rotating something with a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyTorqueImpulse(x: Double, y: Double, z: Double) {
        val bi = nativeInstance ?: return
        val impulse = Stack.borrowVec() // is reused by method
        impulse.set(x, y, z)
        bi.applyTorqueImpulse(impulse)
    }

    /**
     * applies an impulse of torque in global space, e.g., rotating something with a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyTorqueImpulse(impulse: Vector3d) = applyTorqueImpulse(impulse.x, impulse.y, impulse.z)

    /**
     * should be called automatically by bullet, so idk why it's here
     * must be called onPhysicsUpdate()
     * */
    fun applyGravity() {
        nativeInstance?.applyGravity()
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (mass > 0.0) LineShapes.drawCircle(entity, pow(mass, 1.0 / 3.0), 0, 1, 0.0)
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
        val gravity0 = Vector3d(0.0, -9.81, 0.0)
    }
}