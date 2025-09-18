package me.anno.maths

import kotlin.math.abs

/**
 * min and max methods with up to 6 parameters,
 * median,
 * */
object MinMax {

    @JvmStatic
    fun min(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    @JvmStatic
    fun min(a: Long, b: Long): Long {
        return if (a < b) a else b
    }

    @JvmStatic
    fun min(a: Float, b: Float): Float {
        return if (a < b) a else b
    }

    @JvmStatic
    fun min(a: Double, b: Double): Double {
        return if (a < b) a else b
    }



    @JvmStatic
    fun min(a: Int, b: Int, c: Int): Int {
        return min(a, min(b, c))
    }

    @JvmStatic
    fun min(a: Long, b: Long, c: Long): Long {
        return min(a, min(b, c))
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        return value
    }

    @JvmStatic
    fun min(a: Double, b: Double, c: Double): Double {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        return value
    }



    @JvmStatic
    fun min(a: Int, b: Int, c: Int, d: Int): Int {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        return value
    }

    @JvmStatic
    fun min(a: Long, b: Long, c: Long, d: Long): Long {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        return value
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float, d: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        return value
    }

    @JvmStatic
    fun min(a: Double, b: Double, c: Double, d: Double): Double {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        return value
    }



    @JvmStatic
    fun min(a: Int, b: Int, c: Int, d: Int, e: Int): Int {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        return value
    }

    @JvmStatic
    fun min(a: Long, b: Long, c: Long, d: Long, e: Long): Long {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        return value
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float, d: Float, e: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        return value
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        value = if (value < f) value else f
        return value
    }



    @JvmStatic
    fun max(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    @JvmStatic
    fun max(a: Long, b: Long): Long {
        return if (a > b) a else b
    }

    @JvmStatic
    fun max(a: Float, b: Float): Float {
        return if (a > b) a else b
    }

    @JvmStatic
    fun max(a: Double, b: Double): Double {
        return if (a > b) a else b
    }



    @JvmStatic
    fun max(a: Int, b: Int, c: Int): Int {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }

    @JvmStatic
    fun max(a: Long, b: Long, c: Long): Long {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }



    @JvmStatic
    fun max(a: Int, b: Int, c: Int, d: Int): Int {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }

    @JvmStatic
    fun max(a: Long, b: Long, c: Long, d: Long): Long {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float, d: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double, d: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }



    @JvmStatic
    fun max(a: Int, b: Int, c: Int, d: Int, e: Int): Int {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }

    @JvmStatic
    fun max(a: Long, b: Long, c: Long, d: Long, e: Long): Long {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float, d: Float, e: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double, d: Double, e: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }



    @JvmStatic
    fun max(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int): Int {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }

    @JvmStatic
    fun max(a: Long, b: Long, c: Long, d: Long, e: Long, f: Long): Long {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }



    @JvmStatic
    fun absAbsMax(a: Float, b: Float) = max(abs(a), abs(b))

    @JvmStatic
    fun absAbsMax(a: Double, b: Double) = max(abs(a), abs(b))

    @JvmStatic
    fun absAbsMax(a: Float, b: Float, c: Float) = max(abs(a), abs(b), abs(c))

    @JvmStatic
    fun absAbsMax(a: Double, b: Double, c: Double) = max(abs(a), abs(b), abs(c))

    @JvmStatic
    fun absAbsMax(a: Float, b: Float, c: Float, d: Float) = max(abs(a), abs(b), abs(c), abs(d))

    @JvmStatic
    fun absAbsMax(a: Double, b: Double, c: Double, d: Double) = max(abs(a), abs(b), abs(c), abs(d))



    @JvmStatic
    fun median(a: Int, b: Int, c: Int): Int =
        max(min(a, b), min(max(a, b), c))

    @JvmStatic
    fun median(a: Long, b: Long, c: Long) =
        max(min(a, b), min(max(a, b), c))

    @JvmStatic
    fun median(a: Float, b: Float, c: Float) =
        max(min(a, b), min(max(a, b), c))

    @JvmStatic
    fun median(a: Double, b: Double, c: Double) =
        max(min(a, b), min(max(a, b), c))


}