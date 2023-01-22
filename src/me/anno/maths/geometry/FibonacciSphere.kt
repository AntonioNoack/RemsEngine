package me.anno.maths.geometry

import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FibonacciSphere {

    val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // golden angle in radians

    /**
     * creates a sphere with N regularly positioned points
     * https://stackoverflow.com/a/26127012/4979303
     * */
    fun create(n: Int): Array<Vector3f> {
        val div = 2f / (n - 1f)
        return Array(n) { i ->
            val y = 1f - i * div  // y goes from 1 to -1
            val radius = sqrt(1f - y * y)  // radius at y

            val theta = goldenAngle * i // golden angle increment

            val x = cos(theta) * radius
            val z = sin(theta) * radius

            Vector3f(x, y, z)
        }
    }

    fun create2(n: Int): FloatArray {

        // https://stackoverflow.com/a/26127012/4979303

        // to do alternative, which also creates the faces?
        // http://extremelearning.com.au/evenly-distributing-points-on-a-sphere/

        val points = FloatArray(3 * n)

        val div = 2f / (n - 1f)
        var j = 0
        for (i in 0 until n) {

            val y = 1f - i * div  // y goes from 1 to -1
            val radius = sqrt(1f - y * y)  // radius at y

            val theta = goldenAngle * i // golden angle increment

            val x = cos(theta) * radius
            val z = sin(theta) * radius

            points[j++] = x
            points[j++] = y
            points[j++] = z

        }

        return points

    }

}