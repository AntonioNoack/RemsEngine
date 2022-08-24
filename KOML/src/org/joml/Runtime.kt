package org.joml

import kotlin.math.abs

object Runtime {
    fun f(x: Float) = x.toString()
    fun f(x: Double) = x.toString()
    fun equals(a: Float, b: Float, delta: Float) = abs(a - b) <= delta
    fun equals(a: Double, b: Double, delta: Double) = abs(a - b) <= delta
}