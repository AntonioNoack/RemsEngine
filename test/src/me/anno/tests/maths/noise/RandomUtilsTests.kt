package me.anno.tests.maths.noise

import me.anno.maths.Maths.sq
import me.anno.maths.noise.RandomUtils.addGaussianNoise
import me.anno.maths.noise.RandomUtils.nextGaussian
import me.anno.maths.noise.RandomUtils.nextGaussianF
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random

class RandomUtilsTests {
    @Test
    fun testRandomNoise() {
        val n = 10_000
        val random = Random(18646)
        val values = DoubleArray(n) { random.nextGaussian() }

        val mean = values.sum() / n
        val variance = values.sumOf { sq(it - mean) } / n

        val bucket1 = values.count { abs(it) < 1.0 }
        val bucket2 = values.count { abs(it) < 2.0 }
        val bucket3 = values.count { abs(it) < 3.0 }

        println("mean,var: $mean,$variance, b1/2/3: $bucket1,$bucket2,$bucket3")

        assertEquals(0.0, mean, 0.002)
        assertEquals(1.0, variance, 0.001)

        assertEquals(6827, bucket1, 70)
        assertEquals(9550, bucket2, 30)
        assertEquals(9973, bucket3, 10)
    }

    @Test
    fun testRandomNoiseF() {
        val n = 10_000
        val random = Random(18646)
        val values = DoubleArray(n) { random.nextGaussianF().toDouble() }

        val mean = values.sum() / n
        val variance = values.sumOf { sq(it - mean) } / n

        val bucket1 = values.count { abs(it) < 1.0 }
        val bucket2 = values.count { abs(it) < 2.0 }
        val bucket3 = values.count { abs(it) < 3.0 }

        println("mean,var: $mean,$variance, b1/2/3: $bucket1,$bucket2,$bucket3")

        assertEquals(0.0, mean, 0.01)
        assertEquals(1.0, variance, 0.01)

        // https://en.wikipedia.org/wiki/Normal_distribution
        assertEquals(6827, bucket1, 70)
        assertEquals(9550, bucket2, 30)
        assertEquals(9973, bucket3, 10)
    }

    @Test
    fun testAddRandomNoiseF() {
        val n = 10_000
        val random = Random(18646)
        val tmp = FloatArray(2)
        val values = DoubleArray(n) {
            tmp.fill(0f)
            random.addGaussianNoise(tmp, 1f)
            (if (it.hasFlag(1)) tmp[1] else tmp[0]).toDouble()
        }

        val mean = values.sum() / n
        val variance = values.sumOf { sq(it - mean) } / n

        val bucket1 = values.count { abs(it) < 1.0 }
        val bucket2 = values.count { abs(it) < 2.0 }
        val bucket3 = values.count { abs(it) < 3.0 }

        println("mean,var: $mean,$variance, b1/2/3: $bucket1,$bucket2,$bucket3")

        assertEquals(0.0, mean, 0.02)
        assertEquals(1.0, variance, 0.03)

        assertEquals(6827, bucket1, 70)
        assertEquals(9550, bucket2, 30)
        assertEquals(9973, bucket3, 10)
    }
}