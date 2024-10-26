package me.anno.tests.maths.noise

import me.anno.maths.noise.PerlinNoise
import me.anno.tests.LOGGER
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

class PerlinNoiseTest {

    fun inc(history: IntArray, v: Float) {
        history[min((v * history.size).toInt(), history.lastIndex)]++
    }

    @Test
    fun testPerlinDistribution1d() {
        val noise = PerlinNoise(1234, 12, 0.5f, 0f, 1f)
        val history = IntArray(10)
        for (x in 0 until 10000) {
            inc(history, noise[x.toFloat()])
        }
        checkExpected(history)
    }

    @Test
    fun testPerlinDistribution2d() {
        val noise = PerlinNoise(1234, 12, 0.5f, 0f, 1f)
        val history = IntArray(10)
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                inc(history, noise[x.toFloat(), y.toFloat()])
            }
        }
        checkExpected(history)
    }

    @Test
    fun testPerlinDistribution3d() {
        val noise = PerlinNoise(1234, 12, 0.5f, 0f, 1f)
        val history = IntArray(10)
        for (z in 0 until 21) {
            for (y in 0 until 22) {
                for (x in 0 until 22) {
                    inc(history, noise[x.toFloat(), y.toFloat(), z.toFloat()])
                }
            }
        }
        checkExpected(history)
    }

    @Test
    fun testPerlinDistribution4d() {
        val noise = PerlinNoise(1234, 12, 0.5f, 0f, 1f)
        val history = IntArray(10)
        for (w in 0 until 10) {
            for (z in 0 until 10) {
                for (y in 0 until 10) {
                    for (x in 0 until 10) {
                        inc(history, noise[x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()])
                    }
                }
            }
        }
        checkExpected(history, 3.7)
    }

    private fun checkExpected(history: IntArray, v: Double = 3.5) {
        LOGGER.info("Actual History: ${history.toList()}")
        val expectedCurve = history.indices.map { index ->
            val x = (index - history.lastIndex * 0.5) * v / history.size
            exp(-x * x)
        }
        val scale = history.sum() / expectedCurve.sum()
        val expectedCurve1 = expectedCurve.map { it * scale }
        LOGGER.info("Expected History: ${expectedCurve1.map { it.toInt() }.toList()}")
        assertTrue(history.withIndex().count { (index, histValue) ->
            val expected = expectedCurve1[index]
            abs(histValue - expected) < 250
        } == history.size)
    }
}