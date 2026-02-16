package me.anno.tests.maths

import me.anno.maths.Maths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MathsTest {
    @Test
    fun testMixAngleF() {
        val epsilon = 1e-4f
        for (i in -23 until 23) {
            val a = (i * 3.14f / 10f)
            for (j in -23 until 23) {
                val b = (j * 3.14f / 10f)
                val expected = atan2(
                    sin(a) + sin(b),
                    cos(a) + cos(b),
                )
                val actual = Maths.mixAngle(a, b, 0.5f)
                assertEquals(cos(expected), cos(actual), epsilon) {
                    "$a, $b -> $expected vs $actual"
                }
                assertEquals(sin(expected), sin(actual), epsilon) {
                    "$a, $b -> $expected vs $actual"
                }
            }
        }
    }

    @Test
    fun testMixAngle() {
        val epsilon = 1e-13
        for (i in -23 until 23) {
            val a = (i * 3.14 / 10)
            for (j in -23 until 23) {
                val b = (j * 3.14 / 10)
                val expected = atan2(
                    sin(a) + sin(b),
                    cos(a) + cos(b),
                )
                val actual = Maths.mixAngle(a, b, 0.5)
                assertEquals(cos(expected), cos(actual), epsilon) {
                    "$a, $b -> $expected vs $actual"
                }
                assertEquals(sin(expected), sin(actual), epsilon) {
                    "$a, $b -> $expected vs $actual"
                }
            }
        }
    }
}