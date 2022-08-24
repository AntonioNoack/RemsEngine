package org.joml

import java.text.NumberFormat
import kotlin.math.abs

object Runtime {
    fun format(x: Float, f: NumberFormat?) = x.toString()
    fun format(x: Double, f: NumberFormat?) = x.toString()
    fun format(x: Float, f: Int) = x.toString()
    fun format(x: Double, f: Int) = x.toString()
    fun equals(a: Float, b: Float, delta: Float) = abs(a-b) <= delta
    fun equals(a: Double, b: Double, delta: Double) = abs(a-b) <= delta
}