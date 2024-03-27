package me.anno.tests.maths

import me.anno.maths.LinearRegression
import me.anno.maths.Maths.sq
import org.joml.Vector2d

fun main() {
    // todo the error must be much smaller!!!
    for (degree in 3..20) {
        val ys = IntArray(degree) { it }
        val xs = IntArray(degree) { 18 - it }
        val pts = xs.withIndex()
            .map { (idx, x) -> Vector2d(x.toDouble(), ys[idx].toDouble()) }
            .toMutableList()
        val poly = LinearRegression.findPolynomialCoefficients(pts, pts.size, 0.0)!!
        val mse = pts.sumOf { pt ->
            sq(pt.y - LinearRegression.evaluatePolynomial(pt.x, poly))
        } / pts.size
        println("$degree -> $mse")
    }
}