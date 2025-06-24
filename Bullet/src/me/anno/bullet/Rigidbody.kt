package me.anno.bullet

import com.bulletphysics.collision.dispatch.ActivationState.ACTIVE
import com.bulletphysics.collision.dispatch.ActivationState.ALWAYS_ACTIVE
import com.bulletphysics.collision.dispatch.ActivationState.DISABLE_SIMULATION
import com.bulletphysics.collision.dispatch.ActivationState.SLEEPING
import com.bulletphysics.collision.dispatch.ActivationState.WANTS_DEACTIVATION
import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import me.anno.bullet.constraints.Constraint
import me.anno.ecs.Component
import me.anno.ecs.EntityPhysics.getPhysics
import me.anno.ecs.EntityQuery.hasComponent
import me.anno.ecs.EntityTransform.getLocalXAxis
import me.anno.ecs.EntityTransform.getLocalYAxis
import me.anno.ecs.EntityTransform.getLocalZAxis
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.pow
import org.joml.Vector3d
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class Rigidbody : Component(), OnDrawGUI {

    @Range(0.0, 15.0)
    var group = 1

    // which groups to collide with
    @DebugProperty
    @NotSerializedProperty
    val collisionMask
        get() = collisionMatrix[group]

    @DebugProperty
    @NotSerializedProperty
    var bulletInstance: RigidBody? = null

    @NotSerializedProperty
    val linkedConstraints = ArrayList<Constraint<*>>()

    @NotSerializedProperty
    val activeColliders = ArrayList<Collider>()

    @DebugProperty
    @NotSerializedProperty
    val bulletState: String
        get() = when (val s = bulletInstance?.activationState ?: -1) {
            ACTIVE -> "Active"
            SLEEPING -> "Island Sleeping"
            WANTS_DEACTIVATION -> "Wants Deactivation"
            ALWAYS_ACTIVE -> "Disable Deactivation"
            DISABLE_SIMULATION -> "Disable Simulation"
            -1 -> "null"
            else -> s.toString()
        }

    @DebugWarning
    @NotSerializedProperty
    val isMissingCollider: String?
        get() = if (entity!!.hasComponent(Collider::class)) null else "True"

    @SerializedProperty
    var activeByDefault = true

    @DebugAction
    fun activate() {
        val bi = bulletInstance
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
                bulletInstance?.setGravity(gravity)
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
                bulletInstance?.setGravity(value)
        }

    @Group("Mass")
    @Docs("How heavy it is; 0 means static")
    @SerializedProperty
    var mass = 0.0
        set(value) {
            if (field != value) {
                if ((field > 0.0) != (value > 0.0)) {
                    invalidatePhysics()
                } else {
                    val bulletInstance = bulletInstance
                    if (bulletInstance != null) {
                        val inertia = Vector3d()
                        bulletInstance.collisionShape!!.calculateLocalInertia(value, inertia)
                        bulletInstance.setMassProps(mass, inertia)
                    }
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
            bulletInstance?.linearDamping = value
        }

    @Group("Rotation")
    @Range(0.0, 1.0)
    @Docs("Friction against rotation when moving through air / water")
    var angularDamping = 0.0
        set(value) {
            field = value
            bulletInstance?.angularDamping = value
        }

    @Docs("How elastic a body is, 1 = fully elastic, 0 = all energy absorbed (knead)")
    @Range(0.0, 1.0)
    var restitution = 0.1
        set(value) {
            field = value
            bulletInstance?.restitution = value
        }

    @Group("Movement")
    @Docs("Minimum velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var linearSleepingThreshold = 0.01
        set(value) {
            field = value
            bulletInstance?.setSleepingThresholds(value, angularSleepingThreshold)
        }

    @Group("Rotation")
    @Docs("Minimum angular velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var angularSleepingThreshold = 0.01
        set(value) {
            field = value
            bulletInstance?.setSleepingThresholds(linearSleepingThreshold, value)
        }

    // getter not needed, is updated automatically from BulletPhysics.kt
    @Group("Movement")
    var globalLinearVelocity: Vector3d = Vector3d()
        set(value) {
            field.set(value)
            bulletInstance?.setLinearVelocity(value)
        }

    @Group("Movement")
    var localLinearVelocity: Vector3d = Vector3d()
        get() {
            transform?.globalTransform?.transformDirectionInverse(globalLinearVelocity, field)
            return field
        }
        set(value) {
            field.set(value)
            val bi = bulletInstance
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
            bulletInstance?.setAngularVelocity(value)
        }

    @Group("Movement")
    var localAngularVelocity: Vector3d = Vector3d()
        get() {
            transform?.globalTransform?.transformDirectionInverse(globalAngularVelocity, field)
            return field
        }
        set(value) {
            field.set(value)
            val bi = bulletInstance
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

    fun invalidatePhysics() {
        val entity = entity ?: return
        getPhysics(BulletPhysics::class)
            ?.invalidate(entity)
    }

    /**
     * Slowing down on contact. 1.0 = high traction, 0.0 = like on black ice
     * */
    @Group("Movement")
    @Range(0.0, 1.0)
    var friction: Double = 0.5
        set(value) {
            field = value
            bulletInstance?.friction = friction
        }

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
    fun applyImpulse(strengthX: Double, strengthY: Double, strengthZ: Double) {
        val bulletInstance = bulletInstance ?: return
        val impulse = Stack.borrowVec()
        impulse.set(strengthX, strengthY, strengthZ)
        bulletInstance.applyCentralImpulse(impulse)
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
        val bulletInstance = bulletInstance ?: return
        val relativePosition = Stack.newVec()
        relativePosition.set(relPosX, relPosY, relPosZ)
        val strength = Stack.newVec()
        strength.set(strengthX, strengthY, strengthZ)
        bulletInstance.applyImpulse(strength, relativePosition)
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
        val bulletInstance = bulletInstance ?: return
        val impulse = Stack.borrowVec() // is reused by method
        impulse.set(x, y, z)
        bulletInstance.applyTorqueImpulse(impulse)
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
        bulletInstance?.applyGravity()
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (mass > 0.0) LineShapes.drawCircle(entity, pow(mass, 1.0 / 3.0), 0, 1, 0.0)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Rigidbody) return
        dst.centerOfMass = centerOfMass
        dst.activeByDefault = activeByDefault
        dst.linearDamping = linearDamping
        dst.angularDamping = angularDamping
        dst.friction = friction
        dst.mass = mass
        dst.angularSleepingThreshold = angularSleepingThreshold
        dst.linearSleepingThreshold = linearSleepingThreshold
        dst.restitution = restitution
        dst.overrideGravity = overrideGravity
        dst.gravity.set(gravity)
    }

    companion object {
        val gravity0 = Vector3d(0.0, -9.81, 0.0)

        // todo define some kind of matrix
        // todo this would need to be a) standardized
        // todo or be customizable...
        val collisionMatrix = ShortArray(16) { (1 shl it).toShort() }
        // val centerOfMassColor = Vector4f(1f, 0f, 0f, 1f)
    }
}