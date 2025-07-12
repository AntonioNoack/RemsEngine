package speiger.primitivecollections

import me.anno.utils.types.Floats.toIntOr
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
        return 1.shl(32 - (x - 1).countLeadingZeroBits())
    }

    @JvmStatic
    fun nextPowerOfTwo(x: Long): Long {
        return 1L.shl(64 - (x - 1L).countLeadingZeroBits())
    }

    @JvmStatic
    fun getRequiredBits(value: Int): Int {
        return (nextPowerOfTwo(value + 1) - 1).countOneBits()
    }

    @JvmStatic
    fun getRequiredBits(value: Long): Int {
        return (nextPowerOfTwo(value + 1L) - 1L).countOneBits()
    }

    @JvmStatic
    fun arraySize(size: Int, loadFactor: Float): Int {
        val v0 = initialSize(size, loadFactor)
        val v1 = max(2L, nextPowerOfTwo(v0.toLong()))
        return min(1L shl 30, v1).toInt()
    }

    fun initialSize(usedSize: Int, loadFactor: Float = DEFAULT_LOAD_FACTOR): Int {
        val rawValue = ceil(max(usedSize, 0) / loadFactor)
        return max(min(rawValue, 2e9f).toIntOr(), 4)
    }
}