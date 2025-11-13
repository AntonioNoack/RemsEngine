/*
Added by Roman Ponomarev (rponom@gmail.com)
April 04, 2008

TODO:
 - add clamping od accumulated impulse to improve stability
*/
package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.atan2

/**
 * JAVA NOTE: SliderConstraint from 2.71
 * @author jezek2
 */
class SliderConstraint : TypedConstraint {

    val frameOffsetA = Transform()
    val frameOffsetB = Transform()

    // use frameA fo define limits, if true
    var useLinearReferenceFrameA: Boolean

    // linear limits
    @JvmField
    var lowerLinearLimit = 0f

    @JvmField
    var upperLinearLimit = 0f

    // angular limits
    @JvmField
    var lowerAngularLimit = 0f

    @JvmField
    var upperAngularLimit = 0f

    // softness, restitution and damping for different cases
    // DirLin - moving inside linear limits
    // LimLin - hitting linear limit
    // DirAng - moving inside angular limits
    // LimAng - hitting angular limit
    // OrthoLin, OrthoAng - against constraint axis

    var softnessDirLinear = 0f
    var restitutionDirLinear = 0f
    var dampingDirLinear = 0f

    var softnessDirAngular = 0f
    var restitutionDirAngular = 0f
    var dampingDirAngular = 0f

    var softnessLimitLinear = 0f
    var restitutionLimitLinear = 0f
    var dampingLimitLinear = 0f

    var softnessLimitAngular = 0f
    var restitutionLimitAngular = 0f
    var dampingLimitAngular = 0f

    var softnessOrthogonalLinear = 0f
    var restitutionOrthogonalLinear = 0f
    var dampingOrthogonalLinear = 0f

    var softnessOrthogonalAngular = 0f
    var restitutionOrthogonalAngular = 0f
    var dampingOrthogonalAngular = 0f

    // for internal use
    private var solveLinLim = false
    private var solveAngLim = false

    private val jacLin = Array(3) { Vector3f() }
    private val jacLinDiagABInv = FloatArray(3)

    private var timeStep = 0f
    val calculatedTransformA = Transform()
    val calculatedTransformB = Transform()

    private val sliderAxis = Vector3f()
    private val realPivotAInW = Vector3d()
    private val realPivotBInW = Vector3d()
    private val projPivotInW = Vector3d()
    private val delta = Vector3f()
    private val depth = Vector3f()
    private val relPosA = Vector3f()
    private val relPosB = Vector3f()

    var linearPosition = 0f
    var angularPosition = 0f
    private var kAngle = 0f

    var poweredLinearMotor = false
    var targetLinearMotorVelocity = 0f
    var maxLinearMotorForce = 0f
    private var accumulatedLinearMotorImpulse = 0f

    var poweredAngularMotor = false
    var targetAngularMotorVelocity = 0f
    var maxAngularMotorForce = 0f
    private var accumulatedAngMotorImpulse = 0f

    constructor(
        rbA: RigidBody,
        rbB: RigidBody,
        frameOffsetA: Transform,
        frameOffsetB: Transform,
        useLinearReferenceFrameA: Boolean
    ) : super(rbA, rbB) {
        this.frameOffsetA.set(frameOffsetA)
        this.frameOffsetB.set(frameOffsetB)
        this.useLinearReferenceFrameA = useLinearReferenceFrameA
        initParams()
    }

    fun initParams() {
        lowerLinearLimit = 1f
        upperLinearLimit = -1f
        lowerAngularLimit = 0f
        upperAngularLimit = 0f

        softnessDirLinear = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionDirLinear = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingDirLinear = 0f

        softnessDirAngular = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionDirAngular = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingDirAngular = 0f

        softnessOrthogonalLinear = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionOrthogonalLinear = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingOrthogonalLinear = SLIDER_CONSTRAINT_DEF_DAMPING

        softnessOrthogonalAngular = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionOrthogonalAngular = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingOrthogonalAngular = SLIDER_CONSTRAINT_DEF_DAMPING

        softnessLimitLinear = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionLimitLinear = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingLimitLinear = SLIDER_CONSTRAINT_DEF_DAMPING

        softnessLimitAngular = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionLimitAngular = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingLimitAngular = SLIDER_CONSTRAINT_DEF_DAMPING

        poweredLinearMotor = false
        targetLinearMotorVelocity = 0f
        maxLinearMotorForce = 0f
        accumulatedLinearMotorImpulse = 0f

        poweredAngularMotor = false
        targetAngularMotorVelocity = 0f
        maxAngularMotorForce = 0f
        accumulatedAngMotorImpulse = 0f
    }

    override fun buildJacobian() {
        if (useLinearReferenceFrameA) {
            buildJacobianInt(rigidBodyA, rigidBodyB, frameOffsetA, frameOffsetB)
        } else {
            buildJacobianInt(rigidBodyB, rigidBodyA, frameOffsetB, frameOffsetA)
        }
    }

    override fun solveConstraint(timeStep: Float) {
        this.timeStep = timeStep
        if (useLinearReferenceFrameA) {
            solveConstraintInt(rigidBodyA, rigidBodyB)
        } else {
            solveConstraintInt(rigidBodyB, rigidBodyA)
        }
    }

    fun buildJacobianInt(rbA: RigidBody, rbB: RigidBody, frameInA: Transform, frameInB: Transform) {

        // calculate transforms
        calculatedTransformA.setMul(rbA.worldTransform, frameInA)
        calculatedTransformB.setMul(rbB.worldTransform, frameInB)
        realPivotAInW.set(calculatedTransformA.origin)
        realPivotBInW.set(calculatedTransformB.origin)
        calculatedTransformA.basis.getColumn(0, sliderAxis) // along X
        realPivotBInW.sub(realPivotAInW, delta)
        sliderAxis.mulAdd(sliderAxis.dot(delta), realPivotAInW, projPivotInW)
        projPivotInW.sub(rbA.worldTransform.origin, relPosA)
        realPivotBInW.sub(rbB.worldTransform.origin, relPosB)

        // linear part
        for (i in 0 until 3) {
            val normalWorld = jacLin[i]
            calculatedTransformA.basis.getColumn(i, normalWorld)

            jacLinDiagABInv[i] = JacobianEntry.calculateDiagonalInv(
                rbA.worldTransform.basis, rbB.worldTransform.basis,
                relPosA, relPosB, normalWorld,
                rbA.invInertiaLocal, rbA.inverseMass,
                rbB.invInertiaLocal, rbB.inverseMass
            )

            depth[i] = delta.dot(normalWorld)
        }
        testLinLimits()

        // angular part
        testAngLimits()

        val axisA = Stack.newVec3f()
        calculatedTransformA.basis.getColumn(0, axisA)
        kAngle = 1f / (rbA.computeAngularImpulseDenominator(axisA) + rbB.computeAngularImpulseDenominator(axisA))
        // clear accumulator for motors
        accumulatedLinearMotorImpulse = 0f
        accumulatedAngMotorImpulse = 0f
        Stack.subVec3f(1)
    }

    fun solveConstraintInt(rbA: RigidBody, rbB: RigidBody) {
        val tmp = Stack.newVec3f()

        // linear
        val velA = rbA.getVelocityInLocalPoint(relPosA, Stack.newVec3f())
        val velB = rbB.getVelocityInLocalPoint(relPosB, Stack.newVec3f())
        val relVel = Stack.newVec3f()
        velA.sub(velB, relVel)

        val impulseVector = Stack.newVec3f()
        for (i in 0..2) {
            val normal = jacLin[i]
            val relVel = normal.dot(relVel)
            // calculate positional error
            val depth = depth[i]
            // get parameters
            val softness =
                if (i != 0) softnessOrthogonalLinear
                else if (solveLinLim) softnessLimitLinear
                else softnessDirLinear
            val restitution =
                if (i != 0) restitutionOrthogonalLinear
                else if (solveLinLim) restitutionLimitLinear
                else restitutionDirLinear
            val damping =
                if (i != 0) dampingOrthogonalLinear
                else if (solveLinLim) dampingLimitLinear
                else dampingDirLinear

            // calculate and apply impulse
            var normalImpulse = softness * (restitution * depth / timeStep - damping * relVel) * jacLinDiagABInv[i]
            if (abs(normalImpulse) > breakingImpulseThreshold) {
                isBroken = true
                break
            }

            normal.mul(normalImpulse, impulseVector)
            rbA.applyImpulse(impulseVector, relPosA)
            impulseVector.negate(tmp)
            rbB.applyImpulse(tmp, relPosB)

            if (poweredLinearMotor && i == 0) {
                // apply linear motor
                if (accumulatedLinearMotorImpulse < maxLinearMotorForce) {
                    val desiredMotorVel = targetLinearMotorVelocity
                    val motorRelVel = desiredMotorVel + relVel
                    normalImpulse = -motorRelVel * jacLinDiagABInv[i]
                    // clamp accumulated impulse
                    var newAcc = accumulatedLinearMotorImpulse + abs(normalImpulse)
                    if (newAcc > maxLinearMotorForce) {
                        newAcc = maxLinearMotorForce
                    }
                    val del = newAcc - accumulatedLinearMotorImpulse
                    normalImpulse = if (normalImpulse < 0.0) -del else del

                    accumulatedLinearMotorImpulse = newAcc
                    // apply clamped impulse
                    normal.mul(normalImpulse, impulseVector)
                    rbA.applyImpulse(impulseVector, relPosA)
                    impulseVector.negate(tmp)
                    rbB.applyImpulse(tmp, relPosB)
                }
            }
        }

        // angular
        // get axes in world space
        val axisA = Stack.newVec3f()
        val axisB = Stack.newVec3f()
        calculatedTransformA.basis.getColumn(0, axisA)
        calculatedTransformB.basis.getColumn(0, axisB)

        val angVelA = rbA.angularVelocity
        val angVelB = rbB.angularVelocity

        val angVelAroundAxisA = Stack.newVec3f()
        val angVelAroundAxisB = Stack.newVec3f()
        val angleAOrthogonal = Stack.newVec3f()
        val angleBOrthogonal = Stack.newVec3f()
        val velRelOrthogonal = Stack.newVec3f()

        axisA.mul(axisA.dot(angVelA), angVelAroundAxisA)
        axisB.mul(axisB.dot(angVelB), angVelAroundAxisB)
        angVelA.sub(angVelAroundAxisA, angleAOrthogonal)
        angVelB.sub(angVelAroundAxisB, angleBOrthogonal)
        angleAOrthogonal.sub(angleBOrthogonal, velRelOrthogonal)

        // solve orthogonal angular velocity correction
        val len = velRelOrthogonal.length()
        if (len > 0.00001f) {
            val normal = Stack.newVec3f()
            velRelOrthogonal.normalize(normal)
            val denominator = rbA.computeAngularImpulseDenominator(normal) +
                    rbB.computeAngularImpulseDenominator(normal)
            velRelOrthogonal.mul((1f / denominator) * dampingOrthogonalAngular * softnessOrthogonalAngular)
            Stack.subVec3f(1)
        }

        // solve angular positional correction
        val angularError = Stack.newVec3f()
        axisA.cross(axisB, angularError)
        angularError.mul(1f / timeStep)
        val len2 = angularError.length()
        if (len2 > 0.00001f) {
            val normal = Stack.newVec3f()
            angularError.normalize(normal)
            val denominator = rbA.computeAngularImpulseDenominator(normal) +
                    rbB.computeAngularImpulseDenominator(normal)
            angularError.mul((1f / denominator) * restitutionOrthogonalAngular * softnessOrthogonalAngular)
            Stack.subVec3f(1)
        }

        // apply impulse
        angularError.sub(velRelOrthogonal, tmp)
        rbA.applyTorqueImpulse(tmp)
        tmp.negate()
        rbB.applyTorqueImpulse(tmp)
        Stack.subVec3f(1) // angularError

        var impulseMag: Float

        // solve angular limits
        angVelB.sub(angVelA, tmp)
        if (solveAngLim) {
            impulseMag = tmp.dot(axisA) * dampingLimitAngular + angularPosition * restitutionLimitAngular / timeStep
            impulseMag *= kAngle * softnessLimitAngular
        } else {
            impulseMag = tmp.dot(axisA) * dampingDirAngular + angularPosition * restitutionDirAngular / timeStep
            impulseMag *= kAngle * softnessDirAngular
        }

        if (abs(impulseMag) > breakingImpulseThreshold) {
            isBroken = true
            impulseMag = 0f
        }

        applyTorqueImpulse(impulseMag, axisA, tmp, rbA, rbB)

        // apply angular motor
        if (poweredAngularMotor && accumulatedAngMotorImpulse < maxAngularMotorForce) {
            applyAngularMotor(
                angVelAroundAxisA, angVelAroundAxisB,
                axisA, tmp, rbA, rbB
            )
        }

        Stack.subVec3f(12)
    }

    private fun applyTorqueImpulse(
        impulseMag: Float,
        axisA: Vector3f, tmp: Vector3f,
        rbA: RigidBody, rbB: RigidBody
    ) {
        val impulse = Stack.newVec3f()
        axisA.mul(impulseMag, impulse)
        rbA.applyTorqueImpulse(impulse)
        impulse.negate(tmp)
        rbB.applyTorqueImpulse(tmp)
        Stack.subVec3f(1) // impulse
    }

    private fun applyAngularMotor(
        angVelAroundAxisA: Vector3f,
        angVelAroundAxisB: Vector3f,
        axisA: Vector3f, tmp: Vector3f,
        rbA: RigidBody, rbB: RigidBody
    ) {
        val velRel = Stack.newVec3f()
        angVelAroundAxisA.sub(angVelAroundAxisB, velRel)
        val projRelVel = velRel.dot(axisA)

        val desiredMotorVel = targetAngularMotorVelocity
        val motorRelVel = desiredMotorVel - projRelVel

        var angImpulse = kAngle * motorRelVel
        if (abs(angImpulse) > breakingImpulseThreshold) {
            isBroken = true
            angImpulse = 0f
        }

        // clamp accumulated impulse
        var newAcc = accumulatedAngMotorImpulse + abs(angImpulse)
        if (newAcc > maxAngularMotorForce) {
            newAcc = maxAngularMotorForce
        }
        val del = newAcc - accumulatedAngMotorImpulse
        angImpulse = if (angImpulse < 0f) -del else del

        accumulatedAngMotorImpulse = newAcc

        // apply clamped impulse
        val motorImp = Stack.newVec3f()
        axisA.mul(angImpulse, motorImp)
        rbA.applyTorqueImpulse(motorImp)
        motorImp.negate(tmp)
        rbB.applyTorqueImpulse(tmp)

        Stack.subVec3f(2) // motorImp, velRel
    }

    fun testLinLimits() {
        solveLinLim = false
        linearPosition = depth.x
        if (lowerLinearLimit <= upperLinearLimit) {
            if (depth.x > upperLinearLimit) {
                depth.x -= upperLinearLimit
                solveLinLim = true
            } else if (depth.x < lowerLinearLimit) {
                depth.x -= lowerLinearLimit
                solveLinLim = true
            } else {
                depth.x = 0f
            }
        } else {
            depth.x = 0f
        }
    }

    fun testAngLimits() {
        angularPosition = 0f
        solveAngLim = false
        if (lowerAngularLimit <= upperAngularLimit) {
            val axisAY = Stack.newVec3f()
            val axisAZ = Stack.newVec3f()
            val axisBY = Stack.newVec3f()
            calculatedTransformA.basis.getColumn(1, axisAY)
            calculatedTransformA.basis.getColumn(2, axisAZ)
            calculatedTransformB.basis.getColumn(1, axisBY)

            val rotation = atan2(axisBY.dot(axisAZ), axisBY.dot(axisAY))
            if (rotation < lowerAngularLimit) {
                angularPosition = rotation - lowerAngularLimit
                solveAngLim = true
            } else if (rotation > upperAngularLimit) {
                angularPosition = rotation - upperAngularLimit
                solveAngLim = true
            }
            Stack.subVec3f(3)
        }
    }

    companion object {
        const val SLIDER_CONSTRAINT_DEF_SOFTNESS = 1f
        const val SLIDER_CONSTRAINT_DEF_DAMPING = 1f
        const val SLIDER_CONSTRAINT_DEF_RESTITUTION = 0.7f
    }
}
