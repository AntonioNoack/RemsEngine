package org.joml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.sqrt

object JomlMath {

    @JvmStatic
    fun String.addSigns(): String {
        val res = StringBuilder()
        var eIndex = Int.MIN_VALUE
        for (i in indices) {
            val c = this[i]
            if (c == 'E') {
                eIndex = i
            } else {
                if (c == ' ' && eIndex == i - 1) {
                    res.append('+')
                    continue
                }
                if (c in '0'..'9' && eIndex == i - 1) {
                    res.append('+')
                }
            }
            res.append(c)
        }
        return res.toString()
    }

    @JvmStatic
    fun clamp(x: Float, min: Float, max: Float): Float = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Double, min: Double, max: Double): Double = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun invsqrt(x: Float): Float = 1f / max(sqrt(x), 1e-38f)

    @JvmStatic
    fun invsqrt(x: Double): Double = 1.0 / max(sqrt(x), 1e-308)

    @JvmStatic
    fun invLength(x: Float, y: Float, z: Float): Float = invsqrt(x * x + y * y + z * z)

    @JvmStatic
    fun invLength(x: Double, y: Double, z: Double): Double = invsqrt(x * x + y * y + z * z)

    // can be replaced in the future
    @JvmStatic
    fun isFinite(x: Float): Boolean = x.isFinite()

    @JvmStatic
    fun isFinite(x: Double): Boolean = x.isFinite()

    @JvmStatic
    fun safeAsin(x: Float): Float = asin(clamp(x, -1f, +1f))

    @JvmStatic
    fun safeAsin(x: Double): Double = asin(clamp(x, -1.0, +1.0))

    @JvmStatic
    fun safeAcos(x: Float): Float = acos(clamp(x, -1f, +1f))

    @JvmStatic
    fun safeAcos(x: Double): Double = acos(clamp(x, -1.0, +1.0))

    @JvmStatic
    fun absEqualsOne(x: Float): Boolean = abs(x) == 1f

    @JvmStatic
    fun absEqualsOne(x: Double): Boolean = abs(x) == 1.0

    @JvmStatic
    fun hash(d: Double): Int {
        val tmp = d.toRawBits()
        return tmp.toInt() xor (tmp shr 32).toInt()
    }

    @JvmStatic
    fun satAdd(a: Int, b: Int): Int {
        val tmp = a.toLong() + b.toLong()
        return clamp(tmp, Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    @JvmStatic
    fun satSub(a: Int, b: Int): Int {
        val tmp = a.toLong() - b.toLong()
        return clamp(tmp, Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    @JvmStatic
    private fun clamp(x: Long, min: Long, max: Long): Long {
        return if (x < min) min else if (x > max) max else x
    }

    @JvmStatic
    fun smoothStep(x: Float, vx: Float, t: Float, t2: Float, t3: Float): Float {
        return (x + x - vx - vx) * t3 + 3f * (vx - x) * t2 + x * t + x
    }

    @JvmStatic
    fun smoothStep(x: Double, vx: Double, t: Double, t2: Double, t3: Double): Double {
        return (x + x - vx - vx) * t3 + 3f * (vx - x) * t2 + x * t + x
    }

    @JvmStatic
    fun mix(x: Float, ox: Float, t: Float): Float {
        return (ox - x) * t + x
    }

    @JvmStatic
    fun mix(x: Double, ox: Double, t: Double): Double {
        return (ox - x) * t + x
    }

    @JvmStatic
    fun hermite(x: Float, t0: Float, v1: Float, t1: Float, t: Float, t2: Float, t3: Float): Float {
        return (x + x - v1 - v1 + t1 + t0) * t3 + (3f * v1 - 3f * x - t0 - t0 - t1) * t2 + x * t + x
    }

    @JvmStatic
    fun hermite(x: Double, t0: Double, v1: Double, t1: Double, t: Double, t2: Double, t3: Double): Double {
        return (x + x - v1 - v1 + t1 + t0) * t3 + (3.0 * v1 - 3.0 * x - t0 - t0 - t1) * t2 + x * t + x
    }
}