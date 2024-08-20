package me.anno.tests.structures

import me.anno.maths.Maths.sq
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.weightedRandomIndex
import me.anno.utils.structures.maps.LazyMap
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.random.Random

class WeightedRandomTest {
    @Test
    fun test() {
        val samples = 10_000
        val values = listOf(1, 2, 3, 4, 5)
        val weights = LazyMap { key: Int -> sq(key.toDouble()) }
        val random = Random(1234)
        val histogram = IntArray(values.size)
        for (i in 0 until samples) {
            val j = values.weightedRandomIndex(random, weights::get)
            histogram[j]++
        }
        val sum = values.map(weights::get).sum()
        val expected = values.map { samples * weights[it] / sum }
        for (i in values.indices) {
            val exp = expected[i]
            val probability = weights[values[i]] / sum
            val variance = samples * probability * (1f - probability)
            val twoSigma = sqrt(variance) * 2f
            assertTrue(histogram[i].toDouble() in exp - twoSigma..exp + twoSigma)
        }
    }
}