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
 * These algorithms are not cryptically-secure.
 * If you need the same algorithm as Java or Kotlin by default, just implement it yourself (or ask nicely).
 * */
object RandomBySeed {

    const val GENERATED_BITS = 48
    const val MASK: Long = (1L shl GENERATED_BITS) - 1

    // 48 bits is the best we can do for our magic
    // this magic was especially chosen for its great non-period properties
    // we also choose the same values as Java, so you could replace Random(seed).nextLong() with getRandomLong(seed)
    // other function won't work as easily, because I've replaced them with faster methods

    private const val MULTIPLIER: Long = 0x5DEECE66DL
    private const val ADDEND: Long = 0xBL
    private const val MASK_DOUBLE = MASK.toDouble()

    private const val FLOAT_MANTISSA_BITS = 23
    private const val DOUBLE_MANTISSA_BITS = 52

    /**
     * Probability is a value between 0 and 1.
     * If the value is 0, the event never happens.
     * If the value is 1, the event always happens.
     * */
    fun getRandomBool(seed: Long, probability: Float): Boolean {
        // this should be fine for probabilities from 1 downTo 1e-12
        val seed1 = getNextSeed(initialMix(seed))
        return seed1 < probability * MASK_DOUBLE
    }

    fun getRandomInt(seed: Long): Int {
        return getRandomBits32(seed)
    }

    fun getRandomFloat(seed: Long): Float {
        // create float [1,2) from bits, then subtract one
        val random = getNextSeed(initialMix(seed))
        return rawRandomFloat(random, 127 shl FLOAT_MANTISSA_BITS) - 1f
    }

    fun getRandomDouble(seed: Long): Double {
        // not fully random, but should be good enough
        // create double [1,2) from bits, then subtract one
        val random = getNextSeed(initialMix(seed))
        return rawRandomDouble(random, 1023L shl DOUBLE_MANTISSA_BITS) - 1.0
    }

    fun rawRandomFloat(random: Long, mask: Int): Float {
        val mantissa = random.shr(GENERATED_BITS - FLOAT_MANTISSA_BITS).toInt()
        return Float.fromBits(mantissa or mask)
    }

    fun rawRandomDouble(random: Long, mask: Long): Double {
        val mantissa = random.shl(DOUBLE_MANTISSA_BITS - GENERATED_BITS)
        return Double.fromBits(mantissa or mask)
    }

    fun getRandomLong(seed: Long): Long {
        val seed0 = initialMix(seed)
        val seed1 = getNextSeed(seed0)
        val seed2 = getNextSeed(seed1)
        return seed1.shl(32) xor seed2
    }

    fun getRandomInt(seed: Long, min: Int, maxExcl: Int): Int {
        val numValues = maxExcl - min
        if (numValues < 1) return getRandomInt(seed)

        var seedI = initialMix(seed)
        val numBits = 32 - (numValues - 1).countLeadingZeroBits()
        while (true) {
            seedI = getNextSeed(seedI)
            val value = getRandomBitsFromSeed(seedI, numBits)
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

    fun getRandomGaussianF(seed: Long): Float {
        var seed2 = initialMix(seed)
        val mask = 128 shl FLOAT_MANTISSA_BITS // [2,4)
        val offset = 3f
        while (true) {
            val seed1 = getNextSeed(seed2)
            seed2 = getNextSeed(seed1)

            val x = rawRandomFloat(seed1, mask) - offset
            val y = rawRandomFloat(seed2, mask) - offset

            val s = x * x + y * y
            if (s >= 1f || s == 0f) continue

            return x * sqrt(-2f * ln(s) / s)
        }
    }

    fun getRandomGaussian(seed: Long): Double {
        var seed2 = initialMix(seed)
        val offset = 3.0
        val mask = 1024L shl DOUBLE_MANTISSA_BITS // 1023 -> [1,2), 1024 -> [2,4)
        while (true) {
            val seed1 = getNextSeed(seed2)
            seed2 = getNextSeed(seed1)

            // 2 .. 4 -> -1 .. 1
            val x = rawRandomDouble(seed1, mask) - offset
            val y = rawRandomDouble(seed2, mask) - offset

            val s = x * x + y * y
            if (s >= 1.0 || s == 0.0) continue

            return x * sqrt(-2 * ln(s) / s)
        }
    }

    fun initialMix(seed: Long): Long {
        // xor is not good for seed = 0..n, so use multiplier instead
        return (seed * MULTIPLIER).and(MASK)
    }

    fun getNextSeed(seed: Long): Long {
        return (seed * MULTIPLIER + ADDEND).and(MASK)
    }

    fun getRandomBits32(seed: Long): Int {
        return getNextSeed(initialMix(seed)).toInt()
    }

    fun getRandomBitsFromSeed(seed: Long, bits: Int): Int {
        return (seed ushr (48 - bits)).toInt()
    }
}