package com.bulletphysics.extras.gimpact

import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Matrix3d
import kotlin.math.abs

class BoxBoxTransformCache {
    val T1to0: Vector3d = Vector3d() // Transforms translation of model1 to model 0
    val R1to0: Matrix3d = Matrix3d() // Transforms Rotation of model1 to model 0, equal  to R0' * R1
    val AR: Matrix3d = Matrix3d() // Absolute value of m_R1to0

    fun calcAbsoluteMatrix() {
        //static const btVector3 vepsi(1e-6f,1e-6f,1e-6f);
        //m_AR[0] = vepsi + m_R1to0[0].absolute();
        //m_AR[1] = vepsi + m_R1to0[1].absolute();
        //m_AR[2] = vepsi + m_R1to0[2].absolute();
        val eps = 1e-6
        AR.m00 = eps + abs(R1to0.m00)
        AR.m01 = eps + abs(R1to0.m01)
        AR.m02 = eps + abs(R1to0.m02)

        AR.m10 = eps + abs(R1to0.m10)
        AR.m11 = eps + abs(R1to0.m11)
        AR.m12 = eps + abs(R1to0.m12)

        AR.m20 = eps + abs(R1to0.m20)
        AR.m21 = eps + abs(R1to0.m21)
        AR.m22 = eps + abs(R1to0.m22)
    }

    /**
     * Calc the transformation relative  1 to 0. Inverts matrices by transposing.
     */
    fun calcFromHomogenic(trans0: Transform, trans1: Transform) {
        val tmpTrans = Stack.newTrans()
        tmpTrans.setInverse(trans0)
        tmpTrans.mul(trans1)

        T1to0.set(tmpTrans.origin)
        R1to0.set(tmpTrans.basis)
        Stack.subTrans(1)

        calcAbsoluteMatrix()
    }

    fun transform(point: Vector3d, out: Vector3d): Vector3d {
        var point = point
        val tmp = Stack.newVec()

        if (point === out) {
            point = Stack.borrowVec(point)
        }
        R1to0.getRow(0, tmp)
        out.x = tmp.dot(point) + T1to0.x
        R1to0.getRow(1, tmp)
        out.y = tmp.dot(point) + T1to0.y
        R1to0.getRow(2, tmp)
        out.z = tmp.dot(point) + T1to0.z

        Stack.subVec(1)
        return out
    }
}