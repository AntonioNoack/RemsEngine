package me.anno.tests.maths

import me.anno.maths.LinearRegression
import org.joml.Vector2d

fun main() {
    // todo the error must be much smaller!!!
    val ys = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    val xs = intArrayOf(18, 17, 16, 15, 14, 13, 12, 11, 10, 9)
    val pts = xs.withIndex()
        .map { (idx, x) -> Vector2d(x.toDouble(), ys[idx].toDouble()) }
        .toMutableList()
    val poly = LinearRegression.findPolynomialCoefficients(pts)!!
    for (pt in pts) {
        println("${pt.x}: ${pt.y} =?= ${LinearRegression.evaluatePolynomial(pt.x, poly)}")
    }
}