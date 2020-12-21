package me.anno.fonts.signeddistfields.algorithm

import java.lang.StrictMath.pow
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sqrt

object EquationSolver {

    const val TOO_LARGE_RATIO = 1e9 // idk...
    const val M_PI = Math.PI

    fun solveQuadratic(x: DoubleArray, a: Double, b: Double, c: Double): Int {
        // a = 0 -> linear equation
        if (a == 0.0 || abs(b) + abs(c) > TOO_LARGE_RATIO * abs(a)) {
            // a, b = 0 -> no solution
            if (b == 0.0 || abs(c) > TOO_LARGE_RATIO * abs(b)) {
                return if (c == 0.0) -1 else 0 // 0 = 0
            }
            x[0] = -c / b
            return 1
        }
        var dscr = b * b - 4 * a * c
        return if (dscr > 0) {
            dscr = sqrt(dscr)
            x[0] = (-b + dscr) / (2 * a)
            x[1] = (-b - dscr) / (2 * a)
            2
        } else if (dscr == 0.0) {
            x[0] = -b / (2 * a)
            1
        } else 0
    }

    fun solveCubicNormed(x: DoubleArray, a0: Double, b: Double, c: Double): Int {
        var a = a0
        val a2 = a * a
        var q = (a2 - 3 * b) / 9
        val r = (a * (2 * a2 - 9 * b) + 27 * c) / 54
        val r2 = r * r
        val q3 = q * q * q
        var A: Double
        val B: Double
        return if (r2 < q3) {
            var t = r / sqrt(q3)
            if (t < -1) t = -1.0
            if (t > 1) t = 1.0
            t = acos(t)
            a /= 3
            q = -2 * sqrt(q)
            x[0] = q * cos(t / 3) - a
            x[1] = q * cos((t + 2 * M_PI) / 3) - a
            x[2] = q * cos((t - 2 * M_PI) / 3) - a
            3
        } else {
            A = -pow(abs(r) + sqrt(r2 - q3), 1.0 / 3.0)
            if (r < 0) A = -A
            B = if (A == 0.0) 0.0 else q / A
            a /= 3
            x[0] = A + B - a
            x[1] = -0.5 * (A + B) - a
            x[2] = 0.5 * sqrt(3.0) * (A - B)
            if (abs(x[2]) < 1e-14) 2 else 1
        }
    }

    fun solveCubic(x: DoubleArray, a: Double, b: Double, c: Double, d: Double): Int {
        if (a != 0.0) {
            val bn = b / a
            val cn = c / a
            val dn: Double = d / a
            // Check that a isn't "almost zero"
            if (abs(bn) < TOO_LARGE_RATIO && abs(cn) < TOO_LARGE_RATIO && abs(dn) < TOO_LARGE_RATIO)
                return solveCubicNormed(x, bn, cn, dn)
        }
        return solveQuadratic(x, b, c, d)
    }

}