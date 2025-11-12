package com.bulletphysics.linearmath

import com.bulletphysics.BulletGlobals
import cz.advel.stack.Stack
import org.joml.Matrix3d
import org.joml.Matrix3f
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Utility function for matrices.
 *
 * @author jezek2
 */
object DiagonalizeMatrix {

    /**
     * Diagonalizes this matrix by the Jacobi method. rot stores the rotation
     * from the coordinate system in which the matrix is diagonal to the original
     * coordinate system, i.e., old_this = rot * new_this * rot^T. The iteration
     * stops when all off-diagonal elements are less than the threshold multiplied
     * by the sum of the absolute values of the diagonal, or when maxSteps have
     * been executed. Note that this matrix is assumed to be symmetric.
     */
    // JAVA NOTE: diagonalize method from 2.71
    fun diagonalize(mat: Matrix3f, rot: Matrix3f, threshold: Float, maxSteps: Int) {
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
            val mpq = mat[q, p].toFloat()
            val theta = (mat[q, q] - mat[p, p]).toFloat() / (2f * mpq)
            val theta2 = theta * theta
            val cos: Float
            if ((theta2 * theta2) < (10f / BulletGlobals.SIMD_EPSILON)) {
                t = if (theta >= 0f)
                    1f / (theta + sqrt(1f + theta2))
                else
                    1f / (theta - sqrt(1f + theta2))
                cos = 1f / sqrt(1f + t * t)
            } else {
                // approximation for large theta-value, i.e., a nearly diagonal matrix
                t = 1f / (theta * (2f + 0.5f / theta2))
                cos = 1f - 0.5f * t * t
            }
            val sin = cos * t

            // apply rotation to matrix (this = J^T * this * J)
            mat[q, p] = 0.0
            mat[p, q] = 0.0
            mat[p, p] = mat[p, p] - t * mpq
            mat[q, q] = mat[q, q] + t * mpq
            val mrp = mat[p, r]
            val mrq = mat[q, r]
            mat[p, r] = cos * mrp - sin * mrq
            mat[r, p] = cos * mrp - sin * mrq
            mat[q, r] = cos * mrq + sin * mrp
            mat[r, q] = cos * mrq + sin * mrp

            // apply rotation to rot (rot = rot * J)
            val row = Stack.borrowVec3f()
            for (i in 0..2) {
                rot.getRow(i, row)

                val mrp = row[p]
                val mrq = row[q]
                row[p] = cos * mrp - sin * mrq
                row[q] = cos * mrq + sin * mrp
                rot.setRow(i, row)
            }
            step--
        }
    }
}
