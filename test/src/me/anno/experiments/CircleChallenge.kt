package me.anno.experiments

import me.anno.maths.Optimization
import kotlin.math.abs
import kotlin.math.hypot

// a challenge posted in a discord channel
// the outer circle has a radius of 2
// the left side has a half circle (radius 1),
// calculate the center of the third circle,
// which is touching the half and outer circle
// |---_
// |  \  _\
// |   |/ \|
// |__/_\_/|

data class Circle(val x: Double, val y: Double, val r: Double) {
    fun distanceToCenter(other: Circle): Double {
        val dx = x - other.x
        val dy = y - other.y
        return hypot(dx, dy)
    }
    fun gapInside(other: Circle): Double {
        return r - (distanceToCenter(other) + other.r)
    }
    fun gapOutside(other: Circle): Double {
        // traditional case
        return distanceToCenter(other) - (r + other.r)
    }
}

// define error function
fun error(x: Double, y: Double): Double {
    val outer = Circle(0.0, 0.0, 2.0)
    val left = Circle(0.0, 1.0, 1.0)
    val c3 = Circle(x, y, y)
    return abs(outer.gapInside(c3)) + abs(left.gapOutside(c3))
}

fun main() {

    // solve
    val (err, xy) = Optimization.simplexAlgorithm(
        doubleArrayOf(1.5, 0.5), 0.1,
        0.0, 512
    ) { (x, y) ->
        error(x, y)
    }

    val (x, y) = xy
    println("Solution: ($x,$y), error: $err")
}