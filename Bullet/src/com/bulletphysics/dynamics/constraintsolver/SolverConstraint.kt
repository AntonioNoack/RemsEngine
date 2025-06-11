package com.bulletphysics.dynamics.constraintsolver

import org.joml.Vector3d

/**
 * 1D constraint along a normal axis between bodyA and bodyB. It can be combined
 * to solve contact and friction constraints.
 *
 * @author jezek2
 */
class SolverConstraint {
    @JvmField
    val relPos1CrossNormal: Vector3d = Vector3d()
    @JvmField
    val relPos2CrossNormal: Vector3d = Vector3d()

    @JvmField
    val angularComponentA: Vector3d = Vector3d()
    @JvmField
    val angularComponentB: Vector3d = Vector3d()

    @JvmField
    val contactNormal: Vector3d = Vector3d()

    @JvmField
    var appliedPushImpulse: Double = 0.0
    @JvmField
    var appliedImpulse: Double = 0.0

    @JvmField
    var solverBodyIdA: Int = 0
    @JvmField
    var solverBodyIdB: Int = 0

    @JvmField
    var friction: Double = 0.0
    @JvmField
    var restitution: Double = 0.0
    @JvmField
    var jacDiagABInv: Double = 0.0
    @JvmField
    var penetration: Double = 0.0

    @JvmField
    var constraintType: SolverConstraintType? = null
    @JvmField
    var frictionIndex: Int = 0
    @JvmField
    var originalContactPoint: Any? = null
}
