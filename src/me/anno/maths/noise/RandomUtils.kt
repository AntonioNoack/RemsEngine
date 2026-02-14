package me.anno.maths.noise

import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

object RandomUtils {

    fun Random.nextGaussian(): Double {
        while (true) { // from Java.util.Random
            val x = 2.0 * nextDouble() - 1.0
            val y = 2.0 * nextDouble() - 1.0
            val rSq = x * x + y * y
            if (rSq >= 1.0 || rSq == 0.0) continue
            val multiplier = sqrt(-2.0 * ln(rSq) / rSq)
            return x * multiplier
        }
    }

    fun Random.nextGaussianF(): Float {
        while (true) { // from Java.util.Random
            val x = 2f * nextFloat() - 1f
            val y = 2f * nextFloat() - 1f
            val rSq = x * x + y * y
            if (rSq >= 1f || rSq == 0f) continue
            val multiplier = sqrt(-2f * ln(rSq) / rSq)
            return x * multiplier
        }
    }

    fun Random.addGaussianNoise(dst: FloatArray, dstI: Int, factor: Float) {
        while (true) { // from Java.util.Random
            val x = 2f * nextFloat() - 1f
            val y = 2f * nextFloat() - 1f
            val rSq = x * x + y * y
            if (rSq >= 1f || rSq == 0f) continue
            val multiplier = factor * sqrt(-2f * ln(rSq) / rSq)
            dst[dstI] += x * multiplier
            if (dstI + 1 < dst.size) {
                dst[dstI + 1] += y * multiplier
            }
            return
        }
    }

    fun Random.addGaussianNoise(dst: FloatArray, factor: Float) {
        for (i in dst.indices step 2) {
            addGaussianNoise(dst, i, factor)
        }
    }
}