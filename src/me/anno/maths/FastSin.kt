package me.anno.maths

import kotlin.math.hypot

/**
 * Fast approximation using Euler's method to generate sin and cos in equal steps around the circle.
 * Needs normalize() every 1000 or 10000 calls, quality gradually degrades.
 *
 * Modifying dx during the iteration is fine.
 * */
class FastSin {

    var sin = 0f
    var cos = 1f

    /**
     * Steps forward by dx along the perimeter.
     * More accurate the smaller dx is.
     * */
    fun next(dx: Float): Float {
        cos -= dx * sin
        sin += dx * cos
        return sin
    }

    fun normalize() {
        val factor = 1f / hypot(sin, cos)
        sin *= factor
        cos *= factor
    }

    fun reset() {
        sin = 0f
        cos = 1f
    }
}
