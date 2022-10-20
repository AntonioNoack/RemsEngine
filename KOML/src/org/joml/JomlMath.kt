package org.joml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.sqrt

object JomlMath {

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
                if (Character.isDigit(c) && eIndex == i - 1) {
                    res.append('+')
                }
            }
            res.append(c)
        }
        return res.toString()
    }

    fun clamp(x: Float, min: Float, max: Float) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Double, min: Double, max: Double) = if (x < min) min else if (x < max) x else max

    fun invsqrt(x: Float) = 1f / sqrt(x)
    fun invsqrt(x: Double) = 1.0 / sqrt(x)

    /** a*b+c */
    fun fma(a: Float, b: Float, c: Float) = a * b + c

    /** a*b+c */
    fun fma(a: Double, b: Double, c: Double) = a * b + c

    // for the transition away from fma()
    fun fma(a: Float, b: Float) = a + b
    fun fma(a: Double, b: Double) = a + b
    fun fma(a: Float) = a
    fun fma(a: Double) = a

    // can be replaced in the future
    fun isFinite(x: Float) = x.isFinite()
    fun isFinite(x: Double) = x.isFinite()
    fun safeAsin(x: Float) = asin(clamp(x, -1f, +1f))
    fun safeAsin(x: Double) = asin(clamp(x, -1.0, +1.0))
    fun safeAcos(x: Float) = acos(clamp(x, -1f, +1f))
    fun safeAcos(x: Double) = acos(clamp(x, -1.0, +1.0))
    fun absEqualsOne(x: Float) = abs(x) == 1f
    fun absEqualsOne(x: Double) = abs(x) == 1.0

}