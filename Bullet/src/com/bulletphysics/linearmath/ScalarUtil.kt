package com.bulletphysics.linearmath

import com.bulletphysics.BulletGlobals
import kotlin.math.abs

/**
 * Utility functions for scalars (doubles).
 *
 * @author jezek2
 */
object ScalarUtil {
    @JvmStatic
    fun select(a: Double, b: Double, c: Double): Double {
        return if (a >= 0.0) b else c
    }

    @JvmStatic
    fun atan2Fast(y: Double, x: Double): Double {
        val coeff1 = BulletGlobals.SIMD_PI / 4.0
        val coeff2 = 3.0 * coeff1
        val abs_y = abs(y)
        val angle: Double
        if (x >= 0.0) {
            val r = (x - abs_y) / (x + abs_y)
            angle = coeff1 - coeff1 * r
        } else {
            val r = (x + abs_y) / (abs_y - x)
            angle = coeff2 - coeff1 * r
        }
        return if (y < 0.0) -angle else angle
    }
}
