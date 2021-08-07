package me.anno.utils.types

import me.anno.utils.types.Vectors.minus
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector3fc

object Triangles {

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        maxDistance: Float,
        dstNormal: Vector3f,
        dstPosition: Vector3f
    ): Float {
        val n = subCross(a, b, c, dstNormal)
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0f || t >= maxDistance) return Float.POSITIVE_INFINITY
        // val q = origin + direction * t
        dstPosition.set(direction).mul(t).add(origin)
        // if ((b - a).cross(dst - a).dot(n) < 0) return Float.POSITIVE_INFINITY
        // if ((c - b).cross(dst - b).dot(n) < 0) return Float.POSITIVE_INFINITY
        // if ((a - c).cross(dst - c).dot(n) < 0) return Float.POSITIVE_INFINITY
        if (subCrossDot(a, b, dstPosition, n) < 0f) return Float.POSITIVE_INFINITY
        if (subCrossDot(b, c, dstPosition, n) < 0f) return Float.POSITIVE_INFINITY
        if (subCrossDot(c, a, dstPosition, n) < 0f) return Float.POSITIVE_INFINITY
        return t
    }


    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3dc, direction: Vector3dc,
        a: Vector3dc, b: Vector3dc, c: Vector3dc,
        maxDistance: Double,
        dstNormal: Vector3d,
        dstPosition: Vector3d
    ): Double {
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0f || t >= maxDistance) return Double.POSITIVE_INFINITY
        // val q = origin + direction * t
        dstPosition.set(direction).mul(t).add(origin)
        // if ((b - a).cross(dst - a).dot(n) < 0) return Float.POSITIVE_INFINITY
        // if ((c - b).cross(dst - b).dot(n) < 0) return Float.POSITIVE_INFINITY
        // if ((a - c).cross(dst - c).dot(n) < 0) return Float.POSITIVE_INFINITY
        if (subCrossDot(a, b, dstPosition, n) < 0.0) return Double.POSITIVE_INFINITY
        if (subCrossDot(b, c, dstPosition, n) < 0.0) return Double.POSITIVE_INFINITY
        if (subCrossDot(c, a, dstPosition, n) < 0.0) return Double.POSITIVE_INFINITY
        return t
    }

    /**
     * calculates ((b-a) x (c-a)) * n
     * without any allocations
     * */
    fun subCrossDot(a: Vector3fc, b: Vector3fc, c: Vector3fc, n: Vector3fc): Float {
        val x0 = b.x() - a.x()
        val y0 = b.y() - a.y()
        val z0 = b.z() - a.z()
        val x1 = c.x() - a.x()
        val y1 = c.y() - a.y()
        val z1 = c.z() - a.z()
        val rx = y0 * z1 - y1 * z0
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return rx * n.x() + ry * n.y() + rz * n.z()
    }

    /**
     * calculates (b-a) x (c-a)
     * without any allocations
     * */
    fun subCross(a: Vector3fc, b: Vector3fc, c: Vector3fc, dst: Vector3f): Vector3f {
        val x0 = b.x() - a.x()
        val y0 = b.y() - a.y()
        val z0 = b.z() - a.z()
        val x1 = c.x() - a.x()
        val y1 = c.y() - a.y()
        val z1 = c.z() - a.z()
        val rx = y0 * z1 - y1 * z0
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return dst.set(rx, ry, rz)
    }

    /**
     * calculates ((b-a) x (c-a)) * n
     * without any allocations
     * */
    fun subCrossDot(a: Vector3dc, b: Vector3dc, c: Vector3dc, n: Vector3dc): Double {
        val x0 = b.x() - a.x()
        val y0 = b.y() - a.y()
        val z0 = b.z() - a.z()
        val x1 = c.x() - a.x()
        val y1 = c.y() - a.y()
        val z1 = c.z() - a.z()
        val rx = y0 * z1 - y1 * z0
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return rx * n.x() + ry * n.y() + rz * n.z()
    }

    /**
     * calculates (b-a) x (c-a)
     * without any allocations
     * */
    fun subCross(a: Vector3dc, b: Vector3dc, c: Vector3dc, dst: Vector3d): Vector3d {
        val x0 = b.x() - a.x()
        val y0 = b.y() - a.y()
        val z0 = b.z() - a.z()
        val x1 = c.x() - a.x()
        val y1 = c.y() - a.y()
        val z1 = c.z() - a.z()
        val rx = y0 * z1 - y1 * z0
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return dst.set(rx, ry, rz)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val a = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        val b = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        val c = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        val n = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        println((b - a).cross(c - a).dot(n))
        println(subCrossDot(a, b, c, n))
    }

}