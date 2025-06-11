package com.bulletphysics.linearmath

import com.bulletphysics.BulletGlobals
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Matrix3d
import com.bulletphysics.util.getElement
import com.bulletphysics.util.setElement
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Utility functions for matrices.
 *
 * @author jezek2
 */
object MatrixUtil {
    @JvmStatic
    fun scale(dst: Matrix3d, mat: Matrix3d, s: Vector3d) {
        mat.scale(s, dst)
    }

    @JvmStatic
    fun transposeTransform(dst: Vector3d, vec: Vector3d, mat: Matrix3d) {
        mat.transformTranspose(vec, dst)
    }

    @JvmStatic
    fun setRotation(dst: Matrix3d, q: Quaterniond) {
        dst.set(q)
    }

    @JvmStatic
    fun getRotation(mat: Matrix3d, dst: Quaterniond) {
        mat.getUnnormalizedRotation(dst).normalize()
    }

    @JvmStatic
    fun invert(mat: Matrix3d) {
        mat.invert()
    }

    /**
     * Diagonalizes this matrix by the Jacobi method. rot stores the rotation
     * from the coordinate system in which the matrix is diagonal to the original
     * coordinate system, i.e., old_this = rot * new_this * rot^T. The iteration
     * stops when all off-diagonal elements are less than the threshold multiplied
     * by the sum of the absolute values of the diagonal, or when maxSteps have
     * been executed. Note that this matrix is assumed to be symmetric.
     */
    // JAVA NOTE: diagonalize method from 2.71
    fun diagonalize(mat: Matrix3d, rot: Matrix3d, threshold: Double, maxSteps: Int) {
        val row = Stack.newVec()

        rot.identity()
        var step = maxSteps
        while (step > 0) {
            // find off-diagonal element [p][q] with largest magnitude
            var p = 0
            var q = 1
            var r = 2
            var max = abs(mat.m10)
            var v = abs(mat.m20)
            if (v > max) {
                q = 2
                r = 1
                max = v
            }
            v = abs(mat.m21)
            if (v > max) {
                p = 1
                q = 2
                r = 0
                max = v
            }

            var t = threshold * (abs(mat.m00) + abs(mat.m11) + abs(mat.m22))
            if (max <= t) {
                if (max <= BulletGlobals.SIMD_EPSILON * t) {
                    return
                }
                step = 1
            }

            // compute Jacobi rotation J which leads to a zero for element [p][q]
            val mpq = mat.getElement(p, q)
            val theta = (mat.getElement(q, q) - mat.getElement(p, p)) / (2 * mpq)
            val theta2 = theta * theta
            val cos: Double
            if ((theta2 * theta2) < (10f / BulletGlobals.SIMD_EPSILON)) {
                t = if (theta >= 0.0)
                    1.0 / (theta + sqrt(1.0 + theta2))
                else
                    1.0 / (theta - sqrt(1.0 + theta2))
                cos = 1.0 / sqrt(1.0 + t * t)
            } else {
                // approximation for large theta-value, i.e., a nearly diagonal matrix
                t = 1 / (theta * (2 + 0.5 / theta2))
                cos = 1 - 0.5 * t * t
            }
            val sin = cos * t

            // apply rotation to matrix (this = J^T * this * J)
            mat.setElement(p, q, 0.0)
            mat.setElement(q, p, 0.0)
            mat.setElement(p, p, mat.getElement(p, p) - t * mpq)
            mat.setElement(q, q, mat.getElement(q, q) + t * mpq)
            var mrp = mat.getElement(r, p)
            var mrq = mat.getElement(r, q)
            mat.setElement(r, p, cos * mrp - sin * mrq)
            mat.setElement(p, r, cos * mrp - sin * mrq)
            mat.setElement(r, q, cos * mrq + sin * mrp)
            mat.setElement(q, r, cos * mrq + sin * mrp)

            // apply rotation to rot (rot = rot * J)
            for (i in 0..2) {
                rot.getRow(i, row)

                mrp = getCoord(row, p)
                mrq = getCoord(row, q)
                setCoord(row, p, cos * mrp - sin * mrq)
                setCoord(row, q, cos * mrq + sin * mrp)
                rot.setRow(i, row)
            }
            step--
        }
    }
}
