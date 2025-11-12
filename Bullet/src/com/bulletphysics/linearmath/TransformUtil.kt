package com.bulletphysics.linearmath

import com.bulletphysics.BulletGlobals
import cz.advel.stack.Stack
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions for transforms.
 *
 * @author jezek2
 */
object TransformUtil {

    private const val ANGULAR_MOTION_THRESHOLD = 0.5f * BulletGlobals.SIMD_HALF_PI
    private const val INV_48 = 1.0f / 48.0f

    @JvmStatic
    fun integrateTransform(
        curTrans: Transform, linearVelocity: Vector3f, angularVelocity: Vector3f,
        timeStep: Float, predictedTransform: Transform
    ) {
        linearVelocity.mulAdd(timeStep.toDouble(), curTrans.origin, predictedTransform.origin)

        //	//#define QUATERNION_DERIVATIVE
//	#ifdef QUATERNION_DERIVATIVE
//		btQuaternion predictedOrn = curTrans.getRotation();
//		predictedOrn += (angvel * predictedOrn) * (timeStep * btScalar(0.5));
//		predictedOrn.normalize();
//	#else
        // Exponential map
        // google for "Practical Parameterization of Rotations Using the Exponential Map", F. Sebastian Grassia
        val axis = Stack.newVec3f()
        var fAngle = angularVelocity.length()

        // limit the angular motion
        if (fAngle * timeStep > ANGULAR_MOTION_THRESHOLD) {
            fAngle = ANGULAR_MOTION_THRESHOLD / timeStep
        }

        if (fAngle < 0.001f) {
            // use Taylor's expansions of sync function
            angularVelocity.mul(0.5f * timeStep - (timeStep * timeStep * timeStep) * INV_48 * fAngle * fAngle, axis)
        } else {
            // sync(fAngle) = sin(c*fAngle)/t
            angularVelocity.mul(sin(0.5f * fAngle * timeStep) / fAngle, axis)
        }

        val dorn = Stack.newQuat()
        dorn.set(axis.x, axis.y, axis.z, cos(fAngle * timeStep * 0.5f))
        val orn0 = curTrans.getRotation(Stack.newQuat())

        val predictedOrn = Stack.newQuat()
        dorn.mul(orn0, predictedOrn) // new API!
        predictedOrn.normalize()
        //  #endif
        predictedTransform.setRotation(predictedOrn)

        Stack.subVec3f(1)
        Stack.subQuat(3)
    }

    @JvmStatic
    fun calculateAngularVelocity(transform0: Transform, transform1: Transform, timeStep: Float, angVel: Vector3f) {
        val axis = Stack.newVec3f()
        val angle = calculateDiffAxisAngle(transform0, transform1, axis)
        axis.mul(angle / timeStep, angVel)
        Stack.subVec3f(1)
    }

    @JvmStatic
    fun calculateVelocity(transform0: Transform, transform1: Transform, timeStep: Float, linVel: Vector3f): Float {
        calculateLinearVelocity(transform0, transform1, timeStep, linVel)
        return calculateDiffAxisAngle(transform0, transform1) / timeStep
    }

    @JvmStatic
    fun calculateLinearVelocity(transform0: Transform, transform1: Transform, timeStep: Float, linVel: Vector3f) {
        transform1.origin.sub(transform0.origin, linVel).mul(1f / timeStep)
    }

    fun calculateDiffAxisAngle(transform0: Transform, transform1: Transform, axis: Vector3f): Float {
        val tmp = Stack.newMat()
        transform0.basis.invert(tmp)

        val dmat = Stack.newMat()
        transform1.basis.mul(tmp, dmat)

        val dorn = Stack.newQuat()
        dmat.getUnnormalizedRotation(dorn)

        // floating point inaccuracy can lead to w component > 1..., which breaks
        dorn.normalize()

        val result = dorn.angle()
        axis.set(dorn.x, dorn.y, dorn.z)

        // check for axis length
        val len = axis.lengthSquared()
        if (len < BulletGlobals.FLT_EPSILON_SQ) {
            axis.set(1f, 0f, 0f)
        } else {
            axis.mul(1f / sqrt(len))
        }

        Stack.subMat(2)
        Stack.subQuat(1)

        return result
    }

    fun calculateDiffAxisAngle(transform0: Transform, transform1: Transform): Float {
        val tmp = Stack.newMat()
        transform0.basis.transpose(tmp)

        val dmat = Stack.newMat()
        transform1.basis.mul(tmp, dmat)

        val dorn = Stack.newQuat()
        dmat.getUnnormalizedRotation(dorn)

        // floating point inaccuracy can lead to w component > 1..., which breaks
        dorn.normalize()

        val result = dorn.angle()

        Stack.subMat(2)
        Stack.subQuat(1)

        return result
    }
}
