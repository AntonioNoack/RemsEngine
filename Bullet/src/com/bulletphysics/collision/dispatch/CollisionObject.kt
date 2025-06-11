package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.linearmath.Transform
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
    var worldTransform: Transform = Transform()

    /** m_interpolationWorldTransform is used for CCD and interpolation
     * it can be either previous or future (predicted) transform */
    @JvmField
    val interpolationWorldTransform: Transform = Transform()

    /**
     * those two are experimental: just added for bullet time effect, so you can still apply impulses (directly modifying velocities)
     * without destroying the continuous interpolated motion (which uses this interpolation velocities)
     */
    @JvmField
    val interpolationLinearVelocity: Vector3d = Vector3d()

    @JvmField
    val interpolationAngularVelocity: Vector3d = Vector3d()

    @JvmField
    var broadphaseHandle: BroadphaseProxy? = null

    var collisionShape: CollisionShape? = null

    @JvmField
    var collisionFlags: Int = CollisionFlags.STATIC_OBJECT

    @JvmField
    var islandTag: Int = -1

    @JvmField
    var companionId: Int = -1

    var activationState: Int = 1

    @JvmField
    var deactivationTime: Double = 0.0

    @JvmField
    var friction: Double = 0.5

    @JvmField
    var restitution: Double = 0.0

    /**
     * users can point to their objects, m_userPointer is not used by Bullet, see setUserPointer/getUserPointer
     */
    var userObjectPointer: Any? = null

    /**
     * time of impact calculation
     */
    @JvmField
    var hitFraction: Double = 1.0

    /**
     * Swept sphere radius (0.0 by default), see btConvexConvexAlgorithm::
     */
    @JvmField
    var ccdSweptSphereRadius: Double = 0.0

    /**
     * Don't do continuous collision detection if the motion (in one step) is less then ccdMotionThreshold
     */
    var ccdMotionThreshold: Double = 0.0

    /**
     * If some object should have elaborate collision filtering by subclasses
     */
    @JvmField
    var checkCollideWith: Boolean = false

    open fun checkCollideWithOverride(co: CollisionObject?): Boolean {
        return true
    }

    fun mergesSimulationIslands(): Boolean {
        /**static objects, kinematic and object without contact response don't merge islands */
        return ((collisionFlags and (CollisionFlags.STATIC_OBJECT or CollisionFlags.KINEMATIC_OBJECT or CollisionFlags.NO_CONTACT_RESPONSE)) == 0)
    }

    val isStaticObject: Boolean
        get() = (collisionFlags and CollisionFlags.STATIC_OBJECT) != 0

    val isKinematicObject: Boolean
        get() = (collisionFlags and CollisionFlags.KINEMATIC_OBJECT) != 0

    val isStaticOrKinematicObject: Boolean
        get() = (collisionFlags and (CollisionFlags.KINEMATIC_OBJECT or CollisionFlags.STATIC_OBJECT)) != 0

    fun hasContactResponse(): Boolean {
        return (collisionFlags and CollisionFlags.NO_CONTACT_RESPONSE) == 0
    }

    /**
     * Avoid using this internal API call.
     * internalSetTemporaryCollisionShape is used to temporarily replace the actual collision shape by a child collision shape.
     */
    fun internalSetTemporaryCollisionShape(collisionShape: CollisionShape?) {
        this.collisionShape = collisionShape
    }

    fun setActivationStateMaybe(newState: Int) {
        if ((activationState != DISABLE_DEACTIVATION) && (activationState != DISABLE_SIMULATION)) {
            this.activationState = newState
        }
    }

    @Suppress("unused")
    fun forceActivationState(newState: Int) {
        this.activationState = newState
    }

    @JvmOverloads
    fun activate(forceActivation: Boolean = false) {
        if (forceActivation || (collisionFlags and (CollisionFlags.STATIC_OBJECT or CollisionFlags.KINEMATIC_OBJECT)) == 0) {
            setActivationStateMaybe(ACTIVE_TAG)
            deactivationTime = 0.0
        }
    }

    val isActive: Boolean
        get() = (activationState != ISLAND_SLEEPING && activationState != DISABLE_SIMULATION)

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

    fun getInterpolationLinearVelocity(out: Vector3d): Vector3d {
        out.set(interpolationLinearVelocity)
        return out
    }

    fun getInterpolationAngularVelocity(out: Vector3d): Vector3d {
        out.set(interpolationAngularVelocity)
        return out
    }

    val ccdSquareMotionThreshold: Double
        get() = ccdMotionThreshold * ccdMotionThreshold

    fun checkCollideWith(co: CollisionObject?): Boolean {
        if (checkCollideWith) {
            return checkCollideWithOverride(co)
        }
        return true
    }

    companion object {
        // island management, m_activationState1
        const val ACTIVE_TAG: Int = 1
        const val ISLAND_SLEEPING: Int = 2
        const val WANTS_DEACTIVATION: Int = 3
        const val DISABLE_DEACTIVATION: Int = 4
        const val DISABLE_SIMULATION: Int = 5
    }
}
