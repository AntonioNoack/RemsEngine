package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import kotlin.math.abs

/**
 * Taken from https://github.com/bulletphysics/bullet3/blob/master/src/BulletDynamics/ConstraintSolver/btGeneric6DofSpringConstraint.cpp
 * */
class Generic6DofSpringConstraint : TypedConstraint {

    val frameInA: Transform = Transform() //!< the constraint space w.r.t body A
    val frameInB: Transform = Transform() //!< the constraint space w.r.t body B
    var useLinearReferenceFrameA = false

    val springEnabled = BooleanArray(6)
    val equilibriumPoint = Array(2) { Vector3d() }
    val springStiffness = DoubleArray(6)
    val springDamping = DoubleArray(6) { 1.0 }

    val linearLimits = TranslationalLimitMotor()

    val angularLimits = Array(3) { RotationalLimitMotor() }

    constructor() {
        useLinearReferenceFrameA = true
    }

    constructor(
        rbA: RigidBody,
        rbB: RigidBody,
        frameInA: Transform,
        frameInB: Transform,
        useLinearReferenceFrameA: Boolean
    ) : super(rbA, rbB) {
        this.frameInA.set(frameInA)
        this.frameInB.set(frameInB)
        this.useLinearReferenceFrameA = useLinearReferenceFrameA
    }

    fun enableSpring(index: Int, enable: Boolean) {
        assertTrue(index in 0 until 6)
        springEnabled[index] = enable
        if (index < 3) {
            // linearLimits.enableMotor[index] = enable
        } else {
            angularLimits[index - 3].enableMotor = enable
        }
    }

    val calculatedLinearDiff = Vector3d()
    val calculatedAngleDiff = Vector3d()

    fun calculateTransforms() {
        TODO()
    }

    fun setEquilibriumPoint() {
        calculateTransforms()
        equilibriumPoint[0].set(calculatedLinearDiff)
        equilibriumPoint[1].set(calculatedAngleDiff)
    }

    private fun internalUpdateSprings(info: ContactSolverInfo) {
        val dt = info.timeStep / info.numIterations
        // it is assumed that calculateTransforms() have been called before this call
        for (i in 0 until 3) {
            if (!springEnabled[i]) continue
            // todo for all axes...
            val delta = calculatedLinearDiff[i] - equilibriumPoint[0][i]
            // spring force is (delta * m_stiffness) according to Hooke's Law
            val force = delta * springStiffness[i]
            val velFactor = dt * springDamping[i]
            // todo support this
            // linearLimits.targetVelocity.x = velFactor * force
            // linearLimits.maxMotorForce.x = abs(force)
        }
        for (i in 0 until 3) {
            if (!springEnabled[i + 3]) continue
            val delta = calculatedAngleDiff[i] - equilibriumPoint[1][i]
            // spring force is (-delta * m_stiffness) according to Hooke's Law
            val force = -delta * springStiffness[i + 3]
            val velFactor = dt * springDamping[i + 3]
            angularLimits[i].targetVelocity = velFactor * force
            angularLimits[i].maxMotorForce = abs(force)
        }
    }

    override fun buildJacobian() {
        TODO("Not yet implemented")
    }

    override fun solveConstraint(timeStep: Double) {
        TODO("Not yet implemented")
    }
}