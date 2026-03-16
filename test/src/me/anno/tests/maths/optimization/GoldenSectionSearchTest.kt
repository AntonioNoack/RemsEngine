package me.anno.tests.maths.optimization

import me.anno.maths.optimization.GoldenSectionSearch.maximizeFunction
import me.anno.maths.optimization.GoldenSectionSearch.minimizeFunction
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.pow

class GoldenSectionSearchTest {

    private val accuracy = 1e-14

    @Test
    fun testQuadraticFunction() {
        val result = minimizeFunction(-10.0, 10.0, accuracy) { x ->
            (x - 2.0).pow(2)
        }

        assertEquals(2.0, result, accuracy)
    }

    @Test
    fun testNonCenteredMinimum() {
        val result = minimizeFunction(0.0, 20.0, accuracy) { x ->
            (x - 7.5).pow(2) + 3
        }

        assertEquals(7.5, result, 1e-7)
    }

    @Test
    fun testNegativeMinimum() {
        val result = minimizeFunction(-10.0, 0.0, accuracy) { x ->
            (x + 4.0).pow(2)
        }

        assertEquals(-4.0, result, accuracy)
    }

    @Test
    fun testMinimumNearBoundary() {
        val result = minimizeFunction(0.0, 1.0, accuracy) { x ->
            (x - 0.1).pow(2)
        }

        assertEquals(0.1, result, accuracy)
    }

    @Test
    fun testAsymmetricFunctionMin() {
        val result = minimizeFunction(-5.0, 10.0, accuracy) { x ->
            (x - 3).pow(2) + abs(x)
        }

        // true minimum approx
        assertEquals(2.5, result, 1e-7)
    }

    @Test
    fun testQuadraticMaximum() {
        val result = maximizeFunction(-10.0, 10.0, accuracy) { x ->
            -(x - 2.0).pow(2)
        }

        assertEquals(2.0, result, accuracy)
    }

    @Test
    fun testNonCenteredMaximum() {
        val result = maximizeFunction(0.0, 20.0, accuracy) { x ->
            -(x - 7.5).pow(2) + 3
        }

        assertEquals(7.5, result, 1e-7)
    }

    @Test
    fun testNegativeMaximum() {
        val result = maximizeFunction(-10.0, 0.0, accuracy) { x ->
            -(x + 4.0).pow(2)
        }

        assertEquals(-4.0, result, accuracy)
    }

    @Test
    fun testMaximumNearBoundary() {
        val result = maximizeFunction(0.0, 1.0, accuracy) { x ->
            -(x - 0.1).pow(2)
        }

        assertEquals(0.1, result, accuracy)
    }

    @Test
    fun testAsymmetricFunctionMax() {
        val result = maximizeFunction(-5.0, 10.0, accuracy) { x ->
            -(x - 3).pow(2) - abs(x)
        }

        // true maximum approx
        assertEquals(2.5, result, 1e-7)
    }
}