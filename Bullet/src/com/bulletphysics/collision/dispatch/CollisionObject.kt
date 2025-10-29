package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.linearmath.Transform
import me.anno.bullet.bodies.PhysicsBody
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d

/**
 * CollisionObject can be used to manage collision detection objects.
 * It maintains all information that is needed for a collision detection: [CollisionShape],
 * [Transform] and [AABB proxy][BroadphaseProxy]. It can be added to [CollisionWorld].
 *
 * @author jezek2
 */
open class CollisionObject() {

    @JvmField
    val worldTransform = Transform()

    /** used for CCD and interpolation
     * it can be either previous or future (predicted) transform */
    @JvmField
    val interpolationWorldTransform = Transform()

    /**
     * those two are experimental: just added for bullet time effect, so you can still apply impulses (directly modifying velocities)
     * without destroying the continuous interpolated motion (which uses this interpolation velocities)
     */
    @JvmField
    val interpolationLinearVelocity = Vector3d()

    @JvmField
    val interpolationAngularVelocity = Vector3d()

    /**
     * bounds including linear and angular motion,
     * based on ccdSweptSphereRadius
     * */
    @JvmField
    val collisionAabbMin = Vector3d()

    @JvmField
    val collisionAabbMax = Vector3d()

    @JvmField
    var broadphaseHandle: BroadphaseProxy? = null

    @JvmField
    var collisionShape: CollisionShape? = null

    @JvmField
    var collisionFlags: Int = CollisionFlags.STATIC_OBJECT

    @JvmField
    var islandTag: Int = -1

    @JvmField
    var companionId: Int = -1

    @JvmField
    var activationState = ActivationState.ACTIVE

    @JvmField
    var deactivationTime: Double = 0.0

    @JvmField
    var friction: Double = 0.5

    @JvmField
    var restitution: Double = 0.0

    /**
     * time of impact calculation
     */
    @JvmField
    var hitFraction: Double = 1.0

    /**
     * Swept sphere radius (0.0 by default), see btConvexConvexAlgorithm::
     * CCD = convex collision detection
     */
    @JvmField
    var ccdSweptSphereRadius: Double = 0.0

    /**
     * Don't do continuous collision detection if the motion (in one step) is less then ccdMotionThreshold
     */
    @JvmField
    var ccdMotionThreshold: Double = 0.0

    /**
     * If some object should have elaborate collision filtering by subclasses
     */
    @JvmField
    var checkCollideWith: Boolean = false

    @JvmField
    var userData: PhysicsBody<*>? = null

    open fun checkCollideWithOverride(co: CollisionObject?): Boolean {
        return true
    }

    fun mergesSimulationIslands(): Boolean {
        /**static objects, kinematic and object without contact response don't merge islands */
        return ((collisionFlags and (CollisionFlags.STATIC_OBJECT or CollisionFlags.KINEMATIC_OBJECT or CollisionFlags.NO_CONTACT_RESPONSE)) == 0)
    }

    val isStaticObject: Boolean
        get() = collisionFlags.hasFlag(CollisionFlags.STATIC_OBJECT)

    val isKinematicObject: Boolean
        get() = collisionFlags.hasFlag(CollisionFlags.KINEMATIC_OBJECT)

    val isStaticOrKinematicObject: Boolean
        get() = (collisionFlags and (CollisionFlags.KINEMATIC_OBJECT or CollisionFlags.STATIC_OBJECT)) != 0

    fun hasContactResponse(): Boolean {
        return !collisionFlags.hasFlag(CollisionFlags.NO_CONTACT_RESPONSE)
    }

    fun setActivationStateMaybe(newState: ActivationState) {
        if (activationState != ActivationState.ALWAYS_ACTIVE && activationState != ActivationState.DISABLE_SIMULATION) {
            activationState = newState
        }
    }

    @Suppress("unused")
    fun forceActivationState(newState: ActivationState) {
        this.activationState = newState
    }

    @JvmOverloads
    fun activate(forceActivation: Boolean = false) {
        if (forceActivation || (collisionFlags and (CollisionFlags.STATIC_OBJECT or CollisionFlags.KINEMATIC_OBJECT)) == 0) {
            setActivationStateMaybe(ActivationState.ACTIVE)
            deactivationTime = 0.0
        }
    }

    val isActive: Boolean
        get() = (activationState != ActivationState.SLEEPING && activationState != ActivationState.DISABLE_SIMULATION)

    fun getWorldTransform(out: Transform): Transform {
        out.set(worldTransform)
        return out
    }

    fun setWorldTransform(worldTransform: Transform) {
        this.worldTransform.set(worldTransform)
    }

    fun getInterpolationWorldTransform(out: Transform): Transform {
        out.set(interpolationWorldTransform)
        return out
    }

    fun setInterpolationWorldTransform(interpolationWorldTransform: Transform) {
        this.interpolationWorldTransform.set(interpolationWorldTransform)
    }

    @Suppress("unused")
    fun setInterpolationLinearVelocity(linvel: Vector3d) {
        interpolationLinearVelocity.set(linvel)
    }

    @Suppress("unused")
    fun setInterpolationAngularVelocity(angvel: Vector3d) {
        interpolationAngularVelocity.set(angvel)
    }

    val ccdSquareMotionThreshold: Double
        get() = ccdMotionThreshold * ccdMotionThreshold

    fun checkCollideWith(co: CollisionObject?): Boolean {
        if (checkCollideWith) {
            return checkCollideWithOverride(co)
        }
        return true
    }
}
