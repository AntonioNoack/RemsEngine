package me.anno.tests.structures

import me.anno.maths.LinearRegression
import me.anno.utils.assertions.assertEquals
import org.joml.Vector2d
import org.junit.jupiter.api.Test

class LinearRegressionTest {
    @Test
    fun test2ndDegree() {
        // test polynomial of 2nd degree
        val deg2 = LinearRegression.findPolynomialCoefficients(
            listOf(
                Vector2d(-1.0, +1.0),
                Vector2d(+0.0, +0.0),
                Vector2d(+1.0, +1.0)
            ), 3, 0.0
        )!!.toList()
        assertEquals(deg2[0], 0.0, 1e-15)
        assertEquals(deg2[1], 0.0, 1e-15)
        assertEquals(deg2[2], 1.0, 1e-15)
    }

    @Test
    fun test4thDegree() {
        // test polynomial of 3rd/4th degree
        val deg4 = LinearRegression.findPolynomialCoefficients(
            listOf(
                Vector2d(-2.0, +0.0),
                Vector2d(-1.0, +1.0),
                Vector2d(+0.0, +0.0),
                Vector2d(+1.0, -1.0),
                Vector2d(+2.0, +0.0),
            ), 5, 0.0
        )!!.toList()
        assertEquals(deg4[0], 0.0, 1e-15)
        assertEquals(deg4[1], -4.0 / 3.0, 1e-15)
        assertEquals(deg4[2], 0.0, 1e-15)
        assertEquals(deg4[3], 1.0 / 3.0, 1e-15)
        assertEquals(deg4[4], 0.0, 1e-15)
    }
}