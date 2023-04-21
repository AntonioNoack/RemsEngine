package org.joml

import kotlin.math.abs
import kotlin.math.floor

object Runtime {
    fun f(x: Float): String = x.toString()
    fun f(x: Double): String = x.toString()
    fun equals(a: Float, b: Float, delta: Float): Boolean = abs(a - b) <= delta
    fun equals(a: Double, b: Double, delta: Double): Boolean = abs(a - b) <= delta
    fun fract(x: Float): Float = x - floor(x)
    fun fract(x: Double): Double = x - floor(x)
}