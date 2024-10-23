package me.anno.tests.maths

import me.anno.maths.EquationSolver
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.sqrt
import kotlin.random.Random

class EquationSolverTest {

    private fun equals1(a: Float, b: Float): Boolean {
        return abs(a - b) < 1e-4f
    }

    @Test
    fun testQuadraticTwoSolutions() {
        val dst = FloatArray(2)
        val random = Random(1234)
        for (i in 0 until 20) {
            val a = (random.nextFloat() - 0.5f) * 20f
            val x0 = (random.nextFloat() - 0.5f) * 20f
            val x1 = (random.nextFloat() - 0.5f) * 20f
            val b = -a * (x0 + x1)
            val c = a * x0 * x1
            assertEquals(2, EquationSolver.solveQuadratic(dst, a, b, c))
            assertNotEquals(dst[0], dst[1])
            assertTrue(equals1(dst[0], x0) || equals1(dst[1], x0))
            assertTrue(equals1(dst[0], x1) || equals1(dst[1], x1))
        }
    }

    @Test
    fun testQuadraticOneSolution() {
        val dst = FloatArray(2)
        val random = Random(1234)
        var ctr = 0
        for (i in 0 until 20) {
            val a = (random.nextFloat() - 0.5f) * 20f
            val x0 = (random.nextFloat() - 0.5f) * 20f
            val b = -a * (x0 + x0)
            val c = a * x0 * x0
            val div = b * b - 4 * a * c
            if (div != 0f) continue // skipping sample because of precision issues
            assertEquals(1, EquationSolver.solveQuadratic(dst, a, b, c))
            assertEquals(dst[0], x0, 1e-5f)
            ctr++
        }
        println("tested $ctr/20")
        assertTrue(ctr > 10)
    }

    @Test
    fun testQuadraticNoSolution() {
        val dst = FloatArray(2)
        val random = Random(1234)
        for (i in 0 until 20) {
            val a = (random.nextFloat() - 0.5f) * 20f
            val x0 = (random.nextFloat() - 0.5f) * 20f
            val x1 = (random.nextFloat() - 0.5f) * 20f
            val b = -a * (x0 + x1)
            var c = a * x0 * x1
            val div = b * b - 4 * a * c
            c += div / (4f * a) * (1f + random.nextFloat())
            assertEquals(0, EquationSolver.solveQuadratic(dst, a, b, c))
        }
    }

    @Test
    fun testCubicThreeSolutions() {
        val dst = FloatArray(3)
        val random = Random(1234)
        for (i in 0 until 20) {
            val a = (random.nextFloat() - 0.5f) * 20f
            val x0 = (random.nextFloat() - 0.5f) * 20f
            val x1 = (random.nextFloat() - 0.5f) * 20f
            val x2 = (random.nextFloat() - 0.5f) * 20f
            val b = -a * (x0 + x1 + x2)
            val c = a * (x0 * x1 + x1 * x2 + x2 * x0)
            val d = -a * x0 * x1 * x2
            assertEquals(3, EquationSolver.solveCubic(dst, a, b, c, d))
            assertNotEquals(dst[0], dst[1])
            assertNotEquals(dst[1], dst[2])
            assertNotEquals(dst[2], dst[0])
            assertTrue(equals1(dst[0], x0) || equals1(dst[1], x0) || equals1(dst[2], x0))
            assertTrue(equals1(dst[0], x1) || equals1(dst[1], x1) || equals1(dst[2], x1))
            assertTrue(equals1(dst[0], x2) || equals1(dst[1], x2) || equals1(dst[2], x2))
        }
    }

    @Test
    fun testCubicTwoSolutions() {
        val dst = FloatArray(3)
        val random = Random(1234)
        var ctr = 0
        for (i in 0 until 1000) {
            val a = (random.nextFloat() - 0.5f) * 2f
            val x0 = (random.nextFloat() - 0.5f) * 5f
            val x1 = (random.nextFloat() - 0.5f) * 5f
            val b = -a * (x0 + x1 + x1)
            val c = a * (x0 * x1 + x1 * x1 + x1 * x0)
            val d = -a * x0 * x1 * x1

            // precision is a big issue for solving cubic equations
            if (hasThreeSolutions(b / a, c / a, d / a) || hasOneSolution(b / a, c / a, d / a)) {
                continue
            }

            assertEquals(2, EquationSolver.solveCubic(dst, a, b, c, d))
            assertNotEquals(dst[0], dst[1])
            assertTrue(equals1(dst[0], x0) || equals1(dst[1], x0))
            assertTrue(equals1(dst[0], x1) || equals1(dst[1], x1))
            ctr++
        }
        println("tested $ctr/1000")
        assertTrue(ctr > 10)
    }

    @Test
    fun testCubicOneSolution() {
        val dst = FloatArray(3)
        val random = Random(1234)
        var ctr = 0
        for (i in 0 until 20) {
            val a = (random.nextFloat() - 0.5f) * 20f
            val x0 = (random.nextFloat() - 0.5f) * 20f
            val b = -3f * a * (x0)
            val c = 3f * a * (x0 * x0)
            val d = -a * x0 * x0 * x0

            // precision is a big issue for solving cubic equations
            if (!hasOneSolution(b / a, c / a, d / a)) {
                continue
            }

            assertEquals(1, EquationSolver.solveCubic(dst, a, b, c, d))
            assertEquals(dst[0], x0, 0.1f)
            ctr++
        }
        println("tested $ctr/20")
        assertTrue(ctr > 10)
    }

    private fun hasThreeSolutions(a0: Float, b: Float, c: Float): Boolean {
        val a = a0
        val a2 = a * a
        val q = (a2 - 3 * b) / 9
        val r = (a * (2 * a2 - 9 * b) + 27 * c) / 54
        val r2 = r * r
        val q3 = q * q * q
        return r2 < q3
    }

    private fun hasOneSolution(a: Float, b: Float, c: Float): Boolean {
        val a2 = a * a
        val q = (a2 - 3 * b) / 9
        val r = (a * (2 * a2 - 9 * b) + 27 * c) / 54
        val r2 = r * r
        val q3 = q * q * q
        if (r2 < q3) return false
        var a3 = -cbrt(abs(r) + sqrt(r2 - q3))
        if (r < 0) a3 = -a3
        val b3 = if (a3 == 0f) 0f else q / a3
        val cond = +0.5f * sqrt(3f) * (a3 - b3)
        return !(abs(cond) < 1e-14)
    }
}