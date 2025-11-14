package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import me.anno.bullet.constraints.PointConstraint
import org.joml.Vector3d
import kotlin.math.abs

/**
 * Point to point constraint between two rigid bodies each with a pivot point that
 * describes the "ballsocket" location in local space.
 *
 * @author jezek2
 */
class Point2PointConstraint(
    private val settings: PointConstraint,
    rbA: RigidBody, rbB: RigidBody,
    private val pivotInA: Vector3d,
    private val pivotInB: Vector3d
) : TypedConstraint(rbA, rbB) {

    override var breakingImpulse: Float
        get() = settings.breakingImpulse
        set(value) {
            settings.breakingImpulse = value
        }

    /**
     * 3 orthogonal linear constraints
     * */
    private val jacobianInvDiagonals = FloatArray(3)

    override fun buildJacobian() {
        val normal = Stack.newVec3f().set(0f)

        val globalPivotRelativeToA = Stack.newVec3d()
        val globalPivotRelativeToB = Stack.newVec3d()

        val globalPivotRelativeToA1 = Stack.newVec3f()
        val globalPivotRelativeToB1 = Stack.newVec3f()

        val transformA = rigidBodyA.worldTransform
        val transformB = rigidBodyB.worldTransform

        for (i in 0..2) {
            normal[i] = 1f

            transformA.transformPosition(pivotInA, globalPivotRelativeToA)
            globalPivotRelativeToA.sub(transformA.origin)
            globalPivotRelativeToA1.set(globalPivotRelativeToA)

            transformB.transformPosition(pivotInB, globalPivotRelativeToB)
            globalPivotRelativeToB.sub(transformB.origin)
            globalPivotRelativeToB1.set(globalPivotRelativeToB)

            jacobianInvDiagonals[i] = JacobianEntry.calculateDiagonalInv(
                transformA.basis, transformB.basis,
                globalPivotRelativeToA1, globalPivotRelativeToB1, normal,
                rigidBodyA.invInertiaLocal, rigidBodyA.inverseMass,
                rigidBodyB.invInertiaLocal, rigidBodyB.inverseMass
            )
            normal[i] = 0f
        }

        Stack.subVec3f(3)
        Stack.subVec3d(2)
    }

    override fun solveConstraint(timeStep: Float) {
        val diffPos = Stack.newVec3f()
        val tmp2 = Stack.newVec3f()

        val centerOfMassA = rigidBodyA.worldTransform
        val centerOfMassB = rigidBodyB.worldTransform

        val pivotAInW = Stack.newVec3d(pivotInA)
        centerOfMassA.transformPosition(pivotAInW)

        val pivotBInW = Stack.newVec3d(pivotInB)
        centerOfMassB.transformPosition(pivotBInW)

        val normal = Stack.newVec3f().set(0f)

        val relPos1 = Stack.newVec3f()
        val relPos2 = Stack.newVec3f()
        val vel1 = Stack.newVec3f()
        val vel2 = Stack.newVec3f()
        val relVel = Stack.newVec3f()
        val impulseVector = Stack.newVec3f()

        pivotAInW.sub(rigidBodyA.worldTransform.origin, relPos1)
        pivotBInW.sub(rigidBodyB.worldTransform.origin, relPos2)
        pivotAInW.sub(pivotBInW, diffPos)

        val distance = diffPos.length()

        // plastic deformation
        val elasticRange = settings.elasticRange
        val elasticity = distance - settings.restLength
        if (elasticity !in elasticRange) {
            val reference = if (elasticity > 0f) elasticRange.min else elasticRange.max
            val excess = elasticity - reference
            val delta = settings.plasticDeformationRate * excess
            settings.restLength += delta
            if (settings.restLength !in settings.plasticRange) {
                isBroken = true
            }
        }

        // rest-length adjustment
        if (settings.restLength > 0f && distance > 1e-20f) {
            diffPos.mul(1f - settings.restLength / distance)
        }

        val tau = settings.tau / timeStep
        if (!isBroken) for (i in 0..2) {
            normal[i] = 1f

            val jacDiagABInv = jacobianInvDiagonals[i]

            // this jacobian entry could be re-used for all iterations
            rigidBodyA.getVelocityInLocalPoint(relPos1, vel1)
            rigidBodyB.getVelocityInLocalPoint(relPos2, vel2)
            vel1.sub(vel2, relVel)

            val relativeVelocity = normal.dot(relVel)

            // positional error (zeroth order error)
            val depth = -diffPos.dot(normal) // this is the error projected on the normal

            var impulse = (depth * tau - settings.damping * relativeVelocity) * jacDiagABInv
            if (abs(impulse) > breakingImpulse) {
                isBroken = true
                break
            }

            val impulseClamp = settings.impulseClamp
            if (impulseClamp > 0f) {
                if (impulse < -impulseClamp) {
                    impulse = -impulseClamp
                }
                if (impulse > impulseClamp) {
                    impulse = impulseClamp
                }
            }

            normal.mul(impulse, impulseVector)
            pivotAInW.sub(rigidBodyA.worldTransform.origin, tmp2)
            rigidBodyA.applyImpulse(impulseVector, tmp2)

            impulseVector.negate()
            pivotBInW.sub(rigidBodyB.worldTransform.origin, tmp2)
            rigidBodyB.applyImpulse(impulseVector, tmp2)

            normal[i] = 0f
        }

        Stack.subVec3f(9)
        Stack.subVec3d(2)
    }

}
