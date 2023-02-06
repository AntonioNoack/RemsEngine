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
    fun Any.anyToDouble(): Double {
        return when (this) {
            is Int -> this.toDouble()
            is Long -> this.toDouble()
            is Float -> this.toDouble()
            is Double -> this
            else -> throw RuntimeException()
        }
    }

    @JvmStatic
    fun Any.anyToFloat(): Float {
        return when (this) {
            is Int -> this.toFloat()
            is Long -> this.toFloat()
            is Float -> this
            is Double -> this.toFloat()
            else -> throw RuntimeException()
        }
    }

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
    private val f1 = DecimalFormat("#.0", f1Symbols).apply { maximumFractionDigits = 1 }
    @JvmStatic
    private val f1s = DecimalFormat(" #.0;-#.0", f1Symbols).apply { maximumFractionDigits = 1 }

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
    fun fp16ToFP32(bits: Int): Float {
        var mant = bits and 0x03ff // 10 bits mantissa
        var exp = bits and 0x7c00 // 5 bits exponent
        if (exp == 0x7c00) exp = 0x3fc00 // NaN/Inf
        else if (exp != 0) {// normalized value
            exp += 0x1c000 // exp - 15 + 127
            if (mant == 0 && exp > 0x1c400) // smooth transition
                return java.lang.Float.intBitsToFloat(bits and 0x8000 shl 16 or (exp shl 13) or 0x3ff)
        } else if (mant != 0) {// && exp==0 -> subnormal
            exp = 0x1c400 // make it normal
            do {
                mant = mant shl 1 // mantissa * 2
                exp -= 0x400 // decrease exp by 1
            } while (mant and 0x400 == 0) // while not normal
            mant = mant and 0x3ff // discard subnormal bit
        } // else +/-0 -> +/-0
        return Float.fromBits( // combine all parts
            bits and 0x8000 shl 16 // sign  << ( 31 - 15 )
                    or (exp or mant shl 13)  // value << ( 23 - 10 )
        )
    }

}