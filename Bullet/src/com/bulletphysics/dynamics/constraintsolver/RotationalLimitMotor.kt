/*
2007-09-09
btGeneric6DofConstraint Refactored by Francisco Leon
email: projectileman@yahoo.com
http://gimpact.sf.net
*/
package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import org.joml.Vector3f
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
    var lowerLimit = -BulletGlobals.SIMD_INFINITY

    @JvmField
    var upperLimit = BulletGlobals.SIMD_INFINITY

    var targetVelocity = 0f
    var maxMotorForce = 0.1f
    var maxLimitForce = 300f
    var damping = 1f

    /**
     * Relaxation factor
     */
    var limitSoftness = 0.5f

    /**
     * Error tolerance factor when joint is at limit
     */
    var ERP = 0.5f

    /**
     * restitution factor
     */
    var bounce = 0f
    var enableMotor: Boolean = false

    /**
     * How much is violated this limit
     */
    var currentLimitError = 0f

    /**
     * 0=free, 1=at low limit, 2=at high limit
     */
    var currentLimit: Int = 0

    var accumulatedImpulse = 0f

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
    fun testLimitValue(testValue: Float): Int {
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
        timeStep: Float, axis: Vector3f, jacDiagABInv: Float,
        body0: RigidBody, body1: RigidBody?, constraint: TypedConstraint
    ): Float {
        if (!needApplyTorques()) {
            return 0f
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
        val velocityDifference = Stack.newVec3f()
        velocityDifference.set(body0.angularVelocity)
        if (body1 != null) {
            velocityDifference.sub(body1.angularVelocity)
        }

        val relativeVelocity = axis.dot(velocityDifference)

        // correction velocity
        val motorRelativeVelocity = limitSoftness * (targetVelocity - damping * relativeVelocity)

        if (motorRelativeVelocity < BulletGlobals.FLT_EPSILON && motorRelativeVelocity > -BulletGlobals.FLT_EPSILON) {
            Stack.subVec3f(1)
            return 0f // no need for applying force
        }

        // correction impulse
        val unclippedMotorImpulse = (1 + bounce) * motorRelativeVelocity * jacDiagABInv
        if (abs(unclippedMotorImpulse) > constraint.breakingImpulseThreshold) {
            constraint.isBroken = true
            Stack.subVec3f(1)
            return 0f
        }

        // clip correction impulse
        var clippedMotorImpulse = if (unclippedMotorImpulse > 0f) {
            min(unclippedMotorImpulse, maxMotorForce)
        } else {
            max(unclippedMotorImpulse, -maxMotorForce)
        }

        // sort with accumulated impulses
        val lo = -1e38f
        val hi = 1e38f

        val oldImpulseSum = accumulatedImpulse
        val sum = oldImpulseSum + clippedMotorImpulse
        accumulatedImpulse = if (sum > hi) 0f else if (sum < lo) 0f else sum

        clippedMotorImpulse = accumulatedImpulse - oldImpulseSum

        val motorImp = Stack.newVec3f()
        axis.mul(clippedMotorImpulse, motorImp)

        body0.applyTorqueImpulse(motorImp)
        if (body1 != null) {
            motorImp.negate()
            body1.applyTorqueImpulse(motorImp)
        }

        Stack.subVec3f(2)
        return clippedMotorImpulse
    }
}
