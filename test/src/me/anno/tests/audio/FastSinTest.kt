package me.anno.tests.audio

import me.anno.maths.FastSin
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.sq
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sin
import kotlin.math.sqrt

class FastSinTest {

    @Test
    fun testFastSin() {
        assertTrue(runFastSin(10) < 0.07f)
        assertTrue(runFastSin(100) < 6e-4f)
        assertTrue(runFastSin(1000) < 6e-6f)
        assertTrue(runFastSin(10_000) < 9e-7f)
    }

    fun runFastSin(n: Int): Double {
        val dx = TAUf / n
        val s = FastSin()
        var error = 0.0
        val k = n / 10
        var ctr = 0
        repeat(n) { i ->
            val actual = s.next(dx)
            val expected = sin((i + 1) * dx)
            error += sq(expected - actual)
            if (++ctr == k) {
                println("Err[${i + 1}]: ${sqrt((error) / (i + 1))}")
                ctr = 0
            }
        }
        val stdDev = sqrt(error / n)
        println("StdDev: $stdDev")
        return stdDev
    }
}
