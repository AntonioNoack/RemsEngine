package me.anno.bulletjme

import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.physics
import me.anno.ecs.EntityQuery.hasComponent
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.pow
import org.joml.Vector3d
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class Rigidbody : Component() {

    @Range(0.0, 15.0)
    var group = 1

    // which groups to collide with
    @DebugProperty
    @NotSerializedProperty
    val collisionMask
        get() = collisionMatrix[group]

    @DebugProperty
    @NotSerializedProperty
    var bulletInstance: PhysicsRigidBody? = null

    // @NotSerializedProperty
    // val constraints = ArrayList<Constraint<*>>()

    /*@DebugProperty
    @NotSerializedProperty
    val bulletState: String
        get() = when (val s = bulletInstance?.activationState ?: -1) {
            ACTIVE_TAG -> "Active"
            ISLAND_SLEEPING -> "Island Sleeping"
            WANTS_DEACTIVATION -> "Wants Deactivation"
            DISABLE_DEACTIVATION -> "Disable Deactivation"
            DISABLE_SIMULATION -> "Disable Simulation"
            -1 -> "null"
            else -> s.toString()
        }*/

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
        else bi.applyCentralImpulse(Vector3f(0f, 10f * mass.toFloat(), 0f))
    }

    override var isEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidatePhysics()
            }
        }

    @SerializedProperty
    var deleteWhenKilledByDepth = false // mmh... depending on edit mode?

    @SerializedProperty
    var overrideGravity = false
        set(value) {
            field = value
            if (value) {
                bulletInstance?.setGravity(v(gravity))
            } else {
                invalidateRigidbody() // it's complicated ^^
            }
        }

    @SerializedProperty
    var gravity: Vector3d = Vector3d(gravity0)
        set(value) {
            field.set(value)
            if (overrideGravity) {
                bulletInstance?.setGravity(v(value))
            }
        }

    @Docs("How heavy it is; 0 means static")
    @SerializedProperty
    var mass = 0.0
        set(value) {
            if (field != value) {
                if ((field > 0.0) != (value > 0.0)) {
                    invalidatePhysics()
                } else {
                    // todo fix
                    /*val bulletInstance = bulletInstance
                    if (bulletInstance != null) {
                        val inertia = javax.vecmath.Vector3d()
                        bulletInstance.collisionShape.calculateLocalInertia(value, inertia)
                        bulletInstance.setMassProps(mass, inertia)
                    }*/
                }
                field = value
            }
        }

    @Docs("Friction against motion when moving through air / water")
    var linearDamping = 0.1
        set(value) {
            if (field != value) {
                field = value
                invalidatePhysics()
            }
        }

    @Docs("Friction against rotation when moving through air / water")
    var angularDamping = 0.1
        set(value) {
            if (field != value) {
                field = value
                invalidatePhysics()
            }
        }

    @Docs("How bouncy a body is: 1 = perfectly bouncy, 0 = all energy absorbed (knead)")
    @Range(0.0, 1.0)
    var restitution = 0.1
        set(value) {
            field = value
            bulletInstance?.restitution = value.toFloat()
        }

    @Docs("Minimum velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var linearSleepingThreshold = 1.0
        set(value) {
            field = value
            bulletInstance?.setSleepingThresholds(value.toFloat(), angularSleepingThreshold.toFloat())
        }

    @Docs("Minimum angular velocity to count as standing still")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var angularSleepingThreshold = 0.8
        set(value) {
            field = value
            bulletInstance?.setSleepingThresholds(linearSleepingThreshold.toFloat(), value.toFloat())
        }

    @Docs("Minimum time after which an object is marked as sleeping")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var sleepingTimeThreshold = 0.0 // 4.0
        set(value) {
            field = value
            bulletInstance?.deactivationTime = value.toFloat()
        }

    @Docs("velocity in global space")
    @DebugProperty
    var velocity = Vector3d()
        get() {
            val bi = bulletInstance
            if (bi != null) {
                val tmp = Vector3f()
                bulletInstance?.getLinearVelocity(tmp)
                field.set(tmp.x, tmp.y, tmp.z)
            }
            return field
        }
        set(value) {
            field.set(value)
            val bi = bulletInstance
            if (bi != null) {
                val tmp = Vector3f(value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
                bulletInstance?.setLinearVelocity(tmp)
            }
        }

    // todo setter for local velocity :)
    @DebugProperty
    val localVelocity = Vector3d()
        get() {
            val bi = bulletInstance
            val tr = transform
            if (bi != null && tr != null) {
                val t = tr.globalTransform
                val tmp = Vector3f()
                bulletInstance?.getLinearVelocity(tmp)
                field.set(
                    t.m00 * tmp.x + t.m01 * tmp.y + t.m02 * tmp.z,
                    t.m10 * tmp.x + t.m11 * tmp.y + t.m12 * tmp.z,
                    t.m20 * tmp.x + t.m21 * tmp.y + t.m22 * tmp.z
                )
            }
            return field
        }

    val localVelocityX: Double
        get() {
            val tr = transform
            val bi = bulletInstance
            return if (tr != null && bi != null) {
                val t = tr.globalTransform
                val tmp = Vector3f()
                bulletInstance?.getLinearVelocity(tmp)
                t.m00 * tmp.x + t.m01 * tmp.y + t.m02 * tmp.z
            } else 0.0
        }

    val localVelocityY: Double
        get() {
            val tr = transform
            val bi = bulletInstance
            return if (tr != null && bi != null) {
                val t = tr.globalTransform
                val tmp = Vector3f()
                bulletInstance?.getLinearVelocity(tmp)
                t.m10 * tmp.x + t.m11 * tmp.y + t.m12 * tmp.z
            } else 0.0
        }

    val localVelocityZ: Double
        get() {
            val tr = transform
            val bi = bulletInstance
            return if (tr != null && bi != null) {
                val t = tr.globalTransform
                val tmp = Vector3f()
                bulletInstance?.getLinearVelocity(tmp)
                t.m20 * tmp.x + t.m21 * tmp.y + t.m22 * tmp.z
            } else 0.0
        }

    @Docs("Angular velocity in global space")
    @DebugProperty
    var angularVelocity = Vector3d()
        get() {
            val bi = bulletInstance
            if (bi != null) {
                val tmp = Vector3f()
                bulletInstance?.getAngularVelocity(tmp)
                field.set(tmp.x, tmp.y, tmp.z)
            }
            return field
        }
        set(value) {
            field.set(value)
            val bi = bulletInstance
            if (bi != null) {
                bulletInstance?.setAngularVelocity(v(value.x, value.y, value.z))
            }
        }

    fun invalidatePhysics() {
        val entity = entity ?: return
        entity.physics?.invalidate(entity)
    }

    /**
     * slowing down on contact
     * */
    @Range(0.0, 1.0)
    var friction = 0.1
        set(value) {
            field = value
            bulletInstance?.friction = friction.toFloat()
        }

    @SerializedProperty
    var centerOfMass = Vector3d()
        set(value) {
            field.set(value)
            val bi = bulletInstance
            if (bi != null) {
                // todo implement this
                /*val trans = Stack.borrowTrans()
                trans.setIdentity()
                trans.origin.set(value.x, value.y, value.z)
                bi.setCenterOfMassTransform(trans)*/
            }
        }

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

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.physics?.invalidate(entity)
    }

    /**
     * applies "rotation force"
     * must be called onPhysicsUpdate()
     * */
    fun applyTorque(x: Double, y: Double, z: Double) {
        bulletInstance?.applyTorque(v(x, y, z))
    }

    fun applyTorque(v: Vector3d) {
        applyTorque(v.x, v.y, v.z)
    }

    /**
     * applies a force centrally, so it won't rotate the object
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(x: Double, y: Double, z: Double) {
        bulletInstance?.applyCentralForce(v(x, y, z))
    }

    /**
     * applies a force centrally, so it won't rotate the object
     * must be called on physics update
     * */
    fun applyForce(force: Vector3d) {
        applyForce(force.x, force.y, force.z)
    }

    /**
     * applies a force and torque
     * must be called onPhysicsUpdate()
     * */
    fun applyForce(px: Double, py: Double, pz: Double, x: Double, y: Double, z: Double) {
        bulletInstance?.applyForce(v(x, y, z), v(px, py, pz))
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
        bulletInstance?.applyCentralImpulse(v(x, y, z))
    }

    /**
     * applies an impulse in global space, like a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(impulse: Vector3d) = applyImpulse(impulse.x, impulse.y, impulse.z)

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(px: Double, py: Double, pz: Double, x: Double, y: Double, z: Double) {
        bulletInstance?.applyImpulse(v(x, y, z), v(px, py, pz))
    }

    /**
     * applies an impulse, and a bit of torque
     * must be called onPhysicsUpdate()
     * */
    fun applyImpulse(relativePosition: Vector3d, impulse: Vector3d) =
        applyImpulse(relativePosition.x, relativePosition.y, relativePosition.z, impulse.x, impulse.y, impulse.z)

    /**
     * applies an impulse of torque, e.g., rotating something with a hit
     * must be called onPhysicsUpdate()
     * */
    fun applyTorqueImpulse(x: Double, y: Double, z: Double) {
        bulletInstance?.applyTorqueImpulse(v(x, y, z))
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
        bulletInstance?.applyCentralForce(v(gravity))
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (mass > 0.0) LineShapes.drawCircle(entity, pow(mass, 1.0 / 3.0), 0, 1, 0.0)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Rigidbody
        dst.centerOfMass = centerOfMass
        dst.activeByDefault = activeByDefault
        dst.angularDamping = angularDamping
        dst.friction = friction
        dst.mass = mass
        dst.linearDamping = linearDamping
        dst.sleepingTimeThreshold = sleepingTimeThreshold
        dst.angularSleepingThreshold = angularSleepingThreshold
        dst.linearSleepingThreshold = linearSleepingThreshold
        dst.restitution = restitution
        dst.overrideGravity = overrideGravity
        dst.gravity.set(gravity)
    }

    override val className: String get() = "Rigidbody"

    companion object {
        val gravity0 = Vector3d(0.0, -9.81, 0.0)
        fun v(v: Vector3d): Vector3f {
            return Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
        }

        fun v(x: Double, y: Double, z: Double): Vector3f {
            return Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
        }

        fun q(x: Double, y: Double, z: Double, w: Double): Quaternion {
            return Quaternion(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
        }

        // todo define some kind of matrix
        // todo this would need to be a) standardized
        // todo or be customizable...
        val collisionMatrix = ShortArray(16) { (1 shl it).toShort() }
        // val centerOfMassColor = Vector4f(1f, 0f, 0f, 1f)
    }
}