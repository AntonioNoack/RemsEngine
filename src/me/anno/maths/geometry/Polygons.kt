package me.anno.maths.geometry

import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d

/**
 * Utility functions for polygons; only for calculating their surface area at the moment
 * */
object Polygons {

    // surely, we have this implemented already somewhere, find it
    // -> only an ugly version in EarCut operating on FloatArray
    @JvmStatic
    fun getPolygonArea2f(points: List<Vector2f>): Float {
        var sum = 0f
        for (j in points.indices) {
            val i = if (j == 0) points.lastIndex else j - 1
            val ni = points[i]
            val nj = points[j]
            sum += ni.cross(nj)
        }
        return 0.5f * sum
    }

    // surely, we have this implemented already somewhere, find it
    // -> only an ugly version in EarCut operating on FloatArray
    @JvmStatic
    fun getPolygonArea2d(points: List<Vector2d>): Double {
        var sum = 0.0
        for (j in points.indices) {
            val i = if (j == 0) points.lastIndex else j - 1
            val ni = points[i]
            val nj = points[j]
            sum += ni.cross(nj)
        }
        return 0.5 * sum
    }

    @JvmStatic
    fun getPolygonAreaVector3d(points: List<Vector3d>, dst: Vector3d): Vector3d {
        dst.set(0.0)
        if (points.size < 3) return dst
        val pool3 = JomlPools.vec3d
        val tmpB = pool3.create()
        val tmpC = pool3.create()
        val a = points[0]
        for (i in 2 until points.size) {
            val b = points[i - 1].sub(a, tmpB)
            val c = points[i].sub(a, tmpC)
            dst.add(b.cross(c))
        }
        pool3.sub(2)
        return dst.mul(0.5)
    }

    @JvmStatic
    fun getPolygonArea3d(points: List<Vector3d>): Double {
        val dst = JomlPools.vec3d.create()
        val area = getPolygonAreaVector3d(points, dst).length()
        JomlPools.vec3d.sub(1)
        return area
    }
}