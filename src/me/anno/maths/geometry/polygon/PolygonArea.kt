package me.anno.maths.geometry.polygon

import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Calculating the surface area of planar polygons.
 * */
object PolygonArea {

    @JvmStatic
    fun List<Vector2f>.getPolygonArea2f(): Float {
        var sum = 0f
        for (j in indices) {
            val i = if (j == 0) lastIndex else j - 1
            val ni = this[i]
            val nj = this[j]
            sum += ni.cross(nj)
        }
        return 0.5f * sum
    }

    @JvmStatic
    fun List<Vector2d>.getPolygonArea2d(): Double {
        var sum = 0.0
        for (j in indices) {
            val i = if (j == 0) lastIndex else j - 1
            val ni = this[i]
            val nj = this[j]
            sum += ni.cross(nj)
        }
        return 0.5 * sum
    }

    @JvmStatic
    fun List<Vector3d>.getPolygonAreaVector3d(dst: Vector3d): Vector3d {
        dst.set(0.0)
        if (size < 3) return dst
        val pool3 = JomlPools.vec3d
        val tmpB = pool3.create()
        val tmpC = pool3.create()
        val a = this[0]
        for (i in 2 until size) {
            val b = this[i - 1].sub(a, tmpB)
            val c = this[i].sub(a, tmpC)
            dst.add(b.cross(c))
        }
        pool3.sub(2)
        return dst.mul(0.5)
    }

    @JvmStatic
    fun List<Vector3f>.getPolygonAreaVector3f(dst: Vector3f): Vector3f {
        dst.set(0.0)
        if (size < 3) return dst
        val pool3 = JomlPools.vec3f
        val tmpB = pool3.create()
        val tmpC = pool3.create()
        val a = this[0]
        for (i in 2 until size) {
            val b = this[i - 1].sub(a, tmpB)
            val c = this[i].sub(a, tmpC)
            dst.add(b.cross(c))
        }
        pool3.sub(2)
        return dst.mul(0.5f)
    }

    @JvmStatic
    fun getPolygonArea3d(points: List<Vector3d>): Double {
        val dst = JomlPools.vec3d.create()
        val area = points.getPolygonAreaVector3d(dst).length()
        JomlPools.vec3d.sub(1)
        return area
    }
}