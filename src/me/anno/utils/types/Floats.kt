package me.anno.utils.types

import me.anno.maths.Maths
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.FloatBuffer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.PI

object Floats {

    @JvmStatic
    fun FloatBuffer.put3(v: Vector2f) {
        put(v.x)
        put(v.y)
    }

    @JvmStatic
    fun FloatBuffer.put3(v: Vector3f) {
        put(v.x)
        put(v.y)
        put(v.z)
    }

    @JvmStatic
    fun FloatBuffer.put3(v: Vector4f) {
        put(v.x)
        put(v.y)
        put(v.z)
        put(v.w)
    }

    private const val piF180f = (PI / 180).toFloat()
    private const val x180fPif = (180 / PI).toFloat()

    const val piF180d = PI / 180.0
    private const val x180fPid = 180.0 / PI

    @JvmStatic
    fun Int.toDegrees() = this * x180fPif

    @JvmStatic
    fun Int.toRadians() = this * piF180f

    @JvmStatic
    fun Float.toDegrees() = this * x180fPif

    @JvmStatic
    fun Float.toRadians() = this * piF180f

    @JvmStatic
    fun Double.toDegrees() = this * x180fPid

    @JvmStatic
    fun Double.toRadians() = this * piF180d

    @JvmStatic
    fun Float.f6() = "%.6f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f5() = "%.5f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f4() = "%.4f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f3() = "%.3f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f2() = "%.2f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f1(): String = f1.format(this.toDouble())

    @JvmStatic
    fun Double.f6() = "%.6f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f5() = "%.5f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f4() = "%.4f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f3() = "%.3f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f2() = "%.2f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f1(): String = f1.format(this)

    @JvmStatic
    fun Float.f6s() = "% .6f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f5s() = "% .5f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f4s() = "% .4f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f3s() = "% .3f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f2s() = "% .2f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Float.f2x() = "% .2f".format(Locale.ENGLISH, this).replace("-0.00", " 0.00")

    @JvmStatic
    fun Float.f1s(): String = f1s.format(toDouble())

    @JvmStatic
    fun Double.f6s() = "% .6f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f5s() = "% .5f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f4s() = "% .4f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f3s() = "% .3f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f2s() = "% .2f".format(Locale.ENGLISH, this)

    @JvmStatic
    fun Double.f1s(): String = f1s.format(this)

    @JvmStatic
    private val f1Symbols = DecimalFormatSymbols(Locale.ENGLISH)

    @JvmStatic
    private val f1 = DecimalFormat("0.0", f1Symbols).apply { maximumFractionDigits = 1 }

    @JvmStatic
    private val f1s = DecimalFormat(" 0.0;-0.0", f1Symbols).apply { maximumFractionDigits = 1 }

    @JvmStatic
    fun formatPercent(progress: Int, total: Int) = (progress.toDouble() / total.toDouble()).formatPercent()

    @JvmStatic
    fun formatPercent(progress: Long, total: Long) = (progress.toDouble() / total.toDouble()).formatPercent()

    @JvmStatic
    fun Float.formatPercent() = toDouble().formatPercent()

    @JvmStatic
    fun Double.formatPercent() = Maths.clamp(this * 100.0, 0.0, 100.0).f1()

    @JvmStatic
    infix fun ClosedFloatingPointRange<Float>.step(step: Float): Iterator<Float> {
        return object : Iterator<Float> {
            var next = start
            override fun hasNext(): Boolean = next < endInclusive && (next + step) > next
            override fun next(): Float {
                val n = next
                next += step
                return n
            }
        }
    }

    // by x4u on https://stackoverflow.com/a/6162687/4979303
    @JvmStatic
    fun float16ToFloat32(bits: Int): Float {
        var mant = bits and 0x03ff // 10 bits mantissa
        var exp = bits and 0x7c00 // 5 bits exponent
        if (exp == 0x7c00) exp = 0x3fc00 // NaN/Inf
        else if (exp != 0) {// normalized value
            exp += 0x1c000 // exp - 15 + 127
            if (mant == 0 && exp > 0x1c400) // smooth transition
                return Float.fromBits((bits and 0x8000).shl(16) or (exp shl 13) or 0x3ff)
        } else if (mant != 0) {// && exp==0 -> subnormal
            exp = 0x1c400 // make it normal
            do {
                mant = mant shl 1 // mantissa * 2
                exp -= 0x400 // decrease exp by 1
            } while (mant and 0x400 == 0) // while not normal
            mant = mant and 0x3ff // discard subnormal bit
        } // else +/-0 -> +/-0
        return Float.fromBits( // combine all parts
            (bits and 0x8000).shl(16) // sign  << ( 31 - 15 )
                    or ((exp or mant) shl 13)  // value << ( 23 - 10 )
        )
    }

    @Suppress("unused")
    @JvmStatic
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
            sign or ((fp32 and 0x7fffff or 0x800000) +// add subnormal bit
                    (0x800000.ushr(v - 102)) // round depending on cut off
                    ushr (126 - v)) // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
        }
    }

}