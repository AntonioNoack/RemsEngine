package com.bulletphysics.linearmath

import com.bulletphysics.BulletGlobals
import com.bulletphysics.linearmath.MatrixUtil.getRotation
import com.bulletphysics.linearmath.QuaternionUtil.getAngle
import com.bulletphysics.util.setMul
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setScaleAdd
import cz.advel.stack.Stack
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions for transforms.
 *
 * @author jezek2
 */
object TransformUtil {

    private const val SIMD_SQRT12 = 0.7071067811865476
    private const val ANGULAR_MOTION_THRESHOLD = 0.5 * BulletGlobals.SIMD_HALF_PI
    private const val INV_48 = 1.0 / 48.0

    @JvmStatic
    fun planeSpace1(n: Vector3d, p: Vector3d, q: Vector3d) {
        if (abs(n.z) > SIMD_SQRT12) {
            // choose p in y-z plane
            val a = n.y * n.y + n.z * n.z
            val k = 1.0 / sqrt(a)
            p.set(0.0, -n.z * k, n.y * k)
            // set q = n x p
            q.set(a * k, -n.x * p.z, n.x * p.y)
        } else {
            // choose p in x-y plane
            val a = n.x * n.x + n.y * n.y
            val k = 1.0 / sqrt(a)
            p.set(-n.y * k, n.x * k, 0.0)
            // set q = n x p
            q.set(-n.z * p.y, n.z * p.x, a * k)
        }
    }

    @JvmStatic
    fun integrateTransform(
        curTrans: Transform, linearVelocity: Vector3d, angularVelocity: Vector3d,
        timeStep: Double, predictedTransform: Transform
    ) {
        predictedTransform.origin.setScaleAdd(timeStep, linearVelocity, curTrans.origin)

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
            axis.setScale(0.5 * timeStep - (timeStep * timeStep * timeStep) * INV_48 * fAngle * fAngle, angularVelocity)
        } else {
            // sync(fAngle) = sin(c*fAngle)/t
            axis.setScale(sin(0.5 * fAngle * timeStep) / fAngle, angularVelocity)
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
        angVel.setScale(angle / timeStep, axis)
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
        dmat.setMul(transform1.basis, tmp)

        val dorn = Stack.newQuat()
        getRotation(dmat, dorn)

        // floating point inaccuracy can lead to w component > 1..., which breaks
        dorn.normalize()

        val result = getAngle(dorn)
        axis.set(dorn.x, dorn.y, dorn.z)

        // check for axis length
        val len = axis.lengthSquared()
        if (len < BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON) {
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
        transform0.basis.invert(tmp)

        val dmat = Stack.newMat()
        dmat.setMul(transform1.basis, tmp)

        val dorn = Stack.newQuat()
        getRotation(dmat, dorn)

        // floating point inaccuracy can lead to w component > 1..., which breaks
        dorn.normalize()

        val result = getAngle(dorn)

        Stack.subMat(2)
        Stack.subQuat(1)

        return result
    }
}
