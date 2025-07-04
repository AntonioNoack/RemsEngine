package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub

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
    val lowerLimit = Vector3d()

    @JvmField
    val upperLimit = Vector3d()

    @JvmField
    val accumulatedImpulse = Vector3d()

    /**
     * Softness for linear limit
     * */
    var limitSoftness: Double

    /**
     * Damping for linear limit
     * */
    var damping: Double

    /**
     * Bounce parameter for linear limit
     * */
    var restitution: Double

    constructor() {
        lowerLimit.set(0.0, 0.0, 0.0)
        upperLimit.set(0.0, 0.0, 0.0)
        accumulatedImpulse.set(0.0, 0.0, 0.0)

        limitSoftness = 0.7
        damping = 1.0
        restitution = 0.5
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
        return (getCoord(upperLimit, limitIndex) >= getCoord(lowerLimit, limitIndex))
    }

    fun solveLinearAxis(
        timeStep: Double, jacDiagABInv: Double, body1: RigidBody, pointInA: Vector3d,
        body2: RigidBody, pointInB: Vector3d, limitIndex: Int,
        axisNormalOnA: Vector3d, anchorPos: Vector3d
    ): Double {
        val tmp = Stack.newVec()
        val tmpVec = Stack.newVec()

        // find relative velocity
        val relPos1 = Stack.newVec()
        //relPos1.sub(pointInA, body1.getCenterOfMassPosition(tmpVec));
        relPos1.setSub(anchorPos, body1.getCenterOfMassPosition(tmpVec))

        val relPos2 = Stack.newVec()
        //relPos2.sub(pointInB, body2.getCenterOfMassPosition(tmpVec));
        relPos2.setSub(anchorPos, body2.getCenterOfMassPosition(tmpVec))

        val vel1 = body1.getVelocityInLocalPoint(relPos1, Stack.newVec())
        val vel2 = body2.getVelocityInLocalPoint(relPos2, Stack.newVec())
        val vel = Stack.newVec()
        vel.setSub(vel1, vel2)

        val relVel = axisNormalOnA.dot(vel)

        // apply displacement correction

        // positional error (zeroth order error)
        tmp.setSub(pointInA, pointInB)
        var depth = -(tmp).dot(axisNormalOnA)
        var lo = -1e308
        var hi = 1e308

        val minLimit = getCoord(lowerLimit, limitIndex)
        val maxLimit = getCoord(upperLimit, limitIndex)

        // handle the limits
        if (minLimit < maxLimit) {
            if (depth > maxLimit) {
                depth -= maxLimit
                lo = 0.0
            } else {
                if (depth < minLimit) {
                    depth -= minLimit
                    hi = 0.0
                } else {
                    return 0.0
                }
            }
        }

        var normalImpulse = limitSoftness * (restitution * depth / timeStep - damping * relVel) * jacDiagABInv

        val oldNormalImpulse = getCoord(accumulatedImpulse, limitIndex)
        val sum = oldNormalImpulse + normalImpulse
        setCoord(accumulatedImpulse, limitIndex, if (sum > hi) 0.0 else if (sum < lo) 0.0 else sum)
        normalImpulse = getCoord(accumulatedImpulse, limitIndex) - oldNormalImpulse

        val impulseVector = Stack.newVec()
        impulseVector.setScale(normalImpulse, axisNormalOnA)
        body1.applyImpulse(impulseVector, relPos1)

        impulseVector.negate(tmp)
        body2.applyImpulse(tmp, relPos2)

        Stack.subVec(8)
        return normalImpulse
    }
}
