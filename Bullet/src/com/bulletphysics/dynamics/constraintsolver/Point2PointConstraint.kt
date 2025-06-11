package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
import com.bulletphysics.util.setTranspose
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
    private val jac: Array<JacobianEntry> = arrayOf(JacobianEntry(), JacobianEntry(), JacobianEntry())

    private val pivotInA = Vector3d()
    private val pivotInB = Vector3d()

    var setting: ConstraintSetting = ConstraintSetting()

    @Suppress("unused")
    constructor() : super()

    constructor(rbA: RigidBody, rbB: RigidBody, pivotInA: Vector3d, pivotInB: Vector3d) : super(rbA, rbB) {
        this.pivotInA.set(pivotInA)
        this.pivotInB.set(pivotInB)
    }

    @Suppress("unused")
    constructor(rbA: RigidBody, pivotInA: Vector3d) : super(rbA) {
        this.pivotInA.set(pivotInA)
        this.pivotInB.set(pivotInA)
        rbA.getCenterOfMassTransform(Stack.newTrans()).transform(this.pivotInB)
    }

    override fun buildJacobian() {
        appliedImpulse = 0.0

        val normal = Stack.newVec()
        normal.set(0.0, 0.0, 0.0)

        val tmpMat1 = Stack.newMat()
        val tmpMat2 = Stack.newMat()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()
        val tmpVec = Stack.newVec()

        val centerOfMassA = rigidBodyA.getCenterOfMassTransform(Stack.newTrans())
        val centerOfMassB = rigidBodyB.getCenterOfMassTransform(Stack.newTrans())

        for (i in 0..2) {
            setCoord(normal, i, 1.0)

            tmpMat1.setTranspose(centerOfMassA.basis)
            tmpMat2.setTranspose(centerOfMassB.basis)

            tmp1.set(pivotInA)
            centerOfMassA.transform(tmp1)
            tmp1.sub(rigidBodyA.getCenterOfMassPosition(tmpVec))

            tmp2.set(pivotInB)
            centerOfMassB.transform(tmp2)
            tmp2.sub(rigidBodyB.getCenterOfMassPosition(tmpVec))

            jac[i].init(
                tmpMat1, tmpMat2, tmp1, tmp2, normal,
                rigidBodyA.getInvInertiaDiagLocal(Stack.newVec()),
                rigidBodyA.inverseMass,
                rigidBodyB.getInvInertiaDiagLocal(Stack.newVec()),
                rigidBodyB.inverseMass
            )
            setCoord(normal, i, 0.0)
        }
    }

    override fun solveConstraint(timeStep: Double) {
        val tmp = Stack.newVec()
        val tmp2 = Stack.newVec()
        val tmpVec = Stack.newVec()

        val centerOfMassA = rigidBodyA.getCenterOfMassTransform(Stack.newTrans())
        val centerOfMassB = rigidBodyB.getCenterOfMassTransform(Stack.newTrans())

        val pivotAInW = Stack.newVec(pivotInA)
        centerOfMassA.transform(pivotAInW)

        val pivotBInW = Stack.newVec(pivotInB)
        centerOfMassB.transform(pivotBInW)

        val normal = Stack.newVec()
        normal.set(0.0, 0.0, 0.0)

        //btVector3 angvelA = m_rbA.getCenterOfMassTransform().getBasis().transpose() * m_rbA.getAngularVelocity();
        //btVector3 angvelB = m_rbB.getCenterOfMassTransform().getBasis().transpose() * m_rbB.getAngularVelocity();
        val relPos1 = Stack.newVec()
        val relPos2 = Stack.newVec()
        val vel1 = Stack.newVec()
        val vel2 = Stack.newVec()
        val vel = Stack.newVec()
        val impulseVector = Stack.newVec()

        for (i in 0..2) {
            setCoord(normal, i, 1.0)
            val jacDiagABInv = 1.0 / jac[i].diagonal

            relPos1.setSub(pivotAInW, rigidBodyA.getCenterOfMassPosition(tmpVec))
            relPos2.setSub(pivotBInW, rigidBodyB.getCenterOfMassPosition(tmpVec))

            // this jacobian entry could be re-used for all iterations
            rigidBodyA.getVelocityInLocalPoint(relPos1, vel1)
            rigidBodyB.getVelocityInLocalPoint(relPos2, vel2)
            vel.setSub(vel1, vel2)

            val relativeVelocity = normal.dot(vel)

            /*
			//velocity error (first order error)
			btScalar rel_vel = m_jac[i].getRelativeVelocity(m_rbA.getLinearVelocity(),angvelA,
			m_rbB.getLinearVelocity(),angvelB);
			 */

            // positional error (zeroth order error)
            tmp.setSub(pivotAInW, pivotBInW)
            val depth = -tmp.dot(normal) //this is the error projected on the normal

            var impulse =
                depth * setting.tau / timeStep * jacDiagABInv - setting.damping * relativeVelocity * jacDiagABInv
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
            impulseVector.setScale(impulse, normal)
            tmp.setSub(pivotAInW, rigidBodyA.getCenterOfMassPosition(tmpVec))
            rigidBodyA.applyImpulse(impulseVector, tmp)
            tmp.setNegate(impulseVector)
            tmp2.setSub(pivotBInW, rigidBodyB.getCenterOfMassPosition(tmpVec))
            rigidBodyB.applyImpulse(tmp, tmp2)

            setCoord(normal, i, 0.0)
        }

        Stack.subVec(12)
        Stack.subTrans(2)
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    class ConstraintSetting {
        var tau: Double = 0.3
        var damping: Double = 1.0
        var impulseClamp: Double = 0.0
    }
}
