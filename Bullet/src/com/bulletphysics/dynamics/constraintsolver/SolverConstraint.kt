package com.bulletphysics.dynamics.constraintsolver

import org.joml.Vector3d
import org.joml.Vector3f

/**
 * 1D constraint along a normal axis between bodyA and bodyB. It can be combined
 * to solve contact and friction constraints.
 *
 * @author jezek2
 */
class SolverConstraint {
    @JvmField
    val relPos1CrossNormal = Vector3f()
    @JvmField
    val relPos2CrossNormal = Vector3f()

    @JvmField
    val angularComponentA = Vector3f()
    @JvmField
    val angularComponentB = Vector3f()

    @JvmField
    val contactNormal = Vector3f()

    @JvmField
    var appliedPushImpulse = 0f
    @JvmField
    var appliedImpulse = 0f

    @JvmField
    var solverBodyIdA = 0
    @JvmField
    var solverBodyIdB = 0

    @JvmField
    var friction = 0f
    @JvmField
    var restitution = 0f
    @JvmField
    var jacDiagABInv = 0f
    @JvmField
    var penetration = 0f

    @JvmField
    var constraintType: SolverConstraintType? = null
    @JvmField
    var frictionIndex = 0
    @JvmField
    var originalContactPoint: Any? = null
}
