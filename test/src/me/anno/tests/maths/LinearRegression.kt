package me.anno.tests.maths

import me.anno.maths.LinearRegression
import me.anno.maths.Maths.sq
import org.joml.Vector2d

fun main() {
    // todo the error must be much smaller!!!
    //  -> instead of explicit algorithms, implement iterative ones!
    // todo another way seems to be feature normalization :) -> try that
    for (degree in 3..20) {
        val pts = createPoints(degree)
        val poly = LinearRegression.findPolynomialCoefficients(pts, pts.size, 0.0)!!
        val meanSquareError = meanSquareError(pts, poly)
        println("$degree -> $meanSquareError")
    }
}

fun createPoints(degree: Int): List<Vector2d> {
    val ys = IntArray(degree) { it }
    val xs = IntArray(degree) { 18 - it }
    return xs.withIndex()
        .map { (idx, x) -> Vector2d(x.toDouble(), ys[idx].toDouble()) }
}

fun meanSquareError(pts: List<Vector2d>, poly: DoubleArray): Double {
    return pts.sumOf { pt ->
        sq(pt.y - LinearRegression.evaluatePolynomial(pt.x, poly))
    } / pts.size
}
