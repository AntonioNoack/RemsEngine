package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.QuaternionUtil.quatRotate
import com.bulletphysics.linearmath.QuaternionUtil.shortestArcQuat
import com.bulletphysics.linearmath.ScalarUtil
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
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
    private val jacNormals = Array(3) { Vector3d() }
    private val jacInvDiagonals = DoubleArray(3)

    private val rbAFrame = Transform()
    private val rbBFrame = Transform()

    var limitSoftness = 0.0
    var biasFactor = 0.0
    var relaxationFactor = 0.0

    var swingSpan1 = 0.0
    var swingSpan2 = 0.0
    var twistSpan = 0.0

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

    var angularOnly = false

    var solveTwistLimit: Boolean = false
        private set

    private var solveSwingLimit = false

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
            rigidBodyA.worldTransform.transformPosition(pivotAInW)

            val pivotBInW = Stack.newVec(rbBFrame.origin)
            rigidBodyB.worldTransform.transformPosition(pivotBInW)

            val relPos = Stack.newVec()
            pivotBInW.sub(pivotAInW, relPos)

            val normal = jacNormals
            if (relPos.lengthSquared() > BulletGlobals.FLT_EPSILON) {
                relPos.normalize(normal[0])
            } else {
                normal[0].set(1.0, 0.0, 0.0)
            }

            normal[0].findSystem(normal[1], normal[2], false)

            for (i in 0..2) {

                pivotAInW.sub(rigidBodyA.worldTransform.origin, tmp1)
                pivotBInW.sub(rigidBodyB.worldTransform.origin, tmp2)

                jacInvDiagonals[i] = JacobianEntry.calculateDiagonalInv(
                    rigidBodyA.worldTransform.basis, rigidBodyB.worldTransform.basis,
                    tmp1, tmp2, normal[i],
                    rigidBodyA.invInertiaLocal, rigidBodyA.inverseMass,
                    rigidBodyB.invInertiaLocal, rigidBodyB.inverseMass
                )
            }

            Stack.subVec(3)
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

        val rMaxAngle1Sq = 1.0 / (swingSpan1 * swingSpan1)
        val rMaxAngle2Sq = 1.0 / (swingSpan2 * swingSpan2)
        val ellipseAngle = abs(swing1 * swing1) * rMaxAngle1Sq + abs(swing2 * swing2) * rMaxAngle2Sq

        if (ellipseAngle > 1.0) {
            swingCorrection = ellipseAngle - 1.0
            solveSwingLimit = true

            // Calculate necessary axis & factors
            b1Axis2.mul(b2Axis1.dot(b1Axis2), tmp1)
            b1Axis3.mul(b2Axis1.dot(b1Axis3), tmp2)
            tmp1.add(tmp2, tmp)
            b2Axis1.cross(tmp, swingAxis)
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

                b2Axis1.add(b1Axis1, twistAxis)
                twistAxis.mul(0.5)
                twistAxis.normalize()
                twistAxis.mul(-1.0)

                kTwist = 1.0 / (rigidBodyA.computeAngularImpulseDenominator(twistAxis) +
                        rigidBodyB.computeAngularImpulseDenominator(twistAxis))
            } else if (twist > twistSpan * lockedFreeFactor) {
                twistCorrection = (twist - twistSpan)
                solveTwistLimit = true

                b2Axis1.add(b1Axis1, twistAxis)
                twistAxis.mul(0.5)
                twistAxis.normalize()

                kTwist = 1.0 / (rigidBodyA.computeAngularImpulseDenominator(twistAxis) +
                        rigidBodyB.computeAngularImpulseDenominator(twistAxis))
            }
        }
    }

    override fun solveConstraint(timeStep: Double) {

        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()
        val tmp3 = Stack.newVec()

        val pivotAInW = Stack.newVec(rbAFrame.origin)
        rigidBodyA.worldTransform.transformPosition(pivotAInW)

        val pivotBInW = Stack.newVec(rbBFrame.origin)
        rigidBodyB.worldTransform.transformPosition(pivotBInW)

        val tau = 0.3

        // linear part
        if (!angularOnly) {
            val relPos1 = Stack.newVec()
            pivotAInW.sub(rigidBodyA.worldTransform.origin, relPos1)

            val relPos2 = Stack.newVec()
            pivotBInW.sub(rigidBodyB.worldTransform.origin, relPos2)

            val vel1 = rigidBodyA.getVelocityInLocalPoint(relPos1, Stack.newVec())
            val vel2 = rigidBodyB.getVelocityInLocalPoint(relPos2, Stack.newVec())
            val relVel = Stack.newVec()
            vel1.sub(vel2, relVel)

            val impulseVector = Stack.newVec()
            for (i in 0..2) {
                val normal = jacNormals[i]
                val jacDiagABInv = jacInvDiagonals[i]

                val relVel = normal.dot(relVel)
                // positional error (zeroth order error)
                pivotAInW.sub(pivotBInW, tmp1)
                val depth = -(tmp1).dot(normal) // this is the error projected on the normal
                val impulse = depth * tau / timeStep * jacDiagABInv - relVel * jacDiagABInv
                if (impulse > breakingImpulseThreshold) {
                    isBroken = true
                    break
                }

                appliedImpulse += impulse
                normal.mul(impulse, impulseVector)

                pivotAInW.sub(rigidBodyA.getCenterOfMassPosition(tmp3), tmp1)
                rigidBodyA.applyImpulse(impulseVector, tmp1)

                impulseVector.negate(tmp1)
                pivotBInW.sub(rigidBodyB.getCenterOfMassPosition(tmp3), tmp2)
                rigidBodyB.applyImpulse(tmp1, tmp2)
            }
            Stack.subVec(6)
        }

        run {
            // solve angular part
            val angVelA = rigidBodyA.angularVelocity
            val angVelB = rigidBodyB.angularVelocity
            val impulse = Stack.newVec()

            // solve swing limit
            if (solveSwingLimit) {
                angVelB.sub(angVelA, tmp1)
                val amplitude =
                    ((tmp1).dot(swingAxis) * relaxationFactor * relaxationFactor + swingCorrection * (1.0 / timeStep) * biasFactor)
                var impulseMag = amplitude * kSwing

                // Clamp the accumulated impulse
                val temp = accSwingLimitImpulse
                accSwingLimitImpulse = max(accSwingLimitImpulse + impulseMag, 0.0)
                impulseMag = accSwingLimitImpulse - temp

                if (abs(impulseMag) > breakingImpulseThreshold) {
                    isBroken = true
                } else {
                    swingAxis.mul(impulseMag, impulse)
                    rigidBodyA.applyTorqueImpulse(impulse)

                    impulse.negate(tmp1)
                    rigidBodyB.applyTorqueImpulse(tmp1)
                }
            }

            // solve twist limit
            if (solveTwistLimit) {
                angVelB.sub(angVelA, tmp1)
                val amplitude =
                    (tmp1.dot(twistAxis) * relaxationFactor * relaxationFactor + twistCorrection * (1.0 / timeStep) * biasFactor)
                var impulseMag = amplitude * kTwist

                // Clamp the accumulated impulse
                val temp = accTwistLimitImpulse
                accTwistLimitImpulse = max(accTwistLimitImpulse + impulseMag, 0.0)
                impulseMag = accTwistLimitImpulse - temp

                if (abs(impulseMag) > breakingImpulseThreshold) {
                    isBroken = true
                } else {
                    twistAxis.mul(impulseMag, impulse)
                    rigidBodyA.applyTorqueImpulse(impulse)

                    impulse.negate(tmp1)
                    rigidBodyB.applyTorqueImpulse(tmp1)
                }
            }
            Stack.subVec(3)
        }

        Stack.subVec(3)
    }
}
