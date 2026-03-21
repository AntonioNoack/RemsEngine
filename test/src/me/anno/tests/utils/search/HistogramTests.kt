package me.anno.tests.utils.search

import me.anno.utils.assertions.assertEquals
import me.anno.utils.search.Histogram.getHistogramIndex
import me.anno.utils.search.Histogram.getHistogramIndexI
import org.junit.jupiter.api.Test

class HistogramTests {

    @Test
    fun testEqualDistribution() {
        for (size in intArrayOf(3, 5, 6, 11)) {
            val values = IntArray(size) { 10 }
            val scale = size * 0.1f
            for (i in 0..10) {
                val percentile = i / 10f
                val expected = scale * i
                assertEquals(scale * i, getHistogramIndex(values, percentile), 1e-6f)
                assertEquals(expected.toInt(), getHistogramIndexI(values, percentile))
            }
        }
    }

    @Test
    fun testSkewedInterpolation() {
        val bins = 20
        val values = IntArray(bins)
        val fractions = listOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f)
        val n = 1000_000
        var prev = 0f
        for (i in 0 until bins - 1) {
            for (frac in fractions) {
                val k = (frac * n).toInt()
                values.fill(0)
                values[i] = n - k
                values[i + 1] = k

                val expectedFraction =
                    if (frac <= 0.5f) 1f - (0.5f - frac) / (1f - frac)
                    else (2f - 0.5f / frac)

                val expectedI = i + expectedFraction
                assertEquals(expectedI, getHistogramIndex(values, 0.5f), 1e-6f)

                check(expectedI > prev) { "Expected strictly monotonous increase" }
                prev = expectedI
            }
        }
    }
}