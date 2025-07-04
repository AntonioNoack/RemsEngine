/* Hinge Constraint by Dirk Gregorius. Limits added by Marcus Hennix at Starbreeze Studios */
package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.QuaternionUtil.quatRotate
import com.bulletphysics.linearmath.QuaternionUtil.shortestArcQuat
import com.bulletphysics.linearmath.ScalarUtil.atan2Fast
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil.findOrthonormalBasis
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setNormalize
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
import com.bulletphysics.util.setTranspose
import cz.advel.stack.Stack
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Hinge constraint between two rigid bodies each with a pivot point that descibes
 * the axis location in local space. Axis defines the orientation of the hinge axis.
 *
 * @author jezek2
 */
class HingeConstraint : TypedConstraint {

    /**
     * 3 orthogonal linear constraints
     */
    private val linearJacobianInvDiagonals = DoubleArray(3)
    private val linearJacobianJointAxes = Array(3) { Vector3d() }

    /**
     * constraint axii. Assumes z is hinge axis.
     */
    private val rbAFrame = Transform()
    private val rbBFrame = Transform()

    var motorTargetVelocity: Double = 0.0
    var maxMotorImpulse: Double = 0.0

    var limitSoftness = 0.0
    var biasFactor = 0.0
    var relaxationFactor = 0.0

    var lowerLimit: Double = 0.0
    var upperLimit: Double = 0.0

    private var kHinge = 0.0
    private var limitSign: Double = 0.0
    private var correction = 0.0
    private var accLimitImpulse = 0.0

    var angularOnly: Boolean = false
    var enableAngularMotor: Boolean

    private var solveLimit: Boolean = false

    constructor(
        rbA: RigidBody,
        rbB: RigidBody,
        pivotInA: Vector3d,
        pivotInB: Vector3d,
        axisInA: Vector3d,
        axisInB: Vector3d
    ) : super(rbA, rbB) {
        angularOnly = false
        enableAngularMotor = false

        rbAFrame.setTranslation(pivotInA)

        // since no frame is given, assume this to be zero angle and just pick rb transform axis
        val rbAxisA1 = Stack.newVec()
        val rbAxisA2 = Stack.newVec()

        val centerOfMassA = rbA.getCenterOfMassTransform(Stack.newTrans())
        centerOfMassA.basis.getColumn(0, rbAxisA1)
        val projection = axisInA.dot(rbAxisA1)

        if (projection >= 1.0 - BulletGlobals.SIMD_EPSILON) {
            centerOfMassA.basis.getColumn(2, rbAxisA1)
            rbAxisA1.negate()
            centerOfMassA.basis.getColumn(1, rbAxisA2)
        } else if (projection <= -1.0 + BulletGlobals.SIMD_EPSILON) {
            centerOfMassA.basis.getColumn(2, rbAxisA1)
            centerOfMassA.basis.getColumn(1, rbAxisA2)
        } else {
            rbAxisA2.setCross(axisInA, rbAxisA1)
            rbAxisA1.setCross(rbAxisA2, axisInA)
        }

        rbAFrame.basis.setRow(0, rbAxisA1.x, rbAxisA2.x, axisInA.x)
        rbAFrame.basis.setRow(1, rbAxisA1.y, rbAxisA2.y, axisInA.y)
        rbAFrame.basis.setRow(2, rbAxisA1.z, rbAxisA2.z, axisInA.z)

        val rotationArc = shortestArcQuat(axisInA, axisInB, Stack.newQuat())
        val rbAxisB1 = quatRotate(rotationArc, rbAxisA1, Stack.newVec())
        val rbAxisB2 = Stack.newVec()
        rbAxisB2.setCross(axisInB, rbAxisB1)

        rbBFrame.setTranslation(pivotInB)
        rbBFrame.basis.setRow(0, rbAxisB1.x, rbAxisB2.x, -axisInB.x)
        rbBFrame.basis.setRow(1, rbAxisB1.y, rbAxisB2.y, -axisInB.y)
        rbBFrame.basis.setRow(2, rbAxisB1.z, rbAxisB2.z, -axisInB.z)

        // start with free
        lowerLimit = 1e308
        upperLimit = -1e308
        biasFactor = 0.3
        relaxationFactor = 1.0
        limitSoftness = 0.9
        solveLimit = false
    }

    constructor(rbA: RigidBody, pivotInA: Vector3d, axisInA: Vector3d) : super(rbA) {
        angularOnly = false
        enableAngularMotor = false

        // since no frame is given, assume this to be zero angle and just pick rb transform axis
        // fixed axis in worldspace
        val rbAxisA1 = Stack.newVec()
        val centerOfMassA = rbA.getCenterOfMassTransform(Stack.newTrans())
        centerOfMassA.basis.getColumn(0, rbAxisA1)

        val projection = rbAxisA1.dot(axisInA)
        if (projection > BulletGlobals.FLT_EPSILON) {
            rbAxisA1.mul(projection)
            rbAxisA1.sub(axisInA)
        } else {
            centerOfMassA.basis.getColumn(1, rbAxisA1)
        }

        val rbAxisA2 = Stack.newVec()
        rbAxisA2.setCross(axisInA, rbAxisA1)

        rbAFrame.setTranslation(pivotInA)
        rbAFrame.basis.setRow(0, rbAxisA1.x, rbAxisA2.x, axisInA.x)
        rbAFrame.basis.setRow(1, rbAxisA1.y, rbAxisA2.y, axisInA.y)
        rbAFrame.basis.setRow(2, rbAxisA1.z, rbAxisA2.z, axisInA.z)

        val axisInB = Stack.newVec()
        axisInB.setNegate(axisInA)
        centerOfMassA.basis.transform(axisInB)

        val rotationArc = shortestArcQuat(axisInA, axisInB, Stack.newQuat())
        val rbAxisB1 = quatRotate(rotationArc, rbAxisA1, Stack.newVec())
        val rbAxisB2 = Stack.newVec()
        rbAxisB2.setCross(axisInB, rbAxisB1)

        rbBFrame.setTranslation(pivotInA)
        centerOfMassA.transform(rbBFrame.origin)
        rbBFrame.basis.setRow(0, rbAxisB1.x, rbAxisB2.x, axisInB.x)
        rbBFrame.basis.setRow(1, rbAxisB1.y, rbAxisB2.y, axisInB.y)
        rbBFrame.basis.setRow(2, rbAxisB1.z, rbAxisB2.z, axisInB.z)

        // start with free
        lowerLimit = 1e308
        upperLimit = -1e308
        biasFactor = 0.3
        relaxationFactor = 1.0
        limitSoftness = 0.9
        solveLimit = false
    }

    constructor(rbA: RigidBody, rbB: RigidBody, rbAFrame: Transform, rbBFrame: Transform) : super(rbA, rbB) {
        this.rbAFrame.set(rbAFrame)
        this.rbBFrame.set(rbBFrame)
        angularOnly = false
        enableAngularMotor = false

        // flip axis
        this.rbBFrame.basis.m20 *= -1.0
        this.rbBFrame.basis.m21 *= -1.0
        this.rbBFrame.basis.m22 *= -1.0

        // start with free
        lowerLimit = 1e308
        upperLimit = -1e308
        biasFactor = 0.3
        relaxationFactor = 1.0
        limitSoftness = 0.9
        solveLimit = false
    }

    constructor(rbA: RigidBody, rbAFrame: Transform) : super(rbA) {
        this.rbAFrame.set(rbAFrame)
        this.rbBFrame.set(rbAFrame)
        angularOnly = false
        enableAngularMotor = false

        // not providing rigidbody B means implicitly using worldspace for body B

        // flip axis
        this.rbBFrame.basis.m20 *= -1.0
        this.rbBFrame.basis.m21 *= -1.0
        this.rbBFrame.basis.m22 *= -1.0

        this.rbBFrame.setTranslation(this.rbAFrame.origin)
        rbA.getCenterOfMassTransform(Stack.newTrans()).transform(this.rbBFrame.origin)

        // start with free
        lowerLimit = 1e308
        upperLimit = -1e308
        biasFactor = 0.3
        relaxationFactor = 1.0
        limitSoftness = 0.9
        solveLimit = false
    }

    override fun buildJacobian() {
        val tmp = Stack.newVec()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()
        val tmpVec = Stack.newVec()

        val centerOfMassA = rigidBodyA.getCenterOfMassTransform(Stack.newTrans())
        val centerOfMassB = rigidBodyB.getCenterOfMassTransform(Stack.newTrans())

        appliedImpulse = 0.0

        if (!angularOnly) {
            val pivotAInW = Stack.newVec(rbAFrame.origin)
            centerOfMassA.transform(pivotAInW)

            val pivotBInW = Stack.newVec(rbBFrame.origin)
            centerOfMassB.transform(pivotBInW)

            val relPos = Stack.newVec()
            relPos.setSub(pivotBInW, pivotAInW)

            val normal /*[3]*/ = arrayOf<Vector3d>(Stack.newVec(), Stack.newVec(), Stack.newVec())
            if (relPos.lengthSquared() > BulletGlobals.FLT_EPSILON) {
                normal[0].set(relPos)
                normal[0].normalize()
            } else {
                normal[0].set(1.0, 0.0, 0.0)
            }

            findOrthonormalBasis(normal[0], normal[1], normal[2])

            for (i in 0..2) {
                tmp1.setSub(pivotAInW, rigidBodyA.getCenterOfMassPosition(tmpVec))
                tmp2.setSub(pivotBInW, rigidBodyB.getCenterOfMassPosition(tmpVec))

                linearJacobianJointAxes[i].set(normal[i])
                linearJacobianInvDiagonals[i] = JacobianEntry.calculateDiagonalInv(
                    centerOfMassA.basis, centerOfMassB.basis, tmp1, tmp2, normal[i],
                    rigidBodyA.invInertiaLocal, rigidBodyA.inverseMass,
                    rigidBodyB.invInertiaLocal, rigidBodyB.inverseMass
                )
            }
            Stack.subVec(4)
        }

        // calculate two perpendicular jointAxis, orthogonal to hingeAxis
        // these two jointAxis require equal angular velocities for both bodies

        // this is unused for now, it's a todo
        val jointAxis0local = Stack.newVec()
        val jointAxis1local = Stack.newVec()

        rbAFrame.basis.getColumn(2, tmp)
        findOrthonormalBasis(tmp, jointAxis0local, jointAxis1local)

        // TODO: check this
        //rigidBodyA().getCenterOfMassTransform().getBasis() * m_rbAFrame.getBasis().getColumn(2);
        val jointAxis0 = Stack.newVec(jointAxis0local)
        centerOfMassA.basis.transform(jointAxis0)

        val jointAxis1 = Stack.newVec(jointAxis1local)
        centerOfMassA.basis.transform(jointAxis1)

        val hingeAxisWorld = Stack.newVec()
        rbAFrame.basis.getColumn(2, hingeAxisWorld)
        centerOfMassA.basis.transform(hingeAxisWorld)

        // Compute limit information
        val hingeAngle = this.hingeAngle

        //set bias, sign, clear accumulator
        correction = 0.0
        limitSign = 0.0
        solveLimit = false
        accLimitImpulse = 0.0

        if (lowerLimit < upperLimit) {
            if (hingeAngle <= lowerLimit * limitSoftness) {
                correction = (lowerLimit - hingeAngle)
                limitSign = 1.0
                solveLimit = true
            } else if (hingeAngle >= upperLimit * limitSoftness) {
                correction = upperLimit - hingeAngle
                limitSign = -1.0
                solveLimit = true
            }
        }

        // Compute K = J*W*J' for hinge axis
        val axisA = Stack.newVec()
        rbAFrame.basis.getColumn(2, axisA)
        centerOfMassA.basis.transform(axisA)

        kHinge = 1.0 / (rigidBodyA.computeAngularImpulseDenominator(axisA) +
                rigidBodyB.computeAngularImpulseDenominator(axisA))

        Stack.subVec(10)
        Stack.subTrans(2)
    }

    override fun solveConstraint(timeStep: Double) {
        val tmp = Stack.newVec()
        val tmp2 = Stack.newVec()
        val tmpVec = Stack.newVec()

        val centerOfMassA = rigidBodyA.getCenterOfMassTransform(Stack.newTrans())
        val centerOfMassB = rigidBodyB.getCenterOfMassTransform(Stack.newTrans())

        val pivotAInW = Stack.newVec(rbAFrame.origin)
        centerOfMassA.transform(pivotAInW)

        val pivotBInW = Stack.newVec(rbBFrame.origin)
        centerOfMassB.transform(pivotBInW)

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
                val normal = linearJacobianJointAxes[i]
                val jacDiagABInv = linearJacobianInvDiagonals[i]
                val relVel = normal.dot(vel)
                // positional error (zeroth order error)
                tmp.setSub(pivotAInW, pivotBInW)
                val depth = -tmp.dot(normal) // this is the error projected on the normal
                val impulse = (depth * tau / timeStep - relVel) * jacDiagABInv
                if (impulse > breakingImpulseThreshold) {
                    isBroken = true
                    break
                }

                appliedImpulse += impulse
                impulseVector.setScale(impulse, normal)

                tmp.setSub(pivotAInW, rigidBodyA.worldTransform.origin)
                rigidBodyA.applyImpulse(impulseVector, tmp)

                tmp.setNegate(impulseVector)
                tmp2.setSub(pivotBInW, rigidBodyB.worldTransform.origin)
                rigidBodyB.applyImpulse(tmp, tmp2)
            }
            Stack.subVec(6)
        }

        run {
            // solve angular part
            // get axes in world space
            val axisA = Stack.newVec()
            rbAFrame.basis.getColumn(2, axisA)
            centerOfMassA.basis.transform(axisA)

            val axisB = Stack.newVec()
            rbBFrame.basis.getColumn(2, axisB)
            centerOfMassB.basis.transform(axisB)

            val angVelA = rigidBodyA.angularVelocity
            val angVelB = rigidBodyB.angularVelocity

            val angVelAroundHingeAxisA = Stack.newVec()
            angVelAroundHingeAxisA.setScale(axisA.dot(angVelA), axisA)

            val angVelAroundHingeAxisB = Stack.newVec()
            angVelAroundHingeAxisB.setScale(axisB.dot(angVelB), axisB)

            val angleAOrthogonal = Stack.newVec()
            angleAOrthogonal.setSub(angVelA, angVelAroundHingeAxisA)

            val angleBOrthogonal = Stack.newVec()
            angleBOrthogonal.setSub(angVelB, angVelAroundHingeAxisB)

            val velRelOrthogonal = Stack.newVec()
            velRelOrthogonal.setSub(angleAOrthogonal, angleBOrthogonal)

            run {
                // solve orthogonal angular velocity correction
                val relaxation = 1.0
                val len = velRelOrthogonal.length()
                if (len > 0.00001f) {
                    val normal = Stack.newVec()
                    normal.setNormalize(velRelOrthogonal)

                    val denominator = rigidBodyA.computeAngularImpulseDenominator(normal) +
                            rigidBodyB.computeAngularImpulseDenominator(normal)
                    // scale for mass and relaxation
                    velRelOrthogonal.mul((1.0 / denominator) * relaxationFactor)
                    Stack.subVec(1)
                }

                // solve angular positional correction
                // TODO: check
                //Vector3d angularError = -axisA.cross(axisB) *(btScalar(1.)/timeStep);
                val angularError = Stack.newVec()
                angularError.setCross(axisA, axisB)
                angularError.negate()
                angularError.mul(1.0 / timeStep)
                val len2 = angularError.length()
                if (len2 > 0.00001) {
                    val normal2 = Stack.newVec()
                    normal2.setNormalize(angularError)

                    val denominator = rigidBodyA.computeAngularImpulseDenominator(normal2) +
                            rigidBodyB.computeAngularImpulseDenominator(normal2)
                    angularError.mul((1.0 / denominator) * relaxation)
                }

                tmp.setNegate(velRelOrthogonal)
                tmp.add(angularError)
                rigidBodyA.applyTorqueImpulse(tmp)

                tmp.setSub(velRelOrthogonal, angularError)
                rigidBodyB.applyTorqueImpulse(tmp)

                // solve limit
                if (solveLimit) {
                    tmp.setSub(angVelB, angVelA)
                    val amplitude =
                        ((tmp).dot(axisA) * relaxationFactor + correction * (1.0 / timeStep) * biasFactor) * limitSign

                    var impulseMag = amplitude * kHinge
                    if (abs(impulseMag) > breakingImpulseThreshold) {
                        isBroken = true
                    } else {
                        // Clamp the accumulated impulse
                        val temp = accLimitImpulse
                        accLimitImpulse = max(accLimitImpulse + impulseMag, 0.0)
                        impulseMag = accLimitImpulse - temp

                        val impulse = Stack.newVec()
                        impulse.setScale(impulseMag * limitSign, axisA)

                        rigidBodyA.applyTorqueImpulse(impulse)

                        tmp.setNegate(impulse)
                        rigidBodyB.applyTorqueImpulse(tmp)
                        Stack.subVec(1) // impulse
                    }
                }
            }

            // apply motor
            if (enableAngularMotor) {
                // todo: add limits too
                val angularLimit = Stack.newVec()
                angularLimit.set(0.0, 0.0, 0.0)

                val velRel = Stack.newVec()
                velRel.setSub(angVelAroundHingeAxisA, angVelAroundHingeAxisB)
                val projRelVel = velRel.dot(axisA)

                val desiredMotorVel = motorTargetVelocity
                val motorRelVel = desiredMotorVel - projRelVel

                val unclippedMotorImpulse = kHinge * motorRelVel
                if (unclippedMotorImpulse > breakingImpulseThreshold) {
                    isBroken = true
                    Stack.subVec(2) // angularLimit, velrel
                } else {
                    // todo: should clip against accumulated impulse
                    var clippedMotorImpulse = min(unclippedMotorImpulse, maxMotorImpulse)
                    clippedMotorImpulse = max(clippedMotorImpulse, -maxMotorImpulse)
                    val motorImp = Stack.newVec()
                    motorImp.setScale(clippedMotorImpulse, axisA)

                    tmp.setAdd(motorImp, angularLimit)
                    rigidBodyA.applyTorqueImpulse(tmp)

                    tmp.setNegate(motorImp)
                    tmp.sub(angularLimit)
                    rigidBodyB.applyTorqueImpulse(tmp)
                    Stack.subVec(3) // angularLimit, velrel, motorImp
                }
            }
            Stack.subVec(7)
        }

        Stack.subVec(5)
        Stack.subTrans(2)
    }

    val hingeAngle: Double
        get() {
            val centerOfMassA = rigidBodyA.worldTransform
            val centerOfMassB = rigidBodyB.worldTransform

            val refAxis0 = Stack.newVec()
            rbAFrame.basis.getColumn(0, refAxis0)
            centerOfMassA.basis.transform(refAxis0)

            val refAxis1 = Stack.newVec()
            rbAFrame.basis.getColumn(1, refAxis1)
            centerOfMassA.basis.transform(refAxis1)

            val swingAxis = Stack.newVec()
            rbBFrame.basis.getColumn(1, swingAxis)
            centerOfMassB.basis.transform(swingAxis)

            val hingeAngle = atan2Fast(swingAxis.dot(refAxis0), swingAxis.dot(refAxis1))
            Stack.subVec(3)
            return hingeAngle
        }
}

