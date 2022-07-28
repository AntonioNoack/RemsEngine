package me.anno.utils.types

import me.anno.image.ImageWriter
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.plus
import me.anno.utils.types.Vectors.times
import org.joml.*
import java.lang.Math
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
        val distance = (d - n.dot(origin)) / n.dot(direction)
        if (distance < 0f || distance >= maxDistance) return Float.POSITIVE_INFINITY
        direction.mulAdd(distance, origin, dstPosition)
        if (subCrossDot(a, b, dstPosition, n) < 0f ||
            subCrossDot(b, c, dstPosition, n) < 0f ||
            subCrossDot(c, a, dstPosition, n) < 0f
        ) return Float.POSITIVE_INFINITY
        return distance
    }

    /**
     * only front faces are valid; back faces are skipped;
     * returns Infinity on miss
     * */
    fun rayTriangleIntersectionFront(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        maxDistance: Float,
        dstNormal: Vector3f,
        dstPosition: Vector3f
    ): Float {
        val n = subCross(a, b, c, dstNormal)
        val ndd = n.dot(direction)
        if (ndd >= 0f) return Float.POSITIVE_INFINITY
        val d = n.dot(a)
        val distance = (d - n.dot(origin)) / ndd
        if (distance < 0f || distance >= maxDistance) return Float.POSITIVE_INFINITY
        direction.mulAdd(distance, origin, dstPosition)
        if (subCrossDot(a, b, dstPosition, n) < 0f ||
            subCrossDot(b, c, dstPosition, n) < 0f ||
            subCrossDot(c, a, dstPosition, n) < 0f
        ) return Float.POSITIVE_INFINITY
        return distance
    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3dc, direction: Vector3dc,
        a: Vector3dc, b: Vector3dc, c: Vector3dc,
        maxDistance: Double,
        dstPosition: Vector3d,
        dstNormal: Vector3d,
    ): Double {
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        val d = n.dot(a)
        val distance = (d - n.dot(origin)) / n.dot(direction) // distance to triangle
        if (distance < 0f || distance >= maxDistance) return Double.POSITIVE_INFINITY
        direction.mulAdd(distance, origin, dstPosition)
        if (subCrossDot(a, b, dstPosition, n) < 0.0 ||
            subCrossDot(b, c, dstPosition, n) < 0.0 ||
            subCrossDot(c, a, dstPosition, n) < 0.0
        ) return Double.POSITIVE_INFINITY
        return distance
    }

    fun rayTriangleIntersection(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        radiusAtOrigin: Float, radiusPerUnit: Float,
        maxDistance: Float,
        dstPosition: Vector3f,
        dstNormal: Vector3f,
    ): Float {
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        val d = n.dot(a)
        val cx = (a.x() + b.x() + c.x()) * thirdF
        val cy = (a.y() + b.y() + c.y()) * thirdF
        val cz = (a.z() + b.z() + c.z()) * thirdF
        val f = computeConeInterpolation(origin, direction, cx, cy, cz, radiusAtOrigin, radiusPerUnit)
        val ox = mix(origin.x(), cx, f)
        val oy = mix(origin.y(), cy, f)
        val oz = mix(origin.z(), cz, f)
        val distance = (d - n.dot(ox, oy, oz)) / n.dot(direction) // distance to triangle
        if (distance < 0f || distance >= maxDistance) return Float.POSITIVE_INFINITY
        dstPosition.set(direction).mul(distance).add(ox, oy, oz)
        if (subCrossDot(a, b, dstPosition, n) < 0f ||
            subCrossDot(b, c, dstPosition, n) < 0f ||
            subCrossDot(c, a, dstPosition, n) < 0f
        ) return Float.POSITIVE_INFINITY
        return distance
    }

    fun rayTriangleIntersection(
        origin: Vector3dc, direction: Vector3dc,
        a: Vector3dc, b: Vector3dc, c: Vector3dc,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        maxDistance: Double,
        dstPosition: Vector3d,
        dstNormal: Vector3d,
    ): Double {
        // compute triangle normal
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        // compute distance
        val d = n.dot(a)
        val cx = (a.x() + b.x() + c.x()) * thirdD
        val cy = (a.y() + b.y() + c.y()) * thirdD
        val cz = (a.z() + b.z() + c.z()) * thirdD
        val f = computeConeInterpolation(origin, direction, cx, cy, cz, radiusAtOrigin, radiusPerUnit)
        val ox = mix(origin.x(), cx, f)
        val oy = mix(origin.y(), cy, f)
        val oz = mix(origin.z(), cz, f)
        val distance = (d - n.dot(ox, oy, oz)) / n.dot(direction) // distance to triangle
        if (distance < 0.0 || distance >= maxDistance) return Double.POSITIVE_INFINITY
        dstPosition.set(direction).mul(distance).add(ox, oy, oz)
        if (subCrossDot(a, b, dstPosition, n) < 0.0 ||
            subCrossDot(b, c, dstPosition, n) < 0.0 ||
            subCrossDot(c, a, dstPosition, n) < 0.0
        ) return Double.POSITIVE_INFINITY
        return distance
    }

    /**
     * calculates ((b-a) x (c-a)) * n
     * without any allocations
     * */
    fun subCrossDot(a: Vector3fc, b: Vector3fc, c: Vector3fc, n: Vector3f): Float {
        val x0 = b.x() - a.x()
        val y0 = b.y() - a.y()
        val z0 = b.z() - a.z()
        val x1 = c.x() - a.x()
        val y1 = c.y() - a.y()
        val z1 = c.z() - a.z()
        val rx = y0 * z1 - z0 * y1
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return n.dot(rx, ry, rz)
    }

    /**
     * calculates ((b-a) x (c-a)) * n
     * without any allocations
     * */
    fun halfSubCrossDot(ba: Vector3f, a: Vector3f, c: Vector3f, n: Vector3fc): Float {
        val x1 = c.x - a.x
        val y1 = c.y - a.y
        val z1 = c.z - a.z
        val rx = ba.y * z1 - ba.z * y1
        val ry = ba.z * x1 - ba.x * z1
        val rz = ba.x * y1 - ba.y * x1
        return n.dot(rx, ry, rz)
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
        val rx = y0 * z1 - z0 * y1
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
        return n.dot(rx, ry, rz)
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

    @Suppress("unused")
    fun linePointDistance(start: Vector3fc, dir: Vector3fc, px: Float, py: Float, pz: Float): Float {
        val tmp = JomlPools.vec3f.borrow()
        return tmp.set(start).sub(px, py, pz)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3dc, dir: Vector3dc, px: Double, py: Double, pz: Double): Double {
        val tmp = JomlPools.vec3d.borrow()
        return tmp.set(start).sub(px, py, pz)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3fc, dir: Vector3fc, p: Vector3fc): Float {
        val tmp = JomlPools.vec3f.borrow()
        return tmp.set(start).sub(p)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3dc, dir: Vector3dc, p: Vector3dc): Double {
        val tmp = JomlPools.vec3d.borrow()
        return tmp.set(start).sub(p)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3fc, dir: Vector3fc, p: Vector3dc): Float {
        val tmp = JomlPools.vec3f.borrow()
        return tmp.set(start).sub(p.x().toFloat(), p.y().toFloat(), p.z().toFloat())
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3dc, dir: Vector3dc, p: Vector3fc): Double {
        val tmp = JomlPools.vec3d.borrow()
        return tmp.set(start).sub(p)
            .cross(dir).length()
    }

    fun linePointTFactor(start: Vector3fc, dir: Vector3fc, px: Float, py: Float, pz: Float): Float {
        return max(dir.dot(px, py, pz) - dir.dot(start), 0f)
    }

    fun linePointTFactor(start: Vector3dc, dir: Vector3dc, px: Double, py: Double, pz: Double): Double {
        return max(dir.dot(px, py, pz) - dir.dot(start), 0.0)
    }

    /**
     * 0 = far away, 1 = hitting center guaranteed
     * */
    fun computeConeInterpolation(
        origin: Vector3fc, direction: Vector3fc, px: Float, py: Float, pz: Float,
        radiusAtOrigin: Float, radiusPerUnit: Float
    ): Float {
        val distance = abs(linePointTFactor(origin, direction, px, py, pz))
        val radius = max(0f, radiusAtOrigin + distance * radiusPerUnit)
        return min(radius / max(distance, 1e-38f), 1f) // 0 = far away, 1 = hitting center
    }

    /**
     * 0 = far away, 1 = hitting center guaranteed
     * */
    fun computeConeInterpolation(
        origin: Vector3dc, direction: Vector3dc, px: Double, py: Double, pz: Double,
        radiusAtOrigin: Double, radiusPerUnit: Double
    ): Double {
        val distance = abs(linePointTFactor(origin, direction, px, py, pz))
        val radius = max(0.0, radiusAtOrigin + distance * radiusPerUnit)
        return min(radius / max(distance, 1e-308), 1.0) // 0 = far away, 1 = hitting center
    }

    @JvmStatic
    fun main(args: Array<String>) {
        testSubCrossDot()
        testTriangleTest()
    }

    fun testTriangleTest() {
        val a = Vector3d()
        val b = Vector3d(0.4, 1.0, 0.0)
        val c = Vector3d(1.0, 0.0, 0.0)
        val origin = Vector3d(0.5, 0.5, -1.1)
        val size = 256
        ImageWriter.writeImageInt(size, size, false, "triangle", 512) { x, y, _ ->
            val direction = Vector3d((x - size * 0.5) / size, -(y - size * 0.5) / size, 1.0)
            if (rayTriangleIntersection(origin, direction, a, b, c, 1e3, Vector3d(), Vector3d()).isFinite()) 0 else -1
        }
    }

    fun testSubCrossDot() {
        val a = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        val b = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        val c = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        val n = Vector3f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        println((b - a).cross(c - a).dot(n))
        println(subCrossDot(a, b, c, n))
    }

    fun crossDot(
        a: Vector3f,
        b: Vector3f,
        d: Vector3f,
    ): Float {
        val cx = a.y * b.z - a.z * b.y
        val cy = a.z * b.x - a.x * b.z
        val cz = a.x * b.y - a.y * b.x
        return d.dot(cx,cy,cz)
    }

    fun crossDot(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        dx: Float, dy: Float, dz: Float
    ): Float {
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx
        return cx * dx + cy * dy + cz * dz
    }


    fun getSideSign(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx0 = ax - px
        val dy0 = ay - py
        val dx1 = bx - px
        val dy1 = by - py
        return dx1 * dy0 - dy1 * dx0
    }

    fun getSideSign(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx0 = ax - px
        val dy0 = ay - py
        val dx1 = bx - px
        val dy1 = by - py
        return dx1 * dy0 - dy1 * dx0
    }

    fun Vector2fc.getSideSign(b: Vector2fc, c: Vector2fc): Float {
        val bx = b.x() - x()
        val by = b.y() - y()
        val cx = c.x() - x()
        val cy = c.y() - y()
        return cx * by - cy * bx
    }

    fun Vector2dc.getSideSign(b: Vector2dc, c: Vector2dc): Double {
        val bx = b.x() - x()
        val by = b.y() - y()
        val cx = c.x() - x()
        val cy = c.y() - y()
        return cx * by - cy * bx
    }

    fun Vector2fc.isInsideTriangle(a: Vector2fc, b: Vector2fc, c: Vector2fc): Boolean {
        val asX = x() - a.x()
        val asY = y() - a.y()
        val sAb = (b.x() - a.x()) * asY - (b.y() - a.y()) * asX > 0
        if ((c.x() - a.x()) * asY - (c.y() - a.y()) * asX > 0 == sAb) return false
        return (c.x() - b.x()) * (y() - b.y()) - (c.y() - b.y()) * (x() - b.x()) > 0 == sAb
    }

    fun Vector2fc.isInsideTriangle2(a: Vector2fc, b: Vector2fc, c: Vector2fc): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }


    fun Vector2d.isInsideTriangle(a: Vector2dc, b: Vector2dc, c: Vector2dc): Boolean {
        val asX = x() - a.x()
        val asY = y() - a.y()
        val sAb = (b.x() - a.x()) * asY - (b.y() - a.y()) * asX > 0
        if ((c.x() - a.x()) * asY - (c.y() - a.y()) * asX > 0 == sAb) return false
        return (c.x() - b.x()) * (y() - b.y()) - (c.y() - b.y()) * (x() - b.x()) > 0 == sAb
    }

    fun Vector2dc.isInsideTriangle2(a: Vector2dc, b: Vector2dc, c: Vector2dc): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        maxDistance: Float,
        allowBackside: Boolean
    ): Pair<Vector3fc, Float>? {
        val ba = b - a
        val ca = c - a
        val n = ba.cross(ca, Vector3f())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0f || t >= maxDistance) return null
        val q = Vector3f(direction).mul(t).add(origin)
        var sum = 0
        if (subCrossDot(a, b, q, n) < 0.0) sum++
        if (subCrossDot(b, c, q, n) < 0.0) sum++
        if (subCrossDot(c, a, q, n) < 0.0) sum++
        return if (sum == 0 || (allowBackside && sum == 3)) q to t else null
    }

    fun rayTriangleIntersect(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        maxDistance: Float,
        allowBackside: Boolean
    ): Boolean {
        val ba = b - a
        val ca = c - a
        val n = ba.cross(ca, JomlPools.vec3f.borrow())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0f || t >= maxDistance) return false
        val q = origin + direction * t
        var sum = 0
        if (subCrossDot(a, b, q, n) < 0.0) sum++
        if (subCrossDot(b, c, q, n) < 0.0) sum++
        if (subCrossDot(c, a, q, n) < 0.0) sum++
        return sum == 0 || (allowBackside && sum == 3)
    }

    fun rayTriangleIntersect(
        origin: Vector3dc, direction: Vector3dc,
        a: Vector3dc, b: Vector3dc, c: Vector3dc,
        maxDistance: Double,
        allowBackside: Boolean
    ): Boolean {
        val n = subCross(a, b, c, JomlPools.vec3d.borrow())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0.0 || t >= maxDistance) return false
        val q = origin + direction * t
        var sum = 0
        if (subCrossDot(a, b, q, n) < 0.0) sum++
        if (subCrossDot(b, c, q, n) < 0.0) sum++
        if (subCrossDot(c, a, q, n) < 0.0) sum++
        return sum == 0 || (allowBackside && sum == 3)
    }

    const val thirdD = 1.0 / 3.0
    const val thirdF = thirdD.toFloat()

}