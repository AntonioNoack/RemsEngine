package me.anno.fonts.signeddistfields.algorithm

import me.anno.utils.maths.Maths.pow
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sqrt

object EquationSolver {

    const val TOO_LARGE_RATIO = 1e9 // idk...
    const val M_PI = Math.PI.toFloat()

    fun solveQuadratic(x: FloatArray, a: Float, b: Float, c: Float): Int {
        // a = 0 -> linear equation
        if (a == 0f || abs(b) + abs(c) > TOO_LARGE_RATIO * abs(a)) {
            // a, b = 0 -> no solution
            if (b == 0f || abs(c) > TOO_LARGE_RATIO * abs(b)) {
                return if (c == 0f) -1 else 0 // 0 = 0
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
        } else if (dscr == 0f) {
            x[0] = -b / (2 * a)
            1
        } else 0
    }

    fun solveCubicNormed(x: FloatArray, a0: Float, b: Float, c: Float): Int {
        var a = a0
        val a2 = a * a
        var q = (a2 - 3 * b) / 9
        val r = (a * (2 * a2 - 9 * b) + 27 * c) / 54
        val r2 = r * r
        val q3 = q * q * q
        var A: Float
        val B: Float
        return if (r2 < q3) {
            var t = r / sqrt(q3)
            if (t < -1) t = -1f
            if (t > 1) t = 1f
            t = acos(t)
            a /= 3
            q = -2 * sqrt(q)
            x[0] = q * cos(t / 3) - a
            x[1] = q * cos((t + 2 * M_PI) / 3) - a
            x[2] = q * cos((t - 2 * M_PI) / 3) - a
            3
        } else {
            A = -pow(abs(r) + sqrt(r2 - q3), 1f / 3f)
            if (r < 0) A = -A
            B = if (A == 0f) 0f else q / A
            a /= 3
            x[0] = A + B - a
            x[1] = -0.5f * (A + B) - a
            x[2] = +0.5f * sqrt(3f) * (A - B)
            if (abs(x[2]) < 1e-14) 2 else 1
        }
    }

    fun solveCubic(x: FloatArray, a: Float, b: Float, c: Float, d: Float): Int {
        if (a != 0f) {
            val bn = b / a
            val cn = c / a
            val dn: Float = d / a
            // Check that a isn't "almost zero"
            if (abs(bn) < TOO_LARGE_RATIO && abs(cn) < TOO_LARGE_RATIO && abs(dn) < TOO_LARGE_RATIO)
                return solveCubicNormed(x, bn, cn, dn)
        }
        return solveQuadratic(x, b, c, d)
    }

}