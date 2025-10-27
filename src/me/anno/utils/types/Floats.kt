package me.anno.utils.types

import me.anno.maths.Maths
import me.anno.utils.types.NumberFormatter.formatFloat
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object Floats {

    private const val piF180f = (PI / 180).toFloat()
    private const val x180fPif = (180 / PI).toFloat()

    const val piF180d = PI / 180.0
    private const val x180fPid = 180.0 / PI

    @JvmStatic
    fun Float.toDegrees(): Float = this * x180fPif

    @JvmStatic
    fun Float.toRadians(): Float = this * piF180f

    @JvmStatic
    fun Double.toDegrees(): Double = this * x180fPid

    @JvmStatic
    fun Double.toRadians(): Double = this * piF180d

    @JvmStatic
    fun Float.f6(): String = formatFloat(toDouble(), 6, false)

    @JvmStatic
    fun Float.f5(): String = formatFloat(toDouble(), 5, false)

    @JvmStatic
    fun Float.f4(): String = formatFloat(toDouble(), 4, false)

    @JvmStatic
    fun Float.f3(): String = formatFloat(toDouble(), 3, false)

    @JvmStatic
    fun Float.f2(): String = formatFloat(toDouble(), 2, false)

    @JvmStatic
    fun Float.f1(): String = formatFloat(toDouble(), 1, false)

    @JvmStatic
    fun Double.f6(): String = formatFloat(this, 6, false)

    @JvmStatic
    fun Double.f5(): String = formatFloat(this, 5, false)

    @JvmStatic
    fun Double.f4(): String = formatFloat(this, 4, false)

    @JvmStatic
    fun Double.f3(): String = formatFloat(this, 3, false)

    @JvmStatic
    fun Double.f2(): String = formatFloat(this, 2, false)

    @JvmStatic
    fun Double.f1(): String = formatFloat(this, 1, false)

    @JvmStatic
    fun Float.f6s(): String = formatFloat(toDouble(), 6, true)

    @JvmStatic
    fun Float.f5s(): String = formatFloat(toDouble(), 5, true)

    @JvmStatic
    fun Float.f4s(): String = formatFloat(toDouble(), 4, true)

    @JvmStatic
    fun Float.f3s(): String = formatFloat(toDouble(), 3, true)

    @JvmStatic
    fun Float.f2s(): String = formatFloat(toDouble(), 2, true)

    @JvmStatic
    fun Float.f2x(): String = f2s().replace("-0.00", " 0.00")

    @JvmStatic
    fun Float.f1s(): String = formatFloat(toDouble(), 1, true)

    @JvmStatic
    fun Double.f6s(): String = formatFloat(this, 6, true)

    @JvmStatic
    fun Double.f5s(): String = formatFloat(this, 5, true)

    @JvmStatic
    fun Double.f4s(): String = formatFloat(this, 4, true)

    @JvmStatic
    fun Double.f3s(): String = formatFloat(this, 3, true)

    @JvmStatic
    fun Double.f2s(): String = formatFloat(this, 2, true)

    @JvmStatic
    fun Double.f1s(): String = formatFloat(this, 1, true)

    @JvmStatic
    fun formatPercent(progress: Int, total: Int): String = formatPercent(progress.toLong(), total.toLong())

    @JvmStatic
    fun formatPercent(progress: Long, total: Long): String = (progress.toDouble() / total.toDouble()).formatPercent()

    @JvmStatic
    fun Float.formatPercent(): String = toDouble().formatPercent()

    @JvmStatic
    fun Double.formatPercent(): String = Maths.clamp(this * 100.0, 0.0, 100.0).f1()

    @JvmStatic
    infix fun ClosedFloatingPointRange<Float>.step(step: Float): Iterator<Float> {
        return object : Iterator<Float> {
            var nextNumber = start
            override fun hasNext(): Boolean = nextNumber < endInclusive && (nextNumber + step) > nextNumber
            override fun next(): Float {
                val n = nextNumber
                nextNumber += step
                return n
            }
        }
    }

    @JvmStatic
    fun Float.roundToIntOr(ifNaN: Int = 0): Int {
        return if (isNaN()) ifNaN
        else roundToInt()
    }

    @JvmStatic
    fun Float.toIntOr(ifNaN: Int = 0): Int {
        return if (isNaN()) ifNaN
        else toInt()
    }

    @JvmStatic
    fun Double.roundToIntOr(ifNaN: Int = 0): Int {
        return if (isNaN()) ifNaN
        else roundToInt()
    }

    @JvmStatic
    fun Double.toIntOr(ifNaN: Int = 0): Int {
        return if (isNaN()) ifNaN
        else toInt()
    }

    @JvmStatic
    fun Float.roundToLongOr(ifNaN: Long = 0): Long {
        return if (isNaN()) ifNaN
        else roundToLong()
    }

    @JvmStatic
    fun Float.toLongOr(ifNaN: Long = 0): Long {
        return if (isNaN()) ifNaN
        else toLong()
    }

    @JvmStatic
    fun Double.roundToLongOr(ifNaN: Long = 0): Long {
        return if (isNaN()) ifNaN
        else roundToLong()
    }

    @JvmStatic
    fun Double.toLongOr(ifNaN: Long = 0): Long {
        return if (isNaN()) ifNaN
        else toLong()
    }

    @JvmStatic
    fun Float.toHalf() = float32ToFloat16(this)

    @JvmStatic
    fun Int.fromHalf() = float16ToFloat32(this)

    /**
     * Smallest, non-zero, positive value, 5.9604645E-8
     * */
    const val FP16_MIN_VALUE = 1

    /**
     * Largest, finite positive value, 65504
     * */
    const val FP16_MAX_VALUE = 0x7bff

    /**
     * +Inf for fp16
     * */
    const val FP16_POSITIVE_INFINITY = 0x7c00

    /**
     * -Inf for fp16
     * */
    const val FP16_NEGATIVE_INFINITY = 0xfc00

    // by x4u on https://stackoverflow.com/a/6162687/4979303
    @JvmStatic
    fun float16ToFloat32(bits: Int): Float {
        var mantissa = bits and 0x03ff // 10 bits mantissa
        var exponent = bits and 0x7c00 // 5 bits exponent
        if (exponent == 0x7c00) {
            exponent = 0x3fc00 // NaN/Inf
        } else if (exponent != 0) {// normalized value
            exponent += 0x1c000 // exp - 15 + 127
            if (mantissa == 0 && exponent > 0x1c400) {// smooth transition
                return Float.fromBits((bits and 0x8000).shl(16) or (exponent shl 13) or 0x3ff)
            }
        } else if (mantissa != 0) {// && exp==0 -> subnormal
            exponent = 0x1c400 // make it normal
            do {
                mantissa = mantissa shl 1 // mantissa * 2
                exponent -= 0x400 // decrease exp by 1
            } while (mantissa and 0x400 == 0) // while not normal
            mantissa = mantissa and 0x3ff // discard subnormal bit
        } // else +/-0 -> +/-0
        // combine all parts
        val sign = (bits and 0x8000).shl(16) // sign << (31 - 15)
        val value = ((exponent or mantissa) shl 13) // value << ( 23 - 10 )
        return Float.fromBits(sign or value)
    }

    @JvmStatic
    @Suppress("unused")
    fun float32ToFloat16(value: Float): Int {
        val fp32 = value.toRawBits()
        val sign = (fp32.ushr(16)).and(0x8000) // sign only
        var v = (fp32 and 0x7fffffff) + 0x1000 // rounded value
        return if (v >= 0x47800000) { // might be or become NaN/Inf; avoid Inf due to rounding
            if (fp32 and 0x7fffffff >= 0x47800000) { // is or must become NaN/Inf
                if (v < 0x7f800000) sign.or(0x7c00) // remains +/-Inf or NaN
                else sign.or(0x7c00).or(fp32.and(0x007fffff).ushr(13)) // make it +/-Inf
                // keep NaN (and Inf) bits
            } else sign.or(0x7bff) // unrounded not quite Inf
        } else if (v >= 0x38800000) { // remains normalized value
            sign or (v - 0x38000000 ushr 13) // exp - 127 + 15
        } else if (v < 0x33000000) { // too small for subnormal
            sign // becomes +/-0
        } else {
            v = fp32 and 0x7fffffff ushr 23 // tmp exp for subnormal calc
            sign or (((fp32 and 0x7fffff or 0x800000) +// add subnormal bit
                    (0x800000.ushr(v - 102))) // round depending on cut off
                .ushr(126 - v)) // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
        }
    }
}