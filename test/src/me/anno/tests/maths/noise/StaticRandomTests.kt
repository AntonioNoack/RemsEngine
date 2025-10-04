package me.anno.tests.maths.noise

import me.anno.maths.Maths.sq
import me.anno.maths.noise.StaticNoise
import me.anno.maths.noise.StaticNoise.getRandomBool
import me.anno.maths.noise.StaticNoise.getRandomDouble
import me.anno.maths.noise.StaticNoise.getRandomFloat
import me.anno.maths.noise.StaticNoise.getRandomInt
import me.anno.maths.noise.StaticNoise.getRandomLong
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class StaticRandomTests {
    @Test
    fun testRandomNoise() {
        val n = 10_000
        val random = Random(18646)
        val values = DoubleArray(n) {
            val seed = random.nextLong()
            StaticNoise.getRandomGaussian(seed)
        }

        val mean = values.sum() / n
        val variance = values.sumOf { sq(it - mean) } / n

        val bucket1 = values.count { abs(it) < 1.0 }
        val bucket2 = values.count { abs(it) < 2.0 }
        val bucket3 = values.count { abs(it) < 3.0 }

        println("mean,var: $mean,$variance, b1/2/3: $bucket1,$bucket2,$bucket3")

        assertEquals(0.0, mean, 0.002)
        assertEquals(1.0, variance, 0.02)

        assertEquals(6827, bucket1, 80)
        assertEquals(9550, bucket2, 30)
        assertEquals(9973, bucket3, 10)
    }

    @Test
    fun testRandomNoiseF() {
        val n = 10_000
        val random = Random(18646)
        val values = DoubleArray(n) {
            val seed = random.nextLong()
            StaticNoise.getRandomGaussianF(seed).toDouble()
        }

        val mean = values.sum() / n
        val variance = values.sumOf { sq(it - mean) } / n

        val bucket1 = values.count { abs(it) < 1.0 }
        val bucket2 = values.count { abs(it) < 2.0 }
        val bucket3 = values.count { abs(it) < 3.0 }

        println("mean,var: $mean,$variance, b1/2/3: $bucket1,$bucket2,$bucket3")

        assertEquals(0.0, mean, 0.02)
        assertEquals(1.0, variance, 0.02)

        // https://en.wikipedia.org/wiki/Normal_distribution
        assertEquals(6827, bucket1, 70)
        assertEquals(9550, bucket2, 30)
        assertEquals(9973, bucket3, 10)
    }

    @Test
    fun testRandomInt() {
        val numSamples = 1000
        val min = 3
        val random = Random(18646)
        for (max in min + 1 until 20) {
            val numBuckets = max - min
            val buckets = IntArray(numBuckets)
            repeat(numSamples) {
                val seed = random.nextLong()
                buckets[getRandomInt(seed, min, max) - min]++
            }
            if (numBuckets >= 2) {
                val stdDev = sqrt((numBuckets - 1f) * numSamples) / numBuckets
                val avg = numSamples.toFloat() / numBuckets
                val deviations = buckets.map { (it - avg) / stdDev }
                assertTrue(deviations.all { dev -> abs(dev) < 3f })
                // println(deviations)
            }
        }
    }

    @Test
    fun testRandomLong() {
        val numSamples = 1000
        val min = 3L
        val random = Random(18646)
        for (max in min + 1 until 20) {
            val numBuckets = (max - min).toInt()
            val buckets = IntArray(numBuckets)
            repeat(numSamples) {
                val seed = random.nextLong()
                buckets[(getRandomLong(seed, min, max) - min).toInt()]++
            }
            if (numBuckets >= 2) {
                val stdDev = sqrt((numBuckets - 1f) * numSamples) / numBuckets
                val avg = numSamples.toFloat() / numBuckets
                val deviations = buckets.map { (it - avg) / stdDev }
                assertTrue(deviations.all { dev -> abs(dev) < 3f })
                // println(deviations)
            }
        }
    }

    @Test
    fun testRandomFloat() {
        val numSamples = 10000
        val random = Random(18646)

        val numBuckets = 200
        val buckets = IntArray(numBuckets)
        repeat(numSamples) {
            val seed = random.nextLong()
            buckets[(getRandomFloat(seed) * numBuckets).toInt()]++
        }

        val stdDev = sqrt((numBuckets - 1f) * numSamples) / numBuckets
        val avg = numSamples.toFloat() / numBuckets
        val deviations = buckets.map { (it - avg) / stdDev }
        assertTrue(deviations.all { dev -> abs(dev) < 3f })
        // println(deviations)
    }

    @Test
    fun testRandomDouble() {
        val numSamples = 10000
        val random = Random(18646)

        val numBuckets = 200
        val buckets = IntArray(numBuckets)
        repeat(numSamples) {
            val seed = random.nextLong()
            buckets[(getRandomDouble(seed) * numBuckets).toInt()]++
        }

        val stdDev = sqrt((numBuckets - 1f) * numSamples) / numBuckets
        val avg = numSamples.toFloat() / numBuckets
        val deviations = buckets.map { (it - avg) / stdDev }
        assertTrue(deviations.all { dev -> abs(dev) < 3f })
        // println(deviations)
    }

    @Test
    fun testRandomBoolean() {
        val numSamples = 10000
        val random = Random(18646)

        val p = 0.3f
        val numBuckets = 2
        val buckets = IntArray(2)
        repeat(numSamples) {
            val seed = random.nextLong()
            buckets[getRandomBool(seed, p).toInt()]++
        }

        val stdDev = sqrt((numBuckets - 1f) * numSamples) / numBuckets // correct???
        val avg = listOf((1f - p) * numSamples, p * numSamples)
        val deviations = buckets.mapIndexed { idx, it -> (it - avg[idx]) / stdDev }
        assertTrue(deviations.all { dev -> abs(dev) < 3f })
        println(deviations)
    }
}