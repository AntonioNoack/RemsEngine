/*
2007-09-09
btGeneric6DofConstraint Refactored by Francisco Le�n
email: projectileman@yahoo.com
http://gimpact.sf.net
*/
package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Rotation limit structure for generic joints.
 *
 * @author jezek2
 */
class RotationalLimitMotor {
    @JvmField
    var lowerLimit: Double = -BulletGlobals.SIMD_INFINITY

    @JvmField
    var upperLimit: Double = BulletGlobals.SIMD_INFINITY

    var targetVelocity: Double = 0.0
    var maxMotorForce: Double = 0.1
    var maxLimitForce: Double = 300.0
    var damping: Double = 1.0

    /**
     * Relaxation factor
     */
    var limitSoftness: Double = 0.5

    /**
     * Error tolerance factor when joint is at limit
     */
    var ERP: Double = 0.5

    /**
     * restitution factor
     */
    var bounce: Double = 0.0
    var enableMotor: Boolean = false

    /**
     * How much is violated this limit
     */
    var currentLimitError: Double = 0.0

    /**
     * 0=free, 1=at low limit, 2=at high limit
     */
    var currentLimit: Int = 0

    @JvmField
    var accumulatedImpulse: Double = 0.0

    val isLimited: Boolean
        /**
         * Is limited?
         */
        get() = !(lowerLimit >= upperLimit)

    /**
     * Need apply correction?
     */
    fun needApplyTorques(): Boolean {
        return currentLimit != 0 || enableMotor
    }

    /**
     * Calculates error. Calculates currentLimit and currentLimitError.
     */
    fun testLimitValue(testValue: Double): Int {
        if (lowerLimit > upperLimit) {
            currentLimit = 0 // Free from violation
            return 0
        }

        if (testValue < lowerLimit) {
            currentLimit = 1 // low limit violation
            currentLimitError = testValue - lowerLimit
            return 1
        } else if (testValue > upperLimit) {
            currentLimit = 2 // High limit violation
            currentLimitError = testValue - upperLimit
            return 2
        }

        currentLimit = 0 // Free from violation
        return 0
    }

    /**
     * Apply the correction impulses for two bodies.
     */
    fun solveAngularLimits(
        timeStep: Double, axis: Vector3d, jacDiagABInv: Double,
        body0: RigidBody, body1: RigidBody?, constraint: TypedConstraint
    ): Double {
        if (!needApplyTorques()) {
            return 0.0
        }

        var targetVelocity = targetVelocity
        var maxMotorForce = maxMotorForce

        // current error correction
        if (currentLimit != 0) {
            targetVelocity = -ERP * currentLimitError / (timeStep)
            maxMotorForce = maxLimitForce
        }

        maxMotorForce *= timeStep

        // current velocity difference
        val velocityDifference = Stack.newVec()
        velocityDifference.set(body0.angularVelocity)
        if (body1 != null) {
            velocityDifference.sub(body1.angularVelocity)
        }

        val relativeVelocity = axis.dot(velocityDifference)

        // correction velocity
        val motorRelativeVelocity = limitSoftness * (targetVelocity - damping * relativeVelocity)

        if (motorRelativeVelocity < BulletGlobals.FLT_EPSILON && motorRelativeVelocity > -BulletGlobals.FLT_EPSILON) {
            Stack.subVec(1)
            return 0.0 // no need for applying force
        }

        // correction impulse
        val unclippedMotorImpulse = (1 + bounce) * motorRelativeVelocity * jacDiagABInv
        if (abs(unclippedMotorImpulse) > constraint.breakingImpulseThreshold) {
            constraint.isBroken = true
            Stack.subVec(1)
            return 0.0
        }

        // clip correction impulse
        var clippedMotorImpulse = if (unclippedMotorImpulse > 0.0) {
            min(unclippedMotorImpulse, maxMotorForce)
        } else {
            max(unclippedMotorImpulse, -maxMotorForce)
        }

        // sort with accumulated impulses
        val lo = -1e308
        val hi = 1e308

        val oldImpulseSum = accumulatedImpulse
        val sum = oldImpulseSum + clippedMotorImpulse
        accumulatedImpulse = if (sum > hi) 0.0 else if (sum < lo) 0.0 else sum

        clippedMotorImpulse = accumulatedImpulse - oldImpulseSum

        val motorImp = Stack.newVec()
        axis.mul(clippedMotorImpulse, motorImp)

        body0.applyTorqueImpulse(motorImp)
        if (body1 != null) {
            motorImp.negate()
            body1.applyTorqueImpulse(motorImp)
        }

        Stack.subVec(2)
        return clippedMotorImpulse
    }
}
