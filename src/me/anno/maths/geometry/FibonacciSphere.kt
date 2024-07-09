package me.anno.maths.geometry

import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FibonacciSphere {

    val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // golden angle in radians

    private fun get(i: Int, n: Int, dst: Vector3f): Vector3f {
        val div = 2f / (n - 1f)
        val y = 1f - i * div  // y goes from 1 to -1
        val radius = sqrt(1f - y * y)  // radius at y

        val theta = goldenAngle * i // golden angle increment

        val x = cos(theta) * radius
        val z = sin(theta) * radius

        return dst.set(x, y, z)
    }

    /**
     * creates a sphere with N regularly positioned points
     * https://stackoverflow.com/a/26127012/4979303
     * */
    fun create(n: Int): List<Vector3f> {
        return createArrayList(n) { i -> get(i, n, Vector3f()) }
    }

    fun create2(n: Int): FloatArray {
        // https://stackoverflow.com/a/26127012/4979303
        // to do alternative, which also creates the faces?
        // http://extremelearning.com.au/evenly-distributing-points-on-a-sphere/
        val points = FloatArray(3 * n)
        val tmp = Vector3f()
        for (i in 0 until n) {
            get(i, n, tmp)
            tmp.get(points, i * 3)
        }
        return points
    }
}