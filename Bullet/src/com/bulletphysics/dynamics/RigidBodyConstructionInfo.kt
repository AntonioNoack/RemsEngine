package com.bulletphysics.dynamics

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.linearmath.MotionState
import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d

/**
 * RigidBodyConstructionInfo provides information to create a rigid body.
 *
 * Setting mass to zero creates a fixed (non-dynamic) rigid body. For dynamic objects,
 * you can use the collision shape to approximate the local inertia tensor, otherwise
 * use the zero vector (default argument).
 *
 * You can use [MotionState] to synchronize the world transform
 * between physics and graphics objects. And if the motion state is provided, the rigid
 * body will initialize its initial world transform from the motion state,
 * [startWorldTransform][.startWorldTransform] is only used when you don't provide
 * a motion state.
 *
 * @author jezek2
 */
class RigidBodyConstructionInfo(
    @JvmField var mass: Double,
    /**
     * When a motionState is provided, the rigid body will initialize its world transform
     * from the motion state. In this case, startWorldTransform is ignored.
     */
    @JvmField var motionState: MotionState?,
    @JvmField var collisionShape: CollisionShape,
    localInertia: Vector3d
) {
    @JvmField
    val startWorldTransform: Transform = Transform()

    @JvmField
    val localInertia: Vector3d = Vector3d()
    @JvmField
    var linearDamping: Double = 0.0
    @JvmField
    var angularDamping: Double = 0.0

    /**
     * Best simulation results when friction is non-zero.
     */
    @JvmField
    var friction: Double = 0.5

    /**
     * Best simulation results using zero restitution.
     */
    @JvmField
    var restitution: Double = 0.0

    @JvmField
    var linearSleepingThreshold: Double = 0.8
    @JvmField
    var angularSleepingThreshold: Double = 1.0

    /**
     * Additional damping can help avoiding lowpass jitter motion, help stability for ragdolls etc.
     * Such damping is undesirable, so once the overall simulation quality of the rigid body dynamics
     * system has improved, this should become obsolete.
     */
    @JvmField
    var additionalDamping: Boolean = false
    @JvmField
    var additionalDampingFactor: Double = 0.005
    @JvmField
    var additionalLinearDampingThresholdSqr: Double = 0.01
    @JvmField
    var additionalAngularDampingThresholdSqr: Double = 0.01
    @JvmField
    var additionalAngularDampingFactor: Double = 0.01

    @Suppress("unused")
    constructor(mass: Double, motionState: MotionState?, collisionShape: CollisionShape) : this(
        mass, motionState, collisionShape,
        Vector3d(0.0, 0.0, 0.0)
    )

    init {
        this.localInertia.set(localInertia)
        startWorldTransform.setIdentity()
    }
}
