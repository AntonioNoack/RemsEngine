package me.anno.tests.maths.noise

import me.anno.maths.noise.PerlinNoise
import me.anno.tests.LOGGER
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

class PerlinNoiseTest {
    @Test
    fun testPerlinDistribution() {
        val noise = PerlinNoise(1234, 12, 0.5f, 0f, 1f)
        val history = IntArray(10)
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                val hi = min((noise[x.toFloat(), y.toFloat()] * history.size).toInt(), history.lastIndex)
                history[hi]++
            }
        }
        LOGGER.info("Actual History: ${history.toList()}")
        val expectedCurve = history.indices.map { index ->
            val x = (index - history.lastIndex * 0.5) * 3.5 / history.size
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