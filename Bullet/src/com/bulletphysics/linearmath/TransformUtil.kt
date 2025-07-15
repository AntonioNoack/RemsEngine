package com.bulletphysics.linearmath

import com.bulletphysics.BulletGlobals
import cz.advel.stack.Stack
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions for transforms.
 *
 * @author jezek2
 */
object TransformUtil {

    private const val ANGULAR_MOTION_THRESHOLD = 0.5 * BulletGlobals.SIMD_HALF_PI
    private const val INV_48 = 1.0 / 48.0

    @JvmStatic
    fun integrateTransform(
        curTrans: Transform, linearVelocity: Vector3d, angularVelocity: Vector3d,
        timeStep: Double, predictedTransform: Transform
    ) {
        linearVelocity.mulAdd(timeStep, curTrans.origin, predictedTransform.origin)

        //	//#define QUATERNION_DERIVATIVE
//	#ifdef QUATERNION_DERIVATIVE
//		btQuaternion predictedOrn = curTrans.getRotation();
//		predictedOrn += (angvel * predictedOrn) * (timeStep * btScalar(0.5));
//		predictedOrn.normalize();
//	#else
        // Exponential map
        // google for "Practical Parameterization of Rotations Using the Exponential Map", F. Sebastian Grassia
        val axis = Stack.newVec()
        var fAngle = angularVelocity.length()

        // limit the angular motion
        if (fAngle * timeStep > ANGULAR_MOTION_THRESHOLD) {
            fAngle = ANGULAR_MOTION_THRESHOLD / timeStep
        }

        if (fAngle < 0.001f) {
            // use Taylor's expansions of sync function
            angularVelocity.mul(0.5 * timeStep - (timeStep * timeStep * timeStep) * INV_48 * fAngle * fAngle, axis)
        } else {
            // sync(fAngle) = sin(c*fAngle)/t
            angularVelocity.mul(sin(0.5 * fAngle * timeStep) / fAngle, axis)
        }

        val dorn = Stack.newQuat()
        dorn.set(axis.x, axis.y, axis.z, cos(fAngle * timeStep * 0.5))
        val orn0 = curTrans.getRotation(Stack.newQuat())

        val predictedOrn = Stack.newQuat()
        dorn.mul(orn0, predictedOrn) // new API!
        predictedOrn.normalize()
        //  #endif
        predictedTransform.setRotation(predictedOrn)

        Stack.subVec(1)
        Stack.subQuat(3)
    }

    @JvmStatic
    fun calculateAngularVelocity(transform0: Transform, transform1: Transform, timeStep: Double, angVel: Vector3d) {
        val axis = Stack.newVec()
        val angle = calculateDiffAxisAngle(transform0, transform1, axis)
        axis.mul(angle / timeStep, angVel)
        Stack.subVec(1)
    }

    @JvmStatic
    fun calculateVelocity(transform0: Transform, transform1: Transform, timeStep: Double, linVel: Vector3d): Double {
        calculateLinearVelocity(transform0, transform1, timeStep, linVel)
        return calculateDiffAxisAngle(transform0, transform1) / timeStep
    }

    @JvmStatic
    fun calculateLinearVelocity(transform0: Transform, transform1: Transform, timeStep: Double, linVel: Vector3d) {
        transform1.origin.sub(transform0.origin, linVel).mul(1.0 / timeStep)
    }

    fun calculateDiffAxisAngle(transform0: Transform, transform1: Transform, axis: Vector3d): Double {
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
            axis.set(1.0, 0.0, 0.0)
        } else {
            axis.mul(1.0 / sqrt(len))
        }

        Stack.subMat(2)
        Stack.subQuat(1)

        return result
    }

    fun calculateDiffAxisAngle(transform0: Transform, transform1: Transform): Double {
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
