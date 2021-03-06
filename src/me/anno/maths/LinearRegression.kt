package me.anno.maths

import me.anno.maths.LinearAlgebra.inverse
import me.anno.maths.LinearAlgebra.setAx
import me.anno.maths.LinearAlgebra.setABt
import me.anno.maths.LinearAlgebra.setAtB
import me.anno.maths.LinearAlgebra.setAtX
import org.apache.logging.log4j.LogManager
import org.joml.*
import kotlin.math.abs

@Suppress("unused")
object LinearRegression {

    /**
     * @param A feature matrix, row major, [points x features]
     * @param y solution vector, f(x)
     * @return x such that Ax = b
     * */
    fun solve(A: DoubleArray, y: DoubleArray): DoubleArray? {
        val numPts = y.size
        val degree = A.size / numPts
        // y = (XtX)^-1 * Xt * b
        // y = (XtX)^-1 * (Xt * b)
        val xtx = setAtB(A, A, degree, numPts, degree)
        val xty = setAtX(A, y, degree, numPts)
        val xtxInv = inverse(xtx, degree) ?: return null
        return setAx(xtxInv, xty, degree, degree)
    }

    // not tested!
    fun solveT(At: DoubleArray, y: DoubleArray): DoubleArray? {
        val numPts = y.size
        val degree = At.size / numPts
        // beta = (XtX)^-1 * Xt * Y
        val xtx = setABt(At, At, degree, numPts, degree)
        val xty = setAtX(At, y, degree, numPts)
        val xtxInv = inverse(xtx, degree) ?: return null
        return setAx(xtxInv, xty, degree, degree)
    }

    private fun crossMapDoubleT(points: List<Vector2d>, functions: List<(Double) -> Double>): DoubleArray {
        val result = DoubleArray(points.size * functions.size)
        var i = 0
        for (func in functions) {
            for (point in points) {
                val px = point.x
                result[i++] = func(px)
            }
        }
        return result
    }

    fun solve(points: List<Vector2d>, functions: List<(Double) -> Double>): DoubleArray? {
        val xt = crossMapDoubleT(points, functions)
        val y = DoubleArray(points.size) { points[it].y }
        // beta = (XtX)^-1 * Xt * Y
        val xtx = setABt(xt, xt, points.size, functions.size, points.size)
        val xty = setAx(xt, y, points.size, functions.size)
        val xtxInv = inverse(xtx, functions.size) ?: return null
        return setAx(xtxInv, xty, functions.size, functions.size)
    }

    fun findPolynomialCoefficients(points: List<Vector2d>, dimensions: Int = points.size): DoubleArray? {
        // the same as above, just more efficient
        val size = points.size
        val xt = DoubleArray(size * dimensions)
        for (i in 0 until size) xt[i * dimensions] = 1.0
        for (i in 0 until size) {
            val px = points[i].x
            var j = i * dimensions + 1
            xt[j] = px
            for (pow in 2 until dimensions) {
                val k = j + 1
                xt[k] = px * xt[j]
                j = k
            }
        }
        val y = DoubleArray(size) { points[it].y }
        return solve(xt, y)
    }

    fun evaluatePolynomial(x: Double, polynomial: DoubleArray): Double {
        var sum = 0.0
        var pol = 1.0
        for (p in polynomial) {
            sum += pol * p
            pol *= x
        }
        return sum
    }

    fun computePolynomialError(points: List<Vector2d>, polynomial: DoubleArray): Double {
        return points.sumOf { point ->
            val error = evaluatePolynomial(point.x, polynomial) - point.y
            error * error
        }
    }

    fun findPolynomialCoefficients(y: DoubleArray, dimensions: Int = y.size): DoubleArray? {
        // return solve(points, Array(points.size) { exponent -> { x: Double -> pow(x, exponent.toDouble()) } }.toList())
        // the same as above, just more efficient
        val size = y.size
        val xt = DoubleArray(size * dimensions)
        for (i in 0 until size) xt[i * dimensions] = 1.0
        val half = size.shr(1).toDouble()
        for (i in 0 until size) {
            val px = i - half
            var j = i * dimensions + 1
            xt[j] = px
            for (pow in 2 until dimensions) {
                val k = j + 1
                xt[k] = px * xt[j]
                j = k
            }
        }
        return solve(xt, y)
    }

    fun calcXtX4(X: List<Vector3d>): Matrix4d {
        val m = Matrix4d()
        val a = Vector4d()
        for (i in 0 until 4) {
            var ax = 0.0
            var ay = 0.0
            var az = 0.0
            var aw = 0.0
            for (v in X) {
                val f = v[0]
                ax += f * v.x
                ay += f * v.y
                az += f * v.z
                aw *= f * 1.0
            }
            a.set(ax, ay, az, aw)
            m.setColumn(i, a)
        }
        return m
    }

    // y is 1
    fun calcInvXt4(inv: Matrix4d, X: List<Vector3d>): Vector4d {
        val solution = Vector4d()
        val temp = Vector4d()
        for (v in X) {
            temp.set(v.x, v.y, v.z, 1.0)
            solution.add(inv.transform(temp))
        }
        return solution
    }

    // finds the line, along which the points are lined up
    // x,y,z,1
    fun solve3d(X: List<Vector3d>, regularisation: Double): Vector4d {
        val xtx = calcXtX4(X)
        xtx.m00(xtx.m00() + regularisation)
        xtx.m11(xtx.m11() + regularisation)
        xtx.m22(xtx.m22() + regularisation)
        xtx.m33(xtx.m33() + regularisation)
        xtx.invert()
        return calcInvXt4(xtx, X)
    }

    fun evaluateBlackFrame(vs: DoubleArray, bias: Double = 1.0): Double {
        val deg2 = abs(vs[1] + vs[3] - 2 * vs[2]) + bias
        val deg4 = abs(vs[0] + vs[4] - 4 * (vs[1] + vs[3]) + 6 * vs[2])
        return deg4 / deg2
    }

}