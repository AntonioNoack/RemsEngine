package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.ScalarUtil
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.maths.Maths.sq
import org.joml.Vector3f
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
    private val jacNormals = Array(3) { Vector3f() }
    private val jacInvDiagonals = FloatArray(3)

    private val rbAFrame = Transform()
    private val rbBFrame = Transform()

    var limitSoftness = 0f
    var biasFactor = 0f
    var relaxationFactor = 0f

    var swingSpan1 = 0f
    var swingSpan2 = 0f
    var twistSpan = 0f

    private val swingAxis = Vector3f()
    private val twistAxis = Vector3f()

    private var kSwing = 0f
    private var kTwist = 0f

    @get:Suppress("unused")
    var twistLimitSign = 0f
        private set

    private var swingCorrection = 0f
    private var twistCorrection = 0f

    private var accSwingLimitImpulse = 0f
    private var accTwistLimitImpulse = 0f

    var angularOnly = false

    var solveTwistLimit: Boolean = false
        private set

    private var solveSwingLimit = false

    constructor(rbA: RigidBody, rbB: RigidBody, rbAFrame: Transform, rbBFrame: Transform) : super(rbA, rbB) {
        this.rbAFrame.set(rbAFrame)
        this.rbBFrame.set(rbBFrame)

        swingSpan1 = 1e38f
        swingSpan2 = 1e38f
        twistSpan = 1e38f
        biasFactor = 0.3f
        relaxationFactor = 1f

        solveTwistLimit = false
        solveSwingLimit = false
    }

    override fun buildJacobian() {
        val tmp = Stack.newVec3f()
        val tmp1 = Stack.newVec3f()
        val tmp2 = Stack.newVec3f()

        val tmpTrans = Stack.newTrans()

        appliedImpulse = 0f

        // set bias, sign, clear accumulator
        swingCorrection = 0f
        twistLimitSign = 0f
        solveTwistLimit = false
        solveSwingLimit = false
        accTwistLimitImpulse = 0f
        accSwingLimitImpulse = 0f

        if (!angularOnly) {
            val pivotAInW = Stack.newVec3d(rbAFrame.origin)
            rigidBodyA.worldTransform.transformPosition(pivotAInW)

            val pivotBInW = Stack.newVec3d(rbBFrame.origin)
            rigidBodyB.worldTransform.transformPosition(pivotBInW)

            val relPos = Stack.newVec3f()
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

            Stack.subVec3d(3)
        }

        val b1Axis1 = Stack.newVec3f()
        val b1Axis2 = Stack.newVec3f()
        val b1Axis3 = Stack.newVec3f()
        val b2Axis1 = Stack.newVec3f()
        val b2Axis2 = Stack.newVec3f()

        rbAFrame.basis.getColumn(0, b1Axis1)
        rigidBodyA.getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis1)

        rbBFrame.basis.getColumn(0, b2Axis1)
        rigidBodyB.getCenterOfMassTransform(tmpTrans).basis.transform(b2Axis1)

        var swing1 = 0f
        var swing2 = 0f

        var swx: Float
        var swy: Float
        val threshold = 10f
        var factor: Float

        // Get Frame into world space
        if (swingSpan1 >= 0.05f) {
            rbAFrame.basis.getColumn(1, b1Axis2)
            rigidBodyA.getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis2)
            //			swing1 = ScalarUtil.atan2Fast(b2Axis1.dot(b1Axis2), b2Axis1.dot(b1Axis1));
            swx = b2Axis1.dot(b1Axis1)
            swy = b2Axis1.dot(b1Axis2)
            swing1 = ScalarUtil.atan2Fast(swy, swx)
            factor = (swy * swy + swx * swx) * threshold * threshold
            factor /= (factor + 1f)
            swing1 *= factor
        }

        if (swingSpan2 >= 0.05f) {
            rbAFrame.basis.getColumn(2, b1Axis3)
            rigidBodyA.getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis3)
            swx = b2Axis1.dot(b1Axis1)
            swy = b2Axis1.dot(b1Axis3)
            swing2 = ScalarUtil.atan2Fast(swy, swx)
            factor = (swy * swy + swx * swx) * threshold * threshold
            factor /= (factor + 1f)
            swing2 *= factor
        }

        val rMaxAngle1Sq = 1f / (swingSpan1 * swingSpan1)
        val rMaxAngle2Sq = 1f / (swingSpan2 * swingSpan2)
        val ellipseAngle = abs(swing1 * swing1) * rMaxAngle1Sq + abs(swing2 * swing2) * rMaxAngle2Sq

        if (ellipseAngle > 1f) {
            swingCorrection = ellipseAngle - 1f
            solveSwingLimit = true

            // Calculate necessary axis & factors
            b1Axis2.mul(b2Axis1.dot(b1Axis2), tmp1)
            b1Axis3.mul(b2Axis1.dot(b1Axis3), tmp2)
            tmp1.add(tmp2, tmp)
            b2Axis1.cross(tmp, swingAxis)
            swingAxis.normalize()

            val swingAxisSign = if (b2Axis1.dot(b1Axis1) >= 0.0) 1f else -1f
            swingAxis.mul(swingAxisSign)

            kSwing = 1f / (rigidBodyA.computeAngularImpulseDenominator(swingAxis) +
                    rigidBodyB.computeAngularImpulseDenominator(swingAxis))
        }

        // Twist limits
        if (twistSpan >= 0.0) {
            rbBFrame.basis.getColumn(1, b2Axis2)
            rigidBodyB.getCenterOfMassTransform(tmpTrans).basis.transform(b2Axis2)

            val rotationArc = Stack.newQuat().rotationTo(b2Axis1, b1Axis1)
            val twistRef = rotationArc.transform(b2Axis2, Stack.newVec3f())
            val twist = ScalarUtil.atan2Fast(twistRef.dot(b1Axis3), twistRef.dot(b1Axis2))
            Stack.subQuat(1) // rotationArc
            Stack.subVec3f(1) // twistRef

            val lockedFreeFactor = if (twistSpan > 0.05f) limitSoftness else 0f
            if (twist <= -twistSpan * lockedFreeFactor) {
                twistCorrection = -(twist + twistSpan)
                solveTwistLimit = true

                b2Axis1.add(b1Axis1, twistAxis)
                twistAxis.mul(-0.5f).normalize()

                kTwist = 1f / (rigidBodyA.computeAngularImpulseDenominator(twistAxis) +
                        rigidBodyB.computeAngularImpulseDenominator(twistAxis))
            } else if (twist > twistSpan * lockedFreeFactor) {
                twistCorrection = (twist - twistSpan)
                solveTwistLimit = true

                b2Axis1.add(b1Axis1, twistAxis)
                twistAxis.mul(0.5f)
                twistAxis.normalize()

                kTwist = 1f / (rigidBodyA.computeAngularImpulseDenominator(twistAxis) +
                        rigidBodyB.computeAngularImpulseDenominator(twistAxis))
            }
        }
    }

    override fun solveConstraint(timeStep: Float) {

        val pivotAInW = Stack.newVec3d()
        val pivotBInW = Stack.newVec3d()

        rigidBodyA.worldTransform.transformPosition(rbAFrame.origin, pivotAInW)
        rigidBodyB.worldTransform.transformPosition(rbBFrame.origin, pivotBInW)

        val tau = 0.3f

        val tmp1 = Stack.newVec3f()
        val tmp2 = Stack.newVec3f()

        // linear part
        if (!angularOnly) {
            val relPos1 = Stack.newVec3f()
            pivotAInW.sub(rigidBodyA.worldTransform.origin, relPos1)

            val relPos2 = Stack.newVec3f()
            pivotBInW.sub(rigidBodyB.worldTransform.origin, relPos2)

            val vel1 = rigidBodyA.getVelocityInLocalPoint(relPos1, Stack.newVec3f())
            val vel2 = rigidBodyB.getVelocityInLocalPoint(relPos2, Stack.newVec3f())
            val relVel = Stack.newVec3f()
            vel1.sub(vel2, relVel)

            val impulseVector = Stack.newVec3f()
            for (i in 0..2) {
                val normal = jacNormals[i]
                val jacDiagABInv = jacInvDiagonals[i]

                val relVel = normal.dot(relVel)
                // positional error (zeroth order error)
                pivotAInW.sub(pivotBInW, tmp1)
                val depth = -tmp1.dot(normal).toFloat() // this is the error projected on the normal
                val impulse = depth * tau / timeStep * jacDiagABInv - relVel * jacDiagABInv
                if (impulse > breakingImpulseThreshold) {
                    isBroken = true
                    break
                }

                appliedImpulse += impulse
                normal.mul(impulse, impulseVector)

                pivotAInW.sub(rigidBodyA.worldTransform.origin, tmp1)
                rigidBodyA.applyImpulse(impulseVector, tmp1)

                impulseVector.negate(tmp1)
                pivotBInW.sub(rigidBodyB.worldTransform.origin, tmp2)
                rigidBodyB.applyImpulse(tmp1, tmp2)
            }
            Stack.subVec3f(6)
        }

        run {
            // solve angular part
            val angVelA = rigidBodyA.angularVelocity
            val angVelB = rigidBodyB.angularVelocity
            val impulse = Stack.newVec3f()

            // solve swing limit
            if (solveSwingLimit) {
                angVelB.sub(angVelA, tmp1)
                val amplitude =
                    (tmp1.dot(swingAxis) * sq(relaxationFactor) + swingCorrection * (1f / timeStep) * biasFactor)
                var impulseMag = amplitude * kSwing

                // Clamp the accumulated impulse
                val temp = accSwingLimitImpulse
                accSwingLimitImpulse = max(accSwingLimitImpulse + impulseMag, 0f)
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
                    (tmp1.dot(twistAxis) * relaxationFactor * relaxationFactor + twistCorrection * (1f / timeStep) * biasFactor)
                var impulseMag = amplitude * kTwist

                // Clamp the accumulated impulse
                val temp = accTwistLimitImpulse
                accTwistLimitImpulse = max(accTwistLimitImpulse + impulseMag, 0f)
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
            Stack.subVec3f(1)
        }

        Stack.subVec3f(2)
        Stack.subVec3d(2)
    }
}
