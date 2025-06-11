package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.QuaternionUtil.quatRotate
import com.bulletphysics.linearmath.QuaternionUtil.shortestArcQuat
import com.bulletphysics.linearmath.ScalarUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setNormalize
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
import kotlin.math.abs
import kotlin.math.max

/**
 * ConeTwistConstraint can be used to simulate ragdoll joints (upper arm, leg etc).
 *
 * @author jezek2
 */
@Suppress("unused")
class ConeTwistConstraint : TypedConstraint {

    /**
     * 3 orthogonal linear constraints
     * */
    private val jac = arrayOf(JacobianEntry(), JacobianEntry(), JacobianEntry())

    private val rbAFrame = Transform()
    private val rbBFrame = Transform()

    private var limitSoftness = 0.0
    private var biasFactor = 0.0
    private var relaxationFactor = 0.0

    private var swingSpan1 = 0.0
    private var swingSpan2 = 0.0
    private var twistSpan = 0.0

    private val swingAxis = Vector3d()
    private val twistAxis = Vector3d()

    private var kSwing = 0.0
    private var kTwist = 0.0

    @get:Suppress("unused")
    var twistLimitSign: Double = 0.0
        private set
    private var swingCorrection = 0.0
    private var twistCorrection = 0.0

    private var accSwingLimitImpulse = 0.0
    private var accTwistLimitImpulse = 0.0

    private var angularOnly = false

    @get:Suppress("unused")
    var solveTwistLimit: Boolean = false
        private set
    private var solveSwingLimit = false

    @Suppress("unused")
    constructor() : super()

    @Suppress("unused")
    constructor(rbA: RigidBody, rbB: RigidBody, rbAFrame: Transform, rbBFrame: Transform) : super(rbA, rbB) {
        this.rbAFrame.set(rbAFrame)
        this.rbBFrame.set(rbBFrame)

        swingSpan1 = 1e308
        swingSpan2 = 1e308
        twistSpan = 1e308
        biasFactor = 0.3
        relaxationFactor = 1.0

        solveTwistLimit = false
        solveSwingLimit = false
    }

    @Suppress("unused")
    constructor(rbA: RigidBody, rbAFrame: Transform) : super(rbA) {
        this.rbAFrame.set(rbAFrame)
        this.rbBFrame.set(this.rbAFrame)

        swingSpan1 = 1e308
        swingSpan2 = 1e308
        twistSpan = 1e308
        biasFactor = 0.3
        relaxationFactor = 1.0

        solveTwistLimit = false
        solveSwingLimit = false
    }

    override fun buildJacobian() {
        val tmp = Stack.newVec()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        val tmpTrans = Stack.newTrans()

        appliedImpulse = 0.0

        // set bias, sign, clear accumulator
        swingCorrection = 0.0
        twistLimitSign = 0.0
        solveTwistLimit = false
        solveSwingLimit = false
        accTwistLimitImpulse = 0.0
        accSwingLimitImpulse = 0.0

        if (!angularOnly) {
            val pivotAInW = Stack.newVec(rbAFrame.origin)
            rigidBodyA.getCenterOfMassTransform(tmpTrans).transform(pivotAInW)

            val pivotBInW = Stack.newVec(rbBFrame.origin)
            rigidBodyB.getCenterOfMassTransform(tmpTrans).transform(pivotBInW)

            val relPos = Stack.newVec()
            relPos.setSub(pivotBInW, pivotAInW)

            // TODO: stack
            val normal = arrayOf(Stack.newVec(), Stack.newVec(), Stack.newVec())
            if (relPos.lengthSquared() > BulletGlobals.FLT_EPSILON) {
                normal[0].setNormalize(relPos)
            } else {
                normal[0].set(1.0, 0.0, 0.0)
            }

            TransformUtil.planeSpace1(normal[0], normal[1], normal[2])

            for (i in 0..2) {
                val mat1 = rigidBodyA.getCenterOfMassTransform(Stack.newTrans()).basis
                mat1.transpose()

                val mat2 = rigidBodyB.getCenterOfMassTransform(Stack.newTrans()).basis
                mat2.transpose()

                tmp1.setSub(pivotAInW, rigidBodyA.getCenterOfMassPosition(tmp))
                tmp2.setSub(pivotBInW, rigidBodyB.getCenterOfMassPosition(tmp))

                jac[i].init(
                    mat1, mat2, tmp1, tmp2, normal[i],
                    rigidBodyA.getInvInertiaDiagLocal(Stack.newVec()),
                    rigidBodyA.inverseMass,
                    rigidBodyB.getInvInertiaDiagLocal(Stack.newVec()),
                    rigidBodyB.inverseMass
                )
            }
        }

        val b1Axis1 = Stack.newVec()
        val b1Axis2 = Stack.newVec()
        val b1Axis3 = Stack.newVec()
        val b2Axis1 = Stack.newVec()
        val b2Axis2 = Stack.newVec()

        rbAFrame.basis.getColumn(0, b1Axis1)
        rigidBodyA.getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis1)

        rbBFrame.basis.getColumn(0, b2Axis1)
        rigidBodyB.getCenterOfMassTransform(tmpTrans).basis.transform(b2Axis1)

        var swing1 = 0.0
        var swing2 = 0.0

        var swx: Double
        var swy: Double
        val thresh = 10.0
        var fact: Double

        // Get Frame into world space
        if (swingSpan1 >= 0.05f) {
            rbAFrame.basis.getColumn(1, b1Axis2)
            rigidBodyA.getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis2)
            //			swing1 = ScalarUtil.atan2Fast(b2Axis1.dot(b1Axis2), b2Axis1.dot(b1Axis1));
            swx = b2Axis1.dot(b1Axis1)
            swy = b2Axis1.dot(b1Axis2)
            swing1 = ScalarUtil.atan2Fast(swy, swx)
            fact = (swy * swy + swx * swx) * thresh * thresh
            fact = fact / (fact + 1.0)
            swing1 *= fact
        }

        if (swingSpan2 >= 0.05f) {
            rbAFrame.basis.getColumn(2, b1Axis3)
            rigidBodyA.getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis3)
            swx = b2Axis1.dot(b1Axis1)
            swy = b2Axis1.dot(b1Axis3)
            swing2 = ScalarUtil.atan2Fast(swy, swx)
            fact = (swy * swy + swx * swx) * thresh * thresh
            fact = fact / (fact + 1.0)
            swing2 *= fact
        }

        val RMaxAngle1Sq = 1.0 / (swingSpan1 * swingSpan1)
        val RMaxAngle2Sq = 1.0 / (swingSpan2 * swingSpan2)
        val EllipseAngle = abs(swing1 * swing1) * RMaxAngle1Sq + abs(swing2 * swing2) * RMaxAngle2Sq

        if (EllipseAngle > 1.0) {
            swingCorrection = EllipseAngle - 1.0
            solveSwingLimit = true

            // Calculate necessary axis & factors
            tmp1.setScale(b2Axis1.dot(b1Axis2), b1Axis2)
            tmp2.setScale(b2Axis1.dot(b1Axis3), b1Axis3)
            tmp.setAdd(tmp1, tmp2)
            swingAxis.setCross(b2Axis1, tmp)
            swingAxis.normalize()

            val swingAxisSign = if (b2Axis1.dot(b1Axis1) >= 0.0) 1.0 else -1.0
            swingAxis.mul(swingAxisSign)

            kSwing = 1.0 / (rigidBodyA.computeAngularImpulseDenominator(swingAxis) +
                    rigidBodyB.computeAngularImpulseDenominator(swingAxis))
        }

        // Twist limits
        if (twistSpan >= 0.0) {
            rbBFrame.basis.getColumn(1, b2Axis2)
            rigidBodyB.getCenterOfMassTransform(tmpTrans).basis.transform(b2Axis2)

            val rotationArc = shortestArcQuat(b2Axis1, b1Axis1, Stack.newQuat())
            val twistRef = quatRotate(rotationArc, b2Axis2, Stack.newVec())
            val twist = ScalarUtil.atan2Fast(twistRef.dot(b1Axis3), twistRef.dot(b1Axis2))

            val lockedFreeFactor = if (twistSpan > 0.05f) limitSoftness else 0.0
            if (twist <= -twistSpan * lockedFreeFactor) {
                twistCorrection = -(twist + twistSpan)
                solveTwistLimit = true

                twistAxis.setAdd(b2Axis1, b1Axis1)
                twistAxis.mul(0.5)
                twistAxis.normalize()
                twistAxis.mul(-1.0)

                kTwist = 1.0 / (rigidBodyA.computeAngularImpulseDenominator(twistAxis) +
                        rigidBodyB.computeAngularImpulseDenominator(twistAxis))
            } else if (twist > twistSpan * lockedFreeFactor) {
                twistCorrection = (twist - twistSpan)
                solveTwistLimit = true

                twistAxis.setAdd(b2Axis1, b1Axis1)
                twistAxis.mul(0.5)
                twistAxis.normalize()

                kTwist = 1.0 / (rigidBodyA.computeAngularImpulseDenominator(twistAxis) +
                        rigidBodyB.computeAngularImpulseDenominator(twistAxis))
            }
        }
    }

    override fun solveConstraint(timeStep: Double) {
        val tmp = Stack.newVec()
        val tmp2 = Stack.newVec()

        val tmpVec = Stack.newVec()
        val tmpTrans = Stack.newTrans()

        val pivotAInW = Stack.newVec(rbAFrame.origin)
        rigidBodyA.getCenterOfMassTransform(tmpTrans).transform(pivotAInW)

        val pivotBInW = Stack.newVec(rbBFrame.origin)
        rigidBodyB.getCenterOfMassTransform(tmpTrans).transform(pivotBInW)

        val tau = 0.3

        // linear part
        if (!angularOnly) {
            val relPos1 = Stack.newVec()
            relPos1.setSub(pivotAInW, rigidBodyA.getCenterOfMassPosition(tmpVec))

            val relPos2 = Stack.newVec()
            relPos2.setSub(pivotBInW, rigidBodyB.getCenterOfMassPosition(tmpVec))

            val vel1 = rigidBodyA.getVelocityInLocalPoint(relPos1, Stack.newVec())
            val vel2 = rigidBodyB.getVelocityInLocalPoint(relPos2, Stack.newVec())
            val vel = Stack.newVec()
            vel.setSub(vel1, vel2)

            val impulseVector = Stack.newVec()
            for (i in 0..2) {
                val normal = jac[i].linearJointAxis
                val jacDiagABInv = 1.0 / jac[i].diagonal

                val relVel = normal.dot(vel)
                // positional error (zeroth order error)
                tmp.setSub(pivotAInW, pivotBInW)
                val depth = -(tmp).dot(normal) // this is the error projected on the normal
                val impulse = depth * tau / timeStep * jacDiagABInv - relVel * jacDiagABInv
                if (impulse > breakingImpulseThreshold) {
                    isBroken = true
                    break
                }

                appliedImpulse += impulse
                impulseVector.setScale(impulse, normal)

                tmp.setSub(pivotAInW, rigidBodyA.getCenterOfMassPosition(tmpVec))
                rigidBodyA.applyImpulse(impulseVector, tmp)

                tmp.setNegate(impulseVector)
                tmp2.setSub(pivotBInW, rigidBodyB.getCenterOfMassPosition(tmpVec))
                rigidBodyB.applyImpulse(tmp, tmp2)
            }
            Stack.subVec(6)
        }

        run {
            // solve angular part
            val angVelA = rigidBodyA.getAngularVelocity(Stack.newVec())
            val angVelB = rigidBodyB.getAngularVelocity(Stack.newVec())
            val impulse = Stack.newVec()

            // solve swing limit
            if (solveSwingLimit) {
                tmp.setSub(angVelB, angVelA)
                val amplitude =
                    ((tmp).dot(swingAxis) * relaxationFactor * relaxationFactor + swingCorrection * (1.0 / timeStep) * biasFactor)
                var impulseMag = amplitude * kSwing

                // Clamp the accumulated impulse
                val temp = accSwingLimitImpulse
                accSwingLimitImpulse = max(accSwingLimitImpulse + impulseMag, 0.0)
                impulseMag = accSwingLimitImpulse - temp

                if (abs(impulseMag) > breakingImpulseThreshold) {
                    isBroken = true
                } else {
                    impulse.setScale(impulseMag, swingAxis)
                    rigidBodyA.applyTorqueImpulse(impulse)

                    tmp.setNegate(impulse)
                    rigidBodyB.applyTorqueImpulse(tmp)
                }
            }

            // solve twist limit
            if (solveTwistLimit) {
                tmp.setSub(angVelB, angVelA)
                val amplitude =
                    (tmp.dot(twistAxis) * relaxationFactor * relaxationFactor + twistCorrection * (1.0 / timeStep) * biasFactor)
                var impulseMag = amplitude * kTwist

                // Clamp the accumulated impulse
                val temp = accTwistLimitImpulse
                accTwistLimitImpulse = max(accTwistLimitImpulse + impulseMag, 0.0)
                impulseMag = accTwistLimitImpulse - temp

                if (abs(impulseMag) > breakingImpulseThreshold) {
                    isBroken = true
                } else {
                    impulse.setScale(impulseMag, twistAxis)
                    rigidBodyA.applyTorqueImpulse(impulse)

                    tmp.setNegate(impulse)
                    rigidBodyB.applyTorqueImpulse(tmp)
                }
            }
            Stack.subVec(3)
        }

        Stack.subVec(5)
        Stack.subTrans(1)
    }

    fun updateRHS(timeStep: Double) {
    }

    @Suppress("unused")
    fun setAngularOnly(angularOnly: Boolean) {
        this.angularOnly = angularOnly
    }

    @Suppress("unused")
    fun setLimit(swingSpan1: Double, swingSpan2: Double, twistSpan: Double) {
        setLimit(swingSpan1, swingSpan2, twistSpan, 0.8, 0.3, 1.0)
    }

    fun setLimit(
        swingSpan1: Double,
        swingSpan2: Double,
        twistSpan: Double,
        limitSoftness: Double,
        biasFactor: Double,
        relaxationFactor: Double
    ) {
        this.swingSpan1 = swingSpan1
        this.swingSpan2 = swingSpan2
        this.twistSpan = twistSpan

        this.limitSoftness = limitSoftness
        this.biasFactor = biasFactor
        this.relaxationFactor = relaxationFactor
    }
}
