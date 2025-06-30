package speiger.primitivecollections

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * From https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin.
 * */
@Suppress("unused")
object HashUtil {
    const val DEFAULT_MIN_CAPACITY: Int = 16
    const val DEFAULT_MIN_CONCURRENCY: Int = 4
    const val DEFAULT_LOAD_FACTOR: Float = 0.75f
    const val FAST_LOAD_FACTOR: Float = 0.5f
    const val FASTER_LOAD_FACTOR: Float = 0.25f
    private val INT_PHI = -1640531527
    private const val INV_INT_PHI = 340573321

    @JvmStatic
    fun mix(x: Int): Int {
        val h = x * -1640531527
        return h xor (h ushr 16)
    }

    @JvmStatic
    fun invMix(x: Int): Int {
        return (x xor (x ushr 16)) * 340573321
    }

    @JvmStatic
    fun nextPowerOfTwo(x: Int): Int {
        return 1 shl 32 - Integer.numberOfLeadingZeros(x - 1)
    }

    @JvmStatic
    fun nextPowerOfTwo(x: Long): Long {
        return 1L shl 64 - java.lang.Long.numberOfLeadingZeros(x - 1L)
    }

    @JvmStatic
    fun getRequiredBits(value: Int): Int {
        return Integer.bitCount(nextPowerOfTwo(value + 1) - 1)
    }

    @JvmStatic
    fun getRequiredBits(value: Long): Int {
        return java.lang.Long.bitCount(nextPowerOfTwo(value + 1L) - 1L)
    }

    @JvmStatic
    fun arraySize(size: Int, loadFactor: Float): Int {
        return min(
            1073741824L,
            max(2L, nextPowerOfTwo(ceil((size.toFloat() / loadFactor).toDouble()).toLong()))
        ).toInt()
    }
}