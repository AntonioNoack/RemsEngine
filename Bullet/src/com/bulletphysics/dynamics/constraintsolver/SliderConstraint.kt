/*
Added by Roman Ponomarev (rponom@gmail.com)
April 04, 2008

TODO:
 - add clamping od accumulated impulse to improve stability
*/
package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setNormalize
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setScaleAdd
import com.bulletphysics.util.setSub
import kotlin.math.abs
import kotlin.math.atan2

// JAVA NOTE: SliderConstraint from 2.71
/**
 * @author jezek2
 */
class SliderConstraint : TypedConstraint {
    val frameOffsetA: Transform = Transform()
    val frameOffsetB: Transform = Transform()

    // use frameA fo define limits, if true
    var useLinearReferenceFrameA: Boolean

    // linear limits
    @JvmField
    var lowerLinearLimit: Double = 0.0

    @JvmField
    var upperLinearLimit: Double = 0.0

    // angular limits
    @JvmField
    var lowerAngularLimit: Double = 0.0

    @JvmField
    var upperAngularLimit: Double = 0.0

    // softness, restitution and damping for different cases
    // DirLin - moving inside linear limits
    // LimLin - hitting linear limit
    // DirAng - moving inside angular limits
    // LimAng - hitting angular limit
    // OrthoLin, OrthoAng - against constraint axis
    var softnessDirLinear: Double = 0.0
    var restitutionDirLinear: Double = 0.0
    var dampingDirLinear: Double = 0.0
    var softnessDirAngular: Double = 0.0
    var restitutionDirAngular: Double = 0.0
    var dampingDirAngular: Double = 0.0
    var softnessLimitLinear: Double = 0.0
    var restitutionLimitLinear: Double = 0.0
    var dampingLimitLinear: Double = 0.0
    var softnessLimitAngular: Double = 0.0
    var restitutionLimitAngular: Double = 0.0
    var dampingLimitAngular: Double = 0.0
    var softnessOrthogonalLinear: Double = 0.0
    var restitutionOrthogonalLinear: Double = 0.0
    var dampingOrthogonalLinear: Double = 0.0
    var softnessOrthogonalAngular: Double = 0.0
    var restitutionOrthogonalAngular: Double = 0.0
    var dampingOrthogonalAngular: Double = 0.0

    // for internal use
    private var solveLinLim = false
    private var solveAngLim = false

    private val jacLin: Array<JacobianEntry> = arrayOf(JacobianEntry(), JacobianEntry(), JacobianEntry())
    private val jacLinDiagABInv = DoubleArray(3)

    private var timeStep = 0.0
    val calculatedTransformA: Transform = Transform()
    val calculatedTransformB: Transform = Transform()

    private val sliderAxis = Vector3d()
    private val realPivotAInW = Vector3d()
    private val realPivotBInW = Vector3d()
    private val projPivotInW = Vector3d()
    private val delta = Vector3d()
    private val depth = Vector3d()
    private val relPosA = Vector3d()
    private val relPosB = Vector3d()

    var linearPosition: Double = 0.0
    var angularPosition: Double = 0.0
    private var kAngle = 0.0

    var poweredLinearMotor: Boolean = false
    var targetLinearMotorVelocity: Double = 0.0
    var maxLinearMotorForce: Double = 0.0
    private var accumulatedLinearMotorImpulse = 0.0

    var poweredAngularMotor: Boolean = false
    var targetAngularMotorVelocity: Double = 0.0
    var maxAngularMotorForce: Double = 0.0
    private var accumulatedAngMotorImpulse = 0.0

    @Suppress("unused")
    constructor() {
        useLinearReferenceFrameA = true
        initParams()
    }

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
        lowerLinearLimit = 1.0
        upperLinearLimit = -1.0
        lowerAngularLimit = 0.0
        upperAngularLimit = 0.0
        softnessDirLinear = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionDirLinear = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingDirLinear = 0.0
        softnessDirAngular = SLIDER_CONSTRAINT_DEF_SOFTNESS
        restitutionDirAngular = SLIDER_CONSTRAINT_DEF_RESTITUTION
        dampingDirAngular = 0.0
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
        targetLinearMotorVelocity = 0.0
        maxLinearMotorForce = 0.0
        accumulatedLinearMotorImpulse = 0.0

        poweredAngularMotor = false
        targetAngularMotorVelocity = 0.0
        maxAngularMotorForce = 0.0
        accumulatedAngMotorImpulse = 0.0
    }

    override fun buildJacobian() {
        if (useLinearReferenceFrameA) {
            buildJacobianInt(rigidBodyA, rigidBodyB, frameOffsetA, frameOffsetB)
        } else {
            buildJacobianInt(rigidBodyB, rigidBodyA, frameOffsetB, frameOffsetA)
        }
    }

    override fun solveConstraint(timeStep: Double) {
        this.timeStep = timeStep
        val stackPos = Stack.getPosition(null)
        if (useLinearReferenceFrameA) {
            solveConstraintInt(rigidBodyA, rigidBodyB)
        } else {
            solveConstraintInt(rigidBodyB, rigidBodyA)
        }
        Stack.reset(stackPos)
    }

    fun buildJacobianInt(rbA: RigidBody, rbB: RigidBody, frameInA: Transform, frameInB: Transform) {
        val tmpTrans = Stack.newTrans()
        val tmpTrans1 = Stack.newTrans()
        val tmpTrans2 = Stack.newTrans()
        val tmp = Stack.newVec()
        val tmp2 = Stack.newVec()

        // calculate transforms
        calculatedTransformA.setMul(rbA.getCenterOfMassTransform(tmpTrans), frameInA)
        calculatedTransformB.setMul(rbB.getCenterOfMassTransform(tmpTrans), frameInB)
        realPivotAInW.set(calculatedTransformA.origin)
        realPivotBInW.set(calculatedTransformB.origin)
        calculatedTransformA.basis.getColumn(0, tmp)
        sliderAxis.set(tmp) // along X
        delta.setSub(realPivotBInW, realPivotAInW)
        projPivotInW.setScaleAdd(sliderAxis.dot(delta), sliderAxis, realPivotAInW)
        relPosA.setSub(projPivotInW, rbA.getCenterOfMassPosition(tmp))
        relPosB.setSub(realPivotBInW, rbB.getCenterOfMassPosition(tmp))
        val normalWorld = Stack.newVec()

        // linear part
        for (i in 0..2) {
            calculatedTransformA.basis.getColumn(i, normalWorld)

            val mat1 = rbA.getCenterOfMassTransform(tmpTrans1).basis
            mat1.transpose()

            val mat2 = rbB.getCenterOfMassTransform(tmpTrans2).basis
            mat2.transpose()

            jacLin[i].init(
                mat1,
                mat2,
                relPosA,
                relPosB,
                normalWorld,
                rbA.getInvInertiaDiagLocal(tmp),
                rbA.inverseMass,
                rbB.getInvInertiaDiagLocal(tmp2),
                rbB.inverseMass
            )
            jacLinDiagABInv[i] = 1.0 / jacLin[i].diagonal
            setCoord(depth, i, delta.dot(normalWorld))
        }
        testLinLimits()

        // angular part
        testAngLimits()

        val axisA = Stack.newVec()
        calculatedTransformA.basis.getColumn(0, axisA)
        kAngle = 1.0 / (rbA.computeAngularImpulseDenominator(axisA) + rbB.computeAngularImpulseDenominator(axisA))
        // clear accumulator for motors
        accumulatedLinearMotorImpulse = 0.0
        accumulatedAngMotorImpulse = 0.0
    }

    fun solveConstraintInt(rbA: RigidBody, rbB: RigidBody) {
        // todo the stack isn't cleaned here
        val tmp = Stack.newVec()

        // linear
        val velA = rbA.getVelocityInLocalPoint(relPosA, Stack.newVec())
        val velB = rbB.getVelocityInLocalPoint(relPosB, Stack.newVec())
        val vel = Stack.newVec()
        vel.setSub(velA, velB)

        val impulseVector = Stack.newVec()

        for (i in 0..2) {
            val normal = jacLin[i].linearJointAxis
            val relVel = normal.dot(vel)
            // calculate positional error
            val depth = getCoord(this.depth, i)
            // get parameters
            val softness =
                if (i != 0) softnessOrthogonalLinear else if (solveLinLim) softnessLimitLinear else softnessDirLinear
            val restitution =
                if (i != 0) restitutionOrthogonalLinear else if (solveLinLim) restitutionLimitLinear else restitutionDirLinear
            val damping =
                if (i != 0) dampingOrthogonalLinear else if (solveLinLim) dampingLimitLinear else dampingDirLinear
            // calculate and apply impulse
            var normalImpulse = softness * (restitution * depth / timeStep - damping * relVel) * jacLinDiagABInv[i]
            if (abs(normalImpulse) > breakingImpulseThreshold) {
                isBroken = true
                break
            }

            impulseVector.setScale(normalImpulse, normal)
            rbA.applyImpulse(impulseVector, relPosA)
            tmp.setNegate(impulseVector)
            rbB.applyImpulse(tmp, relPosB)

            if (poweredLinearMotor && (i == 0)) {
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
                    if (normalImpulse < 0.0) {
                        normalImpulse = -del
                    } else {
                        normalImpulse = del
                    }
                    accumulatedLinearMotorImpulse = newAcc
                    // apply clamped impulse
                    impulseVector.setScale(normalImpulse, normal)
                    rbA.applyImpulse(impulseVector, relPosA)
                    tmp.setNegate(impulseVector)
                    rbB.applyImpulse(tmp, relPosB)
                }
            }
        }

        // angular
        // get axes in world space
        val axisA = Stack.newVec()
        calculatedTransformA.basis.getColumn(0, axisA)
        val axisB = Stack.newVec()
        calculatedTransformB.basis.getColumn(0, axisB)

        val angVelA = rbA.angularVelocity
        val angVelB = rbB.angularVelocity

        val angVelAroundAxisA = Stack.newVec()
        angVelAroundAxisA.setScale(axisA.dot(angVelA), axisA)
        val angVelAroundAxisB = Stack.newVec()
        angVelAroundAxisB.setScale(axisB.dot(angVelB), axisB)

        val angleAOrthogonal = Stack.newVec()
        angleAOrthogonal.setSub(angVelA, angVelAroundAxisA)
        val angleBOrthogonal = Stack.newVec()
        angleBOrthogonal.setSub(angVelB, angVelAroundAxisB)
        val velRelOrthogonal = Stack.newVec()
        velRelOrthogonal.setSub(angleAOrthogonal, angleBOrthogonal)

        // solve orthogonal angular velocity correction
        val len = velRelOrthogonal.length()
        if (len > 0.00001) {
            val normal = Stack.newVec()
            normal.setNormalize(velRelOrthogonal)
            val denominator =
                rbA.computeAngularImpulseDenominator(normal) + rbB.computeAngularImpulseDenominator(normal)
            velRelOrthogonal.mul((1.0 / denominator) * dampingOrthogonalAngular * softnessOrthogonalAngular)
        }

        // solve angular positional correction
        val angularError = Stack.newVec()
        angularError.setCross(axisA, axisB)
        angularError.mul(1.0 / timeStep)
        val len2 = angularError.length()
        if (len2 > 0.00001) {
            val normal2 = Stack.newVec()
            normal2.setNormalize(angularError)
            val denominator =
                rbA.computeAngularImpulseDenominator(normal2) + rbB.computeAngularImpulseDenominator(normal2)
            angularError.mul((1.0 / denominator) * restitutionOrthogonalAngular * softnessOrthogonalAngular)
        }

        // apply impulse
        tmp.setNegate(velRelOrthogonal)
        tmp.add(angularError)
        rbA.applyTorqueImpulse(tmp)
        tmp.setSub(velRelOrthogonal, angularError)
        rbB.applyTorqueImpulse(tmp)
        var impulseMag: Double

        // solve angular limits
        if (solveAngLim) {
            tmp.setSub(angVelB, angVelA)
            impulseMag = tmp.dot(axisA) * dampingLimitAngular + angularPosition * restitutionLimitAngular / timeStep
            impulseMag *= kAngle * softnessLimitAngular
        } else {
            tmp.setSub(angVelB, angVelA)
            impulseMag = tmp.dot(axisA) * dampingDirAngular + angularPosition * restitutionDirAngular / timeStep
            impulseMag *= kAngle * softnessDirAngular
        }

        if (abs(impulseMag) > breakingImpulseThreshold) {
            isBroken = true
            impulseMag = 0.0
        }

        val impulse = Stack.newVec()
        impulse.setScale(impulseMag, axisA)
        rbA.applyTorqueImpulse(impulse)
        tmp.setNegate(impulse)
        rbB.applyTorqueImpulse(tmp)

        // apply angular motor
        if (poweredAngularMotor) {
            if (accumulatedAngMotorImpulse < maxAngularMotorForce) {
                val velRel = Stack.newVec()
                velRel.setSub(angVelAroundAxisA, angVelAroundAxisB)
                val projRelVel = velRel.dot(axisA)

                val desiredMotorVel = targetAngularMotorVelocity
                val motorRelVel = desiredMotorVel - projRelVel

                var angImpulse = kAngle * motorRelVel
                if (abs(angImpulse) > breakingImpulseThreshold) {
                    isBroken = true
                    angImpulse = 0.0
                }

                // clamp accumulated impulse
                var newAcc = accumulatedAngMotorImpulse + abs(angImpulse)
                if (newAcc > maxAngularMotorForce) {
                    newAcc = maxAngularMotorForce
                }
                val del = newAcc - accumulatedAngMotorImpulse
                angImpulse = if (angImpulse < 0.0) -del else del

                accumulatedAngMotorImpulse = newAcc

                // apply clamped impulse
                val motorImp = Stack.newVec()
                motorImp.setScale(angImpulse, axisA)
                rbA.applyTorqueImpulse(motorImp)
                tmp.setNegate(motorImp)
                rbB.applyTorqueImpulse(tmp)
            }
        }
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
                depth.x = 0.0
            }
        } else {
            depth.x = 0.0
        }
    }

    fun testAngLimits() {
        angularPosition = 0.0
        solveAngLim = false
        if (lowerAngularLimit <= upperAngularLimit) {
            val axisA0 = Stack.newVec()
            calculatedTransformA.basis.getColumn(1, axisA0)
            val axisA1 = Stack.newVec()
            calculatedTransformA.basis.getColumn(2, axisA1)
            val axisB0 = Stack.newVec()
            calculatedTransformB.basis.getColumn(1, axisB0)

            val rot = atan2(axisB0.dot(axisA1), axisB0.dot(axisA0))
            if (rot < lowerAngularLimit) {
                angularPosition = rot - lowerAngularLimit
                solveAngLim = true
            } else if (rot > upperAngularLimit) {
                angularPosition = rot - upperAngularLimit
                solveAngLim = true
            }
            Stack.subVec(3)
        }
    }

    companion object {
        const val SLIDER_CONSTRAINT_DEF_SOFTNESS = 1.0
        const val SLIDER_CONSTRAINT_DEF_DAMPING = 1.0
        const val SLIDER_CONSTRAINT_DEF_RESTITUTION = 0.7
    }
}
