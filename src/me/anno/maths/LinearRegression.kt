package me.anno.maths

import me.anno.maths.LinearAlgebra.inverse
import me.anno.maths.LinearAlgebra.setABt
import me.anno.maths.LinearAlgebra.setAtB
import me.anno.maths.LinearAlgebra.setAtX
import me.anno.maths.LinearAlgebra.setAx
import org.joml.Matrix4d
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector4d

@Suppress("unused")
object LinearRegression {

    /**
     * @param x feature matrix, row major, [points x features]
     * @param y solution vector, f(x)
     * @return v such that Xv = y
     * */
    fun solve(x: DoubleArray, y: DoubleArray, regularisation: Double = 1e-3): DoubleArray? {
        val numPts = y.size
        val degree = x.size / numPts
        // b = (XtX)^-1 * Xt * y
        // b = (XtX)^-1 * (Xt * y)
        val xtx = setAtB(x, x, degree, numPts, degree)
        val t = degree + 1
        for (i in 0 until degree) {
            xtx[i * t] += regularisation
        }
        val xty = setAtX(x, y, degree, numPts)
        val xtxInv = inverse(xtx, degree) ?: return null
        return setAx(xtxInv, xty, degree, degree)
    }

    // not tested!
    fun solveT(xTransposed: DoubleArray, y: DoubleArray, regularisation: Double = 1e-3): DoubleArray? {
        val numPts = y.size
        val degree = xTransposed.size / numPts
        // beta = (XtX)^-1 * Xt * Y
        val xtx = setABt(xTransposed, xTransposed, degree, numPts, degree)
        val t = degree + 1
        for (i in 0 until degree) {
            xtx[i * t] += regularisation
        }
        val xty = setAx(xTransposed, y, degree, numPts)
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

    fun solve(
        points: List<Vector2d>,
        functions: List<(Double) -> Double>,
        regularisation: Double = 1e-3
    ): DoubleArray? {
        val xt = crossMapDoubleT(points, functions)
        val y = DoubleArray(points.size) { points[it].y }
        // beta = (XtX)^-1 * Xt * Y
        val xtx = setABt(xt, xt, functions.size, points.size, functions.size)
        val t = functions.size + 1
        for (i in functions.indices) {
            xtx[i * t] += regularisation
        }
        val xty = setAx(xt, y, functions.size, points.size)
        val xtxInv = inverse(xtx, functions.size) ?: return null
        return setAx(xtxInv, xty, functions.size, functions.size)
    }

    /**
     * finds the polynomial coefficients upto degree #dimensions.
     * This falls apart for large #dimensions, probably because of my bad matrix inverter
     * */
    fun findPolynomialCoefficients(
        points: List<Vector2d>,
        numDimensions: Int = points.size,
        regularisation: Double = 1e-14
    ): DoubleArray? {
        // the same as above, just more efficient
        val numPoints = points.size
        val features = DoubleArray(numPoints * numDimensions)
        for (i in 0 until numPoints) features[i * numDimensions] = 1.0
        for (i in 0 until numPoints) {
            val xi = points[i].x
            var j = i * numDimensions + 1
            features[j] = xi
            for (pow in 2 until numDimensions) {
                val k = j + 1
                features[k] = xi * features[j]
                j = k
            }
        }

        val values = DoubleArray(numPoints) { points[it].y }
        return solve(features, values, regularisation)
    }

    fun evaluatePolynomial(x: Double, polynomial: DoubleArray): Double {
        var sum = 0.0
        var pol = 1.0
        for (coeff in polynomial) {
            val term = pol * coeff
            if (term.isFinite()) sum += term
            else sum = term // higher terms have priority
            pol *= x
        }
        return sum
    }

    fun evaluatePolynomial(x: Double, weights: DoubleArray, polynomial: List<(Double) -> Double>): Double {
        var sum = 0.0
        for (i in polynomial.indices) {
            sum += polynomial[i](x) * weights[i]
        }
        return sum
    }

    fun computePolynomialError(points: List<Vector2d>, polynomial: DoubleArray): Double {
        return points.sumOf { point ->
            val error = evaluatePolynomial(point.x, polynomial) - point.y
            error * error
        }
    }

    fun findPolynomialCoefficients(y: DoubleArray, dimensions: Int = y.size, regularisation: Double): DoubleArray? {
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
        return solve(xt, y, regularisation)
    }

    fun calcXtX4(x: List<Vector3d>): Matrix4d {
        val m = Matrix4d()
        val a = Vector4d()
        for (i in 0 until 4) {
            var ax = 0.0
            var ay = 0.0
            var az = 0.0
            var aw = 0.0
            for (v in x) {
                val f = v[0]
                ax += f * v.x
                ay += f * v.y
                az += f * v.z
                aw *= f
            }
            a.set(ax, ay, az, aw)
            m.setColumn(i, a)
        }
        return m
    }

    // y is 1
    fun calcInvXt4(inv: Matrix4d, x: List<Vector3d>): Vector4d {
        val solution = Vector4d()
        val temp = Vector4d()
        for (v in x) {
            temp.set(v.x, v.y, v.z, 1.0)
            solution.add(inv.transform(temp))
        }
        return solution
    }

    /**
     * finds the line, along which the points are lined up
     * x, y, z, 1
     * */
    fun solve3d(x: List<Vector3d>, regularisation: Double): Vector4d {
        val xtx = calcXtX4(x)
        xtx._m00(xtx.m00 + regularisation)
        xtx._m11(xtx.m11 + regularisation)
        xtx._m22(xtx.m22 + regularisation)
        xtx._m33(xtx.m33 + regularisation)
        xtx.determineProperties()
        xtx.invert()
        return calcInvXt4(xtx, x)
    }
}