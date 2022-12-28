package me.anno.utils.types

import me.anno.maths.Maths
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.FloatBuffer
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
    fun Float.f1() = "%.1f".format(Locale.ENGLISH, this)

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
    fun Double.f1() = "%.1f".format(Locale.ENGLISH, this)

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
    fun Float.f1s() = "% .1f".format(Locale.ENGLISH, this)

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
    fun Double.f1s() = "% .1f".format(Locale.ENGLISH, this)

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

}