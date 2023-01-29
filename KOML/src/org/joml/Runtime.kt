package org.joml

import kotlin.math.abs
import kotlin.math.floor

object Runtime {
    fun f(x: Float) = x.toString()
    fun f(x: Double) = x.toString()
    fun equals(a: Float, b: Float, delta: Float) = abs(a - b) <= delta
    fun equals(a: Double, b: Double, delta: Double) = abs(a - b) <= delta
    fun fract(x: Float) = x - floor(x)
    fun fract(x: Double) = x - floor(x)
}