package me.anno.tests

import me.anno.image.ImageWriter
import me.anno.maths.LinearRegression.findPolynomialCoefficients
import me.anno.utils.types.Floats.f2
import me.anno.utils.types.Floats.f5
import org.joml.Matrix2f
import org.joml.Vector2d
import org.joml.Vector2f

fun main() {

    /**
     * - foliage can be animated by shearing, but that makes the straws longer
     * - foliage can be rotated piece by piece, but I believe this still makes the straws longer
     * -> find what relationship there is, how I can compute dy from dx, depending on position along the straw
     *
     * for moderate angles,
     * x(t) ~       0.500 * t              - 0.048 * t³                 | linear curve, then decreasing gradient
     * y(t) ~ 1.0             - 0.169 * t²                              | quadratic looking curve
     * y(x) ~ 1.0 - 0.056 * x - 1.189 * x² + 1.682 * t³ - 2.115 * t^4   | slightly offset circle begin
     * */

    fun testCurvature(steps: Int, stepSize: Float, stepAngle: Float): Vector2f {
        val mat = Matrix2f()
        mat.rotate(-stepAngle)
        val position = Vector2f()
        for (i in 0 until steps) {
            position.add(0f, stepSize)
            position.mul(mat)
        }
        return position
    }

    fun testCurvature2(steps: Int, size: Float, angle: Float) =
        testCurvature(steps, size / steps, angle / steps)

    val steps = 1000
    val length = 1f
    val xy = ArrayList<Vector2f>()
    val tx = ArrayList<Vector2f>()
    val ty = ArrayList<Vector2f>()

    for (i in 0..314/2) {
        val angle = i / 100f
        val sol = testCurvature2(steps, length, angle)
        println("${angle.f2()} ${sol.x.f5()} ${sol.y.f5()}")
        xy.add(sol)
        tx.add(Vector2f(angle, sol.x))
        ty.add(Vector2f(angle, sol.y))
    }

    // find the formulas x(t), y(t), y(x) using linear regression
    println(findPolynomialCoefficients(tx.map { Vector2d(it) }, 5)?.joinToString())
    println(findPolynomialCoefficients(ty.map { Vector2d(it) }, 5)?.joinToString())
    println(findPolynomialCoefficients(xy.map { Vector2d(it) }, 5)?.joinToString())

    // draw x and y as a relation to angle, as well as y in relation to x
    val size = 512
    val th = 5
    ImageWriter.writeImageCurve(size, size, true, 0, -1, th, xy, "foliage/xy.png")
    ImageWriter.writeImageCurve(size, size, true, 0, -1, th, tx, "foliage/tx.png")
    ImageWriter.writeImageCurve(size, size, true, 0, -1, th, ty, "foliage/ty.png")

}