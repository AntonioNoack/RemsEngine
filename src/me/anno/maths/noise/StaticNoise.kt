package me.anno.maths.noise

import me.anno.maths.Packing.pack64
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Create noise values from a seed without creating any instance.
 * This is good for having a base-seed, property-seed (static, final), and then using their xor value for values.
 *
 * This is the same algorithm as java.util.Random, and not cryptically-secure.
 * */
object StaticNoise {

    // 48 bits is the best we can do for our magic
    // this magic was especially chosen for its great non-period properties
    // we also choose the same values as Java, so you could replace Random(seed).nextLong() with getRandomLong(seed)
    private const val MULTIPLIER: Long = 0x5DEECE66DL
    private const val ADDEND: Long = 0xBL
    private const val INV_FLOAT = 1f / (1L shl 24)
    private const val INV_DOUBLE = 1.0 / (1L shl 53)
    const val MASK: Long = (1L shl 48) - 1

    fun getRandomBool(seed: Long, probability: Float = 0.5f): Boolean {
        // fix this for small probabilities?
        //   issues only occur for events with probability ~ 1e-6
        return if (probability < 0.0001f) {
            // now issues only appear with 1e-15 probability (never)
            getRandomDouble(seed) < probability
        } else {
            getRandomFloat(seed) < probability
        }
    }

    fun getRandomInt(seed: Long): Int {
        return getRandomBits(seed, 32)
    }

    fun getRandomFloat(seed: Long): Float {
        return getRandomBits(seed, 24) * INV_FLOAT
    }

    fun getRandomDouble(seed: Long): Double {
        val seed0 = initialMix(seed)
        val seed1 = getNextSeed(seed0)
        val seed2 = getNextSeed(seed1)
        return createDoubleFromSeeds(seed1, seed2)
    }

    private fun createDoubleFromSeeds(seed1: Long, seed2: Long): Double {
        return createDoubleLongFromSeeds(seed1, seed2) * INV_DOUBLE
    }

    private fun createDoubleLongFromSeeds(seed1: Long, seed2: Long): Long {
        val high = getRandomBitsFromSeed(seed1, 26)
        val low = getRandomBitsFromSeed(seed2, 27)
        return ((high.toLong() shl 27) + low)
    }

    fun getRandomLong(seed: Long): Long {
        val seed0 = initialMix(seed)
        val seed1 = getNextSeed(seed0)
        val seed2 = getNextSeed(seed1)
        val high = getRandomBitsFromSeed(seed1, 32)
        val low = getRandomBitsFromSeed(seed2, 32)
        return pack64(high, low)
    }

    fun getRandomInt(seed: Long, min: Int, maxExcl: Int): Int {
        val numValues = maxExcl - min
        if (numValues < 1) return getRandomInt(seed)

        var seed = initialMix(seed)
        val numBits = 32 - (numValues - 1).countLeadingZeroBits()
        while (true) {
            seed = getNextSeed(seed)
            val value = getRandomBitsFromSeed(seed, numBits)
            if (value < numValues) return min + value
        }
    }

    fun getRandomLong(seed: Long, min: Long, maxExcl: Long): Long {
        val numValues = maxExcl - min
        if (numValues < 1L) return getRandomLong(seed)

        var seedL = initialMix(seed)
        val numBits = 64 - (numValues - 1).countLeadingZeroBits()
        val numBitsHigh = max(numBits - 32, 0)
        val numBitsLow = min(numBits, 32)
        val needsHighBits = numBitsHigh > 0

        while (true) {
            val seedH = if (needsHighBits) getNextSeed(seedL) else seedL
            seedL = getNextSeed(seedH)

            val high = getRandomBitsFromSeed(seedH, numBitsHigh)
            val low = getRandomBitsFromSeed(seedL, numBitsLow)
            val value = pack64(high, low)
            if (value < numValues) return min + value
        }
    }

    fun getRandomGaussian(seed: Long): Double {
        var seed0 = initialMix(seed)
        val toDoubleX2 = INV_DOUBLE * 2.0
        while (true) {
            val seed3 = getNextSeed(seed0)
            val seed2 = getNextSeed(seed3)
            val seed1 = getNextSeed(seed2)
            seed0 = getNextSeed(seed1)

            val v1 = toDoubleX2 * createDoubleLongFromSeeds(seed3, seed2) - 1.0
            val v2 = toDoubleX2 * createDoubleLongFromSeeds(seed1, seed0) - 1.0
            val s = v1 * v1 + v2 * v2
            if (s >= 1.0 || s == 0.0) continue

            return v1 * sqrt(-2 * ln(s) / s)
        }
    }

    fun getRandomGaussianF(seed: Long): Float {
        var seed0 = initialMix(seed)
        val toFloatX2 = 2f / (1L shl 24)
        while (true) {
            val seed1 = getNextSeed(seed0)
            seed0 = getNextSeed(seed1)

            val v1 = toFloatX2 * getRandomBitsFromSeed(seed1, 24) - 1f
            val v2 = toFloatX2 * getRandomBitsFromSeed(seed0, 24) - 1f
            val s = v1 * v1 + v2 * v2
            if (s >= 1f || s == 0f) continue

            return v1 * sqrt(-2f * ln(s) / s)
        }
    }

    fun initialMix(seed: Long): Long {
        return (seed xor MULTIPLIER).and(MASK)
    }

    fun getNextSeed(seed: Long): Long {
        return (seed * MULTIPLIER + ADDEND).and(MASK)
    }

    fun getRandomBits(seed: Long, bits: Int): Int {
        val nextSeed = getNextSeed(initialMix(seed))
        return getRandomBitsFromSeed(nextSeed, bits)
    }

    fun getRandomBitsFromSeed(seed: Long, bits: Int): Int {
        return (seed ushr (48 - bits)).toInt()
    }
}