package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import cz.advel.stack.Stack
import org.joml.Vector3d
import kotlin.math.abs

/**
 * Point to point constraint between two rigid bodies each with a pivot point that
 * describes the "ballsocket" location in local space.
 *
 * @author jezek2
 */
class Point2PointConstraint : TypedConstraint {

    /**
     * 3 orthogonal linear constraints
     * */
    private val jacobianInvDiagonals = DoubleArray(3)

    private val pivotInA = Vector3d()
    private val pivotInB = Vector3d()

    var setting: ConstraintSetting = ConstraintSetting()

    @Suppress("unused")
    constructor() : super()

    constructor(rbA: RigidBody, rbB: RigidBody, pivotInA: Vector3d, pivotInB: Vector3d) : super(rbA, rbB) {
        this.pivotInA.set(pivotInA)
        this.pivotInB.set(pivotInB)
    }

    override fun buildJacobian() {
        appliedImpulse = 0.0

        val normal = Stack.newVec()
        normal.set(0.0)

        val globalPivotRelativeToA = Stack.newVec()
        val globalPivotRelativeToB = Stack.newVec()

        val transformA = rigidBodyA.worldTransform
        val transformB = rigidBodyB.worldTransform

        for (i in 0..2) {
            normal[i] = 1.0

            transformA.transformPosition(pivotInA, globalPivotRelativeToA)
            globalPivotRelativeToA.sub(transformA.origin)

            transformB.transformPosition(pivotInB, globalPivotRelativeToB)
            globalPivotRelativeToB.sub(transformB.origin)

            jacobianInvDiagonals[i] = JacobianEntry.calculateDiagonalInv(
                transformA.basis, transformB.basis,
                globalPivotRelativeToA, globalPivotRelativeToB, normal,
                rigidBodyA.invInertiaLocal, rigidBodyA.inverseMass,
                rigidBodyB.invInertiaLocal, rigidBodyB.inverseMass
            )
            normal[i] = 0.0
        }

        Stack.subVec(3)
    }

    override fun solveConstraint(timeStep: Double) {
        val tmp = Stack.newVec()
        val tmp2 = Stack.newVec()

        val centerOfMassA = rigidBodyA.worldTransform
        val centerOfMassB = rigidBodyB.worldTransform

        val pivotAInW = Stack.newVec(pivotInA)
        centerOfMassA.transformPosition(pivotAInW)

        val pivotBInW = Stack.newVec(pivotInB)
        centerOfMassB.transformPosition(pivotBInW)

        val normal = Stack.newVec()
        normal.set(0.0, 0.0, 0.0)

        val relPos1 = Stack.newVec()
        val relPos2 = Stack.newVec()
        val vel1 = Stack.newVec()
        val vel2 = Stack.newVec()
        val vel = Stack.newVec()
        val impulseVector = Stack.newVec()

        for (i in 0..2) {
            normal[i] = 1.0

            val jacDiagABInv = jacobianInvDiagonals[i]

            pivotAInW.sub(rigidBodyA.worldTransform.origin, relPos1)
            pivotBInW.sub(rigidBodyB.worldTransform.origin, relPos2)

            // this jacobian entry could be re-used for all iterations
            rigidBodyA.getVelocityInLocalPoint(relPos1, vel1)
            rigidBodyB.getVelocityInLocalPoint(relPos2, vel2)
            vel1.sub(vel2, vel)

            val relativeVelocity = normal.dot(vel)

            /*
			//velocity error (first order error)
			btScalar rel_vel = m_jac[i].getRelativeVelocity(m_rbA.getLinearVelocity(),angvelA,
			m_rbB.getLinearVelocity(),angvelB);
			 */

            // positional error (zeroth order error)
            pivotAInW.sub(pivotBInW, tmp)
            val depth = -tmp.dot(normal) //this is the error projected on the normal

            var impulse = (depth * setting.tau / timeStep - setting.damping * relativeVelocity) * jacDiagABInv
            if (abs(impulse) > breakingImpulseThreshold) {
                isBroken = true
                break
            }

            val impulseClamp = setting.impulseClamp
            if (impulseClamp > 0.0) {
                if (impulse < -impulseClamp) {
                    impulse = -impulseClamp
                }
                if (impulse > impulseClamp) {
                    impulse = impulseClamp
                }
            }

            appliedImpulse += impulse
            normal.mul(impulse, impulseVector)
            pivotAInW.sub(rigidBodyA.worldTransform.origin, tmp)
            rigidBodyA.applyImpulse(impulseVector, tmp)

            impulseVector.negate(tmp)
            pivotBInW.sub(rigidBodyB.worldTransform.origin, tmp2)
            rigidBodyB.applyImpulse(tmp, tmp2)

            normal[i] = 0.0
        }

        Stack.subVec(11)
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    class ConstraintSetting {
        var tau: Double = 0.3
        var damping: Double = 1.0
        var impulseClamp: Double = 0.0
    }
}
