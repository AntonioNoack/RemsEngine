package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * 2007-09-09
 * btGeneric6DofConstraint Refactored by Francisco León
 * email: projectileman@yahoo.com
 * http://gimpact.sf.net
 *
 * @author jezek2
 */
class TranslationalLimitMotor {

    @JvmField
    val lowerLimit = Vector3f()

    @JvmField
    val upperLimit = Vector3f()

    @JvmField
    val accumulatedImpulse = Vector3f()

    /**
     * Softness for linear limit
     * */
    var limitSoftness: Float

    /**
     * Damping for linear limit
     * */
    var damping: Float

    /**
     * Bounce parameter for linear limit
     * */
    var restitution: Float

    constructor() {
        lowerLimit.set(0f)
        upperLimit.set(0f)
        accumulatedImpulse.set(0f)

        limitSoftness = 0f
        damping = 1f
        restitution = 0.5f
    }

    constructor(other: TranslationalLimitMotor) {
        lowerLimit.set(other.lowerLimit)
        upperLimit.set(other.upperLimit)
        accumulatedImpulse.set(other.accumulatedImpulse)

        limitSoftness = other.limitSoftness
        damping = other.damping
        restitution = other.restitution
    }

    /**
     * Test limit.
     *
     *
     * - free means upper &lt; lower,<br></br>
     * - locked means upper == lower<br></br>
     * - limited means upper &gt; lower<br></br>
     * - limitIndex: first 3 are linear, next 3 are angular
     */
    fun isLimited(limitIndex: Int): Boolean {
        return upperLimit[limitIndex] >= lowerLimit[limitIndex]
    }

    fun solveLinearAxis(
        timeStep: Float, jacDiagABInv: Float,
        body1: RigidBody, pointInA: Vector3d,
        body2: RigidBody, pointInB: Vector3d, limitIndex: Int,
        axisNormalOnA: Vector3f, anchorPos: Vector3d
    ): Float {

        // find relative velocity
        val relPos1 = Stack.newVec3f()
        val relPos2 = Stack.newVec3f()

        anchorPos.sub(body1.worldTransform.origin, relPos1)
        anchorPos.sub(body2.worldTransform.origin, relPos2)

        val vel1 = body1.getVelocityInLocalPoint(relPos1, Stack.newVec3f())
        val vel2 = body2.getVelocityInLocalPoint(relPos2, Stack.newVec3f())
        val vel = Stack.newVec3f()
        vel1.sub(vel2, vel)

        val relVel = axisNormalOnA.dot(vel)

        // apply displacement correction

        // positional error (zeroth order error)
        val diff = Stack.newVec3f()
        pointInA.sub(pointInB, diff)
        var depth = -diff.dot(axisNormalOnA)
        var lo = -1e38f
        var hi = 1e38f

        val minLimit = lowerLimit[limitIndex]
        val maxLimit = upperLimit[limitIndex]

        // handle the limits
        if (minLimit < maxLimit) {
            if (depth > maxLimit) {
                depth -= maxLimit
                lo = 0f
            } else {
                if (depth < minLimit) {
                    depth -= minLimit
                    hi = 0f
                } else {
                    Stack.subVec3f(6)
                    return 0f
                }
            }
        }

        var normalImpulse = limitSoftness * (restitution * depth / timeStep - damping * relVel) * jacDiagABInv

        val oldNormalImpulse = accumulatedImpulse[limitIndex]
        val sum = oldNormalImpulse + normalImpulse
        accumulatedImpulse[limitIndex] = if (sum > hi) 0f else if (sum < lo) 0f else sum
        normalImpulse = accumulatedImpulse[limitIndex] - oldNormalImpulse

        val impulseVector = Stack.newVec3f()
        axisNormalOnA.mul(normalImpulse, impulseVector)
        body1.applyImpulse(impulseVector, relPos1)
        impulseVector.negate()
        body2.applyImpulse(impulseVector, relPos2)

        Stack.subVec3f(7)
        return normalImpulse
    }
}
