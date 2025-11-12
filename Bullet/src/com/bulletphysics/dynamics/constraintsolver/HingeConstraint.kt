/* Hinge Constraint by Dirk Gregorius. Limits added by Marcus Hennix at Starbreeze Studios */
package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.ScalarUtil.atan2Fast
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f
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
    private val linearJacobianInvDiagonals = FloatArray(3)
    private val linearJacobianJointAxes = Array(3) { Vector3f() }

    /**
     * constraint axii. Assumes z is hinge axis.
     */
    private val rbAFrame = Transform()
    private val rbBFrame = Transform()

    var motorTargetVelocity = 0f
    var maxMotorImpulse = 0f

    var limitSoftness = 0f
    var biasFactor = 0f
    var relaxationFactor = 0f

    var lowerLimit = 0f
    var upperLimit = 0f

    private var kHinge = 0f
    private var limitSign = 0f
    private var correction = 0f
    private var accLimitImpulse = 0f

    var angularOnly: Boolean = false
    var enableAngularMotor: Boolean

    private var solveLimit: Boolean = false

    constructor(
        rbA: RigidBody,
        rbB: RigidBody,
        pivotInA: Vector3d,
        pivotInB: Vector3d,
        axisInA: Vector3f,
        axisInB: Vector3f
    ) : super(rbA, rbB) {
        angularOnly = false
        enableAngularMotor = false

        rbAFrame.setTranslation(pivotInA)

        // since no frame is given, assume this to be zero angle and just pick rb transform axis
        val rbAxisA1 = Stack.newVec3f()
        val rbAxisA2 = Stack.newVec3f()

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
            axisInA.cross(rbAxisA1, rbAxisA2)
            rbAxisA2.cross(axisInA, rbAxisA1)
        }

        rbAFrame.basis.setRow(0, rbAxisA1.x, rbAxisA2.x, axisInA.x)
        rbAFrame.basis.setRow(1, rbAxisA1.y, rbAxisA2.y, axisInA.y)
        rbAFrame.basis.setRow(2, rbAxisA1.z, rbAxisA2.z, axisInA.z)

        val rotationArc = Stack.newQuat().rotationTo(axisInA, axisInB)
        val rbAxisB1 = rotationArc.transform(rbAxisA1, Stack.newVec3f())
        val rbAxisB2 = Stack.newVec3f()
        axisInB.cross(rbAxisB1, rbAxisB2)

        rbBFrame.setTranslation(pivotInB)
        rbBFrame.basis.setRow(0, rbAxisB1.x, rbAxisB2.x, -axisInB.x)
        rbBFrame.basis.setRow(1, rbAxisB1.y, rbAxisB2.y, -axisInB.y)
        rbBFrame.basis.setRow(2, rbAxisB1.z, rbAxisB2.z, -axisInB.z)

        // start with free
        lowerLimit = 1e38f
        upperLimit = -1e38f
        biasFactor = 0.3f
        relaxationFactor = 1f
        limitSoftness = 0.9f
        solveLimit = false

        Stack.subVec3f(4)
        Stack.subQuat(1)
        Stack.subTrans(1)
    }

    constructor(rbA: RigidBody, rbB: RigidBody, rbAFrame: Transform, rbBFrame: Transform) : super(rbA, rbB) {
        this.rbAFrame.set(rbAFrame)
        this.rbBFrame.set(rbBFrame)
        angularOnly = false
        enableAngularMotor = false

        // flip axis
        val rbB = rbBFrame.basis
        rbB.m20 = -rbB.m20
        rbB.m21 = -rbB.m21
        rbB.m22 = -rbB.m22

        // start with free
        lowerLimit = 1e38f
        upperLimit = -1e38f
        biasFactor = 0.3f
        relaxationFactor = 1f
        limitSoftness = 0.9f
        solveLimit = false
    }

    override fun buildJacobian() {
        val tmp = Stack.newVec3f()
        val tmp1 = Stack.newVec3f()
        val tmp2 = Stack.newVec3f()

        val centerOfMassA = rigidBodyA.getCenterOfMassTransform(Stack.newTrans())
        val centerOfMassB = rigidBodyB.getCenterOfMassTransform(Stack.newTrans())

        appliedImpulse = 0f

        if (!angularOnly) {
            val pivotAInW = Stack.newVec3d(rbAFrame.origin)
            centerOfMassA.transformPosition(pivotAInW)

            val pivotBInW = Stack.newVec3d(rbBFrame.origin)
            centerOfMassB.transformPosition(pivotBInW)

            val relPos = Stack.newVec3d()
            pivotBInW.sub(pivotAInW, relPos)

            val normal /*[3]*/ = arrayOf(Stack.newVec3f(), Stack.newVec3f(), Stack.newVec3f())
            if (relPos.lengthSquared() > BulletGlobals.FLT_EPSILON) {
                normal[0].set(relPos)
                normal[0].normalize()
            } else {
                normal[0].set(1f, 0f, 0f)
            }

            normal[0].findSystem(normal[1], normal[2], false)

            for (i in 0..2) {
                pivotAInW.sub(rigidBodyA.worldTransform.origin, tmp1)
                pivotBInW.sub(rigidBodyB.worldTransform.origin, tmp2)

                linearJacobianJointAxes[i].set(normal[i])
                linearJacobianInvDiagonals[i] = JacobianEntry.calculateDiagonalInv(
                    centerOfMassA.basis, centerOfMassB.basis, tmp1, tmp2, normal[i],
                    rigidBodyA.invInertiaLocal, rigidBodyA.inverseMass,
                    rigidBodyB.invInertiaLocal, rigidBodyB.inverseMass
                )
            }
            Stack.subVec3d(4)
        }

        // calculate two perpendicular jointAxis, orthogonal to hingeAxis
        // these two jointAxis require equal angular velocities for both bodies

        // this is unused for now, it's a todo
        val jointAxis0local = Stack.newVec3f()
        val jointAxis1local = Stack.newVec3f()

        rbAFrame.basis.getColumn(2, tmp)
        tmp.findSystem(jointAxis0local, jointAxis1local, false)

        // TODO: check this
        //rigidBodyA().getCenterOfMassTransform().getBasis() * m_rbAFrame.getBasis().getColumn(2);
        val jointAxis0 = Stack.newVec3f(jointAxis0local)
        centerOfMassA.basis.transform(jointAxis0)

        val jointAxis1 = Stack.newVec3f(jointAxis1local)
        centerOfMassA.basis.transform(jointAxis1)

        val hingeAxisWorld = Stack.newVec3f()
        rbAFrame.basis.getColumn(2, hingeAxisWorld)
        centerOfMassA.basis.transform(hingeAxisWorld)

        // Compute limit information
        val hingeAngle = this.hingeAngle

        //set bias, sign, clear accumulator
        correction = 0f
        limitSign = 0f
        solveLimit = false
        accLimitImpulse = 0f

        if (lowerLimit < upperLimit) {
            if (hingeAngle <= lowerLimit * limitSoftness) {
                correction = (lowerLimit - hingeAngle)
                limitSign = 1f
                solveLimit = true
            } else if (hingeAngle >= upperLimit * limitSoftness) {
                correction = upperLimit - hingeAngle
                limitSign = -1f
                solveLimit = true
            }
        }

        // Compute K = J*W*J' for hinge axis
        val axisA = Stack.newVec3f()
        rbAFrame.basis.getColumn(2, axisA)
        centerOfMassA.basis.transform(axisA)

        kHinge = 1f / (rigidBodyA.computeAngularImpulseDenominator(axisA) +
                rigidBodyB.computeAngularImpulseDenominator(axisA))

        Stack.subVec3f(9)
        Stack.subTrans(2)
    }

    override fun solveConstraint(timeStep: Float) {
        val tmp1 = Stack.newVec3f()
        val tmp2 = Stack.newVec3f()

        val centerOfMassA = rigidBodyA.getCenterOfMassTransform(Stack.newTrans())
        val centerOfMassB = rigidBodyB.getCenterOfMassTransform(Stack.newTrans())

        val pivotAInW = Stack.newVec3d(rbAFrame.origin)
        centerOfMassA.transformPosition(pivotAInW)

        val pivotBInW = Stack.newVec3d(rbBFrame.origin)
        centerOfMassB.transformPosition(pivotBInW)

        val tau = 0.3f

        // linear part
        if (!angularOnly) {
            val relPos1 = Stack.newVec3f()
            pivotAInW.sub(rigidBodyA.worldTransform.origin, relPos1)

            val relPos2 = Stack.newVec3f()
            pivotBInW.sub(rigidBodyB.worldTransform.origin, relPos2)

            val vel1 = rigidBodyA.getVelocityInLocalPoint(relPos1, Stack.newVec3f())
            val vel2 = rigidBodyB.getVelocityInLocalPoint(relPos2, Stack.newVec3f())
            val vel = Stack.newVec3f()
            vel1.sub(vel2, vel)

            val impulseVector = Stack.newVec3f()
            for (i in 0..2) {
                val normal = linearJacobianJointAxes[i]
                val jacDiagABInv = linearJacobianInvDiagonals[i]
                val relVel = normal.dot(vel)
                // positional error (zeroth order error)
                pivotAInW.sub(pivotBInW, tmp1)
                val depth = -tmp1.dot(normal).toFloat() // this is the error projected on the normal
                val impulse = (depth * tau / timeStep - relVel) * jacDiagABInv
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
            // get axes in world space
            val axisA = Stack.newVec3f()
            rbAFrame.basis.getColumn(2, axisA)
            centerOfMassA.basis.transform(axisA)

            val axisB = Stack.newVec3f()
            rbBFrame.basis.getColumn(2, axisB)
            centerOfMassB.basis.transform(axisB)

            val angVelA = rigidBodyA.angularVelocity
            val angVelB = rigidBodyB.angularVelocity

            val angVelAroundHingeAxisA = Stack.newVec3f()
            axisA.mul(axisA.dot(angVelA), angVelAroundHingeAxisA)

            val angVelAroundHingeAxisB = Stack.newVec3f()
            axisB.mul(axisB.dot(angVelB), angVelAroundHingeAxisB)

            val angleAOrthogonal = Stack.newVec3f()
            angVelA.sub(angVelAroundHingeAxisA, angleAOrthogonal)

            val angleBOrthogonal = Stack.newVec3f()
            angVelB.sub(angVelAroundHingeAxisB, angleBOrthogonal)

            val velRelOrthogonal = Stack.newVec3f()
            angleAOrthogonal.sub(angleBOrthogonal, velRelOrthogonal)

            run {
                // solve orthogonal angular velocity correction
                val relaxation = 1f
                val len = velRelOrthogonal.length()
                if (len > 0.00001f) {
                    val normal = Stack.newVec3f()
                    velRelOrthogonal.normalize(normal)
                    val denominator = rigidBodyA.computeAngularImpulseDenominator(normal) +
                            rigidBodyB.computeAngularImpulseDenominator(normal)
                    // scale for mass and relaxation
                    velRelOrthogonal.mul(relaxationFactor / denominator)
                    Stack.subVec3d(1)
                }

                // solve angular positional correction
                // TODO: check
                //Vector3d angularError = -axisA.cross(axisB) *(btScalar(1.)/timeStep);
                val angularError = Stack.newVec3f()
                axisA.cross(axisB, angularError)
                angularError.negate()
                angularError.mul(1f / timeStep)
                val len2 = angularError.length()
                if (len2 > 0.00001f) {
                    val normal2 = Stack.newVec3f()
                    angularError.normalize(normal2)
                    val denominator = rigidBodyA.computeAngularImpulseDenominator(normal2) +
                            rigidBodyB.computeAngularImpulseDenominator(normal2)
                    angularError.mul(relaxation / denominator)
                    Stack.subVec3d(1)
                }

                angularError.sub(velRelOrthogonal, tmp1)
                rigidBodyA.applyTorqueImpulse(tmp1)
                tmp1.negate()
                rigidBodyB.applyTorqueImpulse(tmp1)

                // solve limit
                if (solveLimit) {
                    angVelB.sub(angVelA, tmp1)
                    val amplitude =
                        ((tmp1).dot(axisA) * relaxationFactor + correction * (1f / timeStep) * biasFactor) * limitSign

                    var impulseMag = amplitude * kHinge
                    if (abs(impulseMag) > breakingImpulseThreshold) {
                        isBroken = true
                    } else {
                        // Clamp the accumulated impulse
                        val temp = accLimitImpulse
                        accLimitImpulse = max(accLimitImpulse + impulseMag, 0f)
                        impulseMag = accLimitImpulse - temp

                        val impulse = Stack.newVec3f()
                        axisA.mul(impulseMag * limitSign, impulse)

                        rigidBodyA.applyTorqueImpulse(impulse)
                        impulse.negate(tmp1)
                        rigidBodyB.applyTorqueImpulse(tmp1)
                        Stack.subVec3f(1) // impulse
                    }
                }

                Stack.subVec3f(1) // angularError
            }

            // apply motor
            if (enableAngularMotor) {
                // todo: add limits too
                val angularLimit = Stack.newVec3f().set(0.0)
                val velRel = Stack.newVec3f()
                angVelAroundHingeAxisA.sub(angVelAroundHingeAxisB, velRel)
                val projRelVel = velRel.dot(axisA)

                val desiredMotorVel = motorTargetVelocity
                val motorRelVel = desiredMotorVel - projRelVel

                val unclippedMotorImpulse = kHinge * motorRelVel
                if (unclippedMotorImpulse > breakingImpulseThreshold) {
                    isBroken = true
                    Stack.subVec3f(2) // angularLimit, velrel
                } else {
                    // todo: should clip against accumulated impulse
                    var clippedMotorImpulse = min(unclippedMotorImpulse, maxMotorImpulse)
                    clippedMotorImpulse = max(clippedMotorImpulse, -maxMotorImpulse)
                    val motorImp = Stack.newVec3f()
                    axisA.mul(clippedMotorImpulse, motorImp)

                    motorImp.add(angularLimit, tmp1)
                    rigidBodyA.applyTorqueImpulse(tmp1)

                    motorImp.negate(tmp1)
                    tmp1.sub(angularLimit)
                    rigidBodyB.applyTorqueImpulse(tmp1)
                    Stack.subVec3f(3) // angularLimit, velrel, motorImp
                }
            }
            Stack.subVec3f(7)
        }

        Stack.subVec3f(2)
        Stack.subVec3d(2)
        Stack.subTrans(2)
    }

    val hingeAngle: Float
        get() {
            val centerOfMassA = rigidBodyA.worldTransform
            val centerOfMassB = rigidBodyB.worldTransform

            val refAxis0 = Stack.newVec3f()
            rbAFrame.basis.getColumn(0, refAxis0)
            centerOfMassA.basis.transform(refAxis0)

            val refAxis1 = Stack.newVec3f()
            rbAFrame.basis.getColumn(1, refAxis1)
            centerOfMassA.basis.transform(refAxis1)

            val swingAxis = Stack.newVec3f()
            rbBFrame.basis.getColumn(1, swingAxis)
            centerOfMassB.basis.transform(swingAxis)

            val hingeAngle = atan2Fast(swingAxis.dot(refAxis0), swingAxis.dot(refAxis1))
            Stack.subVec3f(3)
            return hingeAngle
        }
}

