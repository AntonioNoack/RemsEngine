package me.anno.utils.maths.geometry

import me.anno.utils.types.Vectors.print
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FibonacciSphere {

    /**
     * creates a sphere with N regularly positioned points
     * */
    fun create(n: Int): Array<Vector3f> {

        // https://stackoverflow.com/a/26127012/4979303

        val points = Array(n) { Vector3f() }
        val phi = (PI * (3.0 - sqrt(5.0))).toFloat() // golden angle in radians

        val div = 2f / (n - 1f)
        for (i in 0 until n) {

            val y = 1f - i * div  // y goes from 1 to -1
            val radius = sqrt(1f - y * y)  // radius at y

            val theta = phi * i // golden angle increment

            val x = cos(theta) * radius
            val z = sin(theta) * radius

            points[i].set(x, y, z)

        }

        return points
    }

    fun create2(n: Int): FloatArray {

        // https://stackoverflow.com/a/26127012/4979303

        // todo alternative, which also creates the faces
        // http://extremelearning.com.au/evenly-distributing-points-on-a-sphere/

        val points = FloatArray(3 * n)
        val phi = (PI * (3.0 - sqrt(5.0))).toFloat() // golden angle in radians

        val div = 2f / (n - 1f)
        var j = 0
        for (i in 0 until n) {

            val y = 1f - i * div  // y goes from 1 to -1
            val radius = sqrt(1f - y * y)  // radius at y

            val theta = phi * i // golden angle increment

            val x = cos(theta) * radius
            val z = sin(theta) * radius

            points[j++] = x
            points[j++] = y
            points[j++] = z

        }

        return points

    }

    @JvmStatic
    fun main(args: Array<String>) {
        val pts = create(12)
        pts.forEach {
            println(it.print())
        }
    }

}