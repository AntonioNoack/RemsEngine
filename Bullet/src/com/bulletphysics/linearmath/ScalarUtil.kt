package com.bulletphysics.linearmath

import me.anno.maths.Maths.PIf
import kotlin.math.abs

/**
 * Utility functions for scalars (doubles).
 *
 * @author jezek2
 */
object ScalarUtil {
    @JvmStatic
    fun select(a: Float, b: Float, c: Float): Float {
        return if (a >= 0.0) b else c
    }

    /**
     * This is a rough approximation of atan.
     * It is 15x faster than the standard math atan, so it is worth it when accuracy is secondary.
     * */
    @JvmStatic
    fun atan2Fast(y: Float, x: Float): Float {
        val absY = abs(y)
        val angle = if (x >= 0f) {
            val r = (x - absY) / (x + absY)
            ATAN_C1 - ATAN_C1 * r
        } else {
            val r = (x + absY) / (absY - x)
            ATAN_C2 - ATAN_C1 * r
        }
        return if (y < 0f) -angle else angle
    }

    private const val ATAN_C1 = PIf * 0.25f
    private const val ATAN_C2 = ATAN_C1 * 3f
}
