package org.joml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
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
    fun invsqrt(x: Float): Float = 1f / sqrt(x)

    @JvmStatic
    fun invsqrt(x: Double): Double = 1.0 / sqrt(x)

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
}