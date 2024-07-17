package me.anno.utils.types

import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.crossLength
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Triangles {

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    @JvmStatic
    fun rayTriangleIntersection(
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
        maxDistance: Float,
        dstNormal: Vector3f,
        dstPosition: Vector3f,
        dstUVW: Vector3f? = null,
    ): Float {
        val n = subCross(a, b, c, dstNormal)
        val dist = n.dot(a)
        val distance = (dist - n.dot(origin)) / n.dot(direction)
        if (distance < 0f || distance >= maxDistance) return Float.POSITIVE_INFINITY
        direction.mulAdd(distance, origin, dstPosition)

        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v1z = c.z - a.z
        val v2x = dstPosition.x - a.x
        val v2y = dstPosition.y - a.y
        val v2z = dstPosition.z - a.z
        val d00 = sq(v0x, v0y, v0z)
        val d01 = v0x * v1x + v0y * v1y + v0z * v1z
        val d11 = sq(v1x, v1y, v1z)
        val d20 = v0x * v2x + v0y * v2y + v0z * v2z
        val d21 = v1x * v2x + v1y * v2y + v1z * v2z
        val d = 1f / (d00 * d11 - d01 * d01)
        val v = (d11 * d20 - d01 * d21) * d
        val w = (d00 * d21 - d01 * d20) * d
        val u = 1f - v - w

        if (u < 0f || v < 0f || w < 0f) return Float.POSITIVE_INFINITY
        dstUVW?.set(u, v, w)
        return distance
    }

    /**
     * only front faces are valid; back faces are skipped;
     * returns Infinity on miss
     * */
    @JvmStatic
    fun rayTriangleIntersectionFront(
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
        maxDistance: Float,
        dstNormal: Vector3f,
        dstPosition: Vector3f,
        dstUVW: Vector3f? = null,
    ): Float {
        val n = subCross(a, b, c, dstNormal)
        val ndd = n.dot(direction)
        if (ndd >= 0f) return Float.POSITIVE_INFINITY
        val dist = n.dot(a)
        val distance = (dist - n.dot(origin)) / ndd
        if (distance < 0f || distance >= maxDistance) return Float.POSITIVE_INFINITY
        direction.mulAdd(distance, origin, dstPosition) // dstPosition = dx, p0=a, p1=b, p2=c

        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v1z = c.z - a.z
        val v2x = dstPosition.x - a.x
        val v2y = dstPosition.y - a.y
        val v2z = dstPosition.z - a.z
        val d00 = sq(v0x, v0y, v0z)
        val d01 = v0x * v1x + v0y * v1y + v0z * v1z
        val d11 = sq(v1x, v1y, v1z)
        val d20 = v0x * v2x + v0y * v2y + v0z * v2z
        val d21 = v1x * v2x + v1y * v2y + v1z * v2z
        val d = 1f / (d00 * d11 - d01 * d01)
        val v = (d11 * d20 - d01 * d21) * d
        val w = (d00 * d21 - d01 * d20) * d
        val u = 1f - v - w

        if (u < 0f || v < 0f || w < 0f) return Float.POSITIVE_INFINITY
        dstUVW?.set(u, v, w)
        return distance
    }

    /**
     * only front faces are valid; back faces are skipped;
     * returns Infinity on miss
     * */
    @JvmStatic
    fun rayTriangleIntersectionFront(
        origin: Vector3d, direction: Vector3d,
        a: Vector3d, b: Vector3d, c: Vector3d,
        maxDistance: Double,
        dstNormal: Vector3d,
        dstPosition: Vector3d,
        dstUVW: Vector3d? = null,
    ): Double {
        val n = subCross(a, b, c, dstNormal)
        val ndd = n.dot(direction)
        if (ndd >= 0.0) return Double.POSITIVE_INFINITY
        val dist = n.dot(a)
        val distance = (dist - n.dot(origin)) / ndd
        if (distance < 0.0 || distance >= maxDistance) return Double.POSITIVE_INFINITY
        direction.mulAdd(distance, origin, dstPosition) // dstPosition = dx, p0=a, p1=b, p2=c

        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v1z = c.z - a.z
        val v2x = dstPosition.x - a.x
        val v2y = dstPosition.y - a.y
        val v2z = dstPosition.z - a.z
        val d00 = sq(v0x, v0y, v0z)
        val d01 = v0x * v1x + v0y * v1y + v0z * v1z
        val d11 = sq(v1x, v1y, v1z)
        val d20 = v0x * v2x + v0y * v2y + v0z * v2z
        val d21 = v1x * v2x + v1y * v2y + v1z * v2z
        val d = 1.0 / (d00 * d11 - d01 * d01)
        val v = (d11 * d20 - d01 * d21) * d
        val w = (d00 * d21 - d01 * d20) * d
        val u = 1.0 - v - w

        if (u < 0.0 || v < 0.0 || w < 0.0) return Double.POSITIVE_INFINITY
        dstUVW?.set(u, v, w)
        return distance
    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    @JvmStatic
    fun rayTriangleIntersection(
        origin: Vector3d, direction: Vector3d,
        a: Vector3d, b: Vector3d, c: Vector3d,
        maxDistance: Double,
        dstPosition: Vector3d,
        dstNormal: Vector3d,
        dstUVW: Vector3f? = null,
    ): Double {
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        val dist = n.dot(a)
        val distance = (dist - n.dot(origin)) / n.dot(direction) // distance to triangle
        if (distance < 0f || distance >= maxDistance) return Double.POSITIVE_INFINITY
        direction.mulAdd(distance, origin, dstPosition)

        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v1z = c.z - a.z
        val v2x = dstPosition.x - a.x
        val v2y = dstPosition.y - a.y
        val v2z = dstPosition.z - a.z
        val d00 = sq(v0x, v0y, v0z)
        val d01 = v0x * v1x + v0y * v1y + v0z * v1z
        val d11 = sq(v1x, v1y, v1z)
        val d20 = v0x * v2x + v0y * v2y + v0z * v2z
        val d21 = v1x * v2x + v1y * v2y + v1z * v2z
        val d = 1.0 / (d00 * d11 - d01 * d01)
        val v = (d11 * d20 - d01 * d21) * d
        val w = (d00 * d21 - d01 * d20) * d
        val u = 1.0 - v - w

        if (u < 0.0 || v < 0.0 || w < 0.0) return Double.POSITIVE_INFINITY
        dstUVW?.set(u, v, w)
        return distance
    }

    @JvmStatic
    fun rayTriangleIntersection(
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
        radiusAtOrigin: Float, radiusPerUnit: Float,
        maxDistance: Float,
        dstPosition: Vector3f,
        dstNormal: Vector3f,
        dstUVW: Vector3f? = null,
    ): Float {
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        val dist = n.dot(a)
        val cx = (a.x + b.x + c.x) * thirdF
        val cy = (a.y + b.y + c.y) * thirdF
        val cz = (a.z + b.z + c.z) * thirdF
        val f = computeConeInterpolation(origin, direction, cx, cy, cz, radiusAtOrigin, radiusPerUnit)
        val ox = mix(origin.x, cx, f)
        val oy = mix(origin.y, cy, f)
        val oz = mix(origin.z, cz, f)
        val distance = (dist - n.dot(ox, oy, oz)) / n.dot(direction) // distance to triangle
        if (distance < 0f || distance >= maxDistance) return Float.POSITIVE_INFINITY
        dstPosition.set(direction).mul(distance).add(ox, oy, oz)

        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v1z = c.z - a.z
        val v2x = dstPosition.x - a.x
        val v2y = dstPosition.y - a.y
        val v2z = dstPosition.z - a.z
        val d00 = sq(v0x, v0y, v0z)
        val d01 = v0x * v1x + v0y * v1y + v0z * v1z
        val d11 = sq(v1x, v1y, v1z)
        val d20 = v0x * v2x + v0y * v2y + v0z * v2z
        val d21 = v1x * v2x + v1y * v2y + v1z * v2z
        val d = 1f / (d00 * d11 - d01 * d01)
        val v = (d11 * d20 - d01 * d21) * d
        val w = (d00 * d21 - d01 * d20) * d
        val u = 1f - v - w

        if (u < 0f || v < 0f || w < 0f) return Float.POSITIVE_INFINITY
        dstUVW?.set(u, v, w)
        return distance
    }

    @JvmStatic
    fun rayTriangleIntersection(
        origin: Vector3d, direction: Vector3d,
        a: Vector3d, b: Vector3d, c: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        maxDistance: Double,
        dstPosition: Vector3d,
        dstNormal: Vector3d,
        dstUVW: Vector3d? = null
    ): Double {
        // compute triangle normal
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        // compute distance
        val dist = n.dot(a)
        val cx = (a.x + b.x + c.x) * thirdD
        val cy = (a.y + b.y + c.y) * thirdD
        val cz = (a.z + b.z + c.z) * thirdD
        val f = computeConeInterpolation(origin, direction, cx, cy, cz, radiusAtOrigin, radiusPerUnit)
        val ox = mix(origin.x, cx, f)
        val oy = mix(origin.y, cy, f)
        val oz = mix(origin.z, cz, f)
        val distance = (dist - n.dot(ox, oy, oz)) / n.dot(direction) // distance to triangle
        if (distance < 0.0 || distance >= maxDistance) return Double.POSITIVE_INFINITY
        dstPosition.set(direction).mul(distance).add(ox, oy, oz)

        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v1z = c.z - a.z
        val v2x = dstPosition.x - a.x
        val v2y = dstPosition.y - a.y
        val v2z = dstPosition.z - a.z
        val d00 = sq(v0x, v0y, v0z)
        val d01 = v0x * v1x + v0y * v1y + v0z * v1z
        val d11 = sq(v1x, v1y, v1z)
        val d20 = v0x * v2x + v0y * v2y + v0z * v2z
        val d21 = v1x * v2x + v1y * v2y + v1z * v2z
        val d = 1.0 / (d00 * d11 - d01 * d01)
        val v = (d11 * d20 - d01 * d21) * d
        val w = (d00 * d21 - d01 * d20) * d
        val u = 1.0 - v - w

        if (u < 0.0 || v < 0.0 || w < 0.0) return Double.POSITIVE_INFINITY
        dstUVW?.set(u, v, w)
        return distance
    }

    /**
     * calculates ((b-a) x (c-a)) * n
     * without any allocations
     * */
    @JvmStatic
    fun subCrossDot(a: Vector3f, b: Vector3f, c: Vector3f, n: Vector3f): Float {
        val x0 = b.x - a.x
        val y0 = b.y - a.y
        val z0 = b.z - a.z
        val x1 = c.x - a.x
        val y1 = c.y - a.y
        val z1 = c.z - a.z
        val rx = y0 * z1 - z0 * y1
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return n.dot(rx, ry, rz)
    }

    /**
     * calculates (ba x (c-a)) * n
     * without any allocations
     * */
    @JvmStatic
    fun halfSubCrossDot(ba: Vector3f, a: Vector3f, c: Vector3f, n: Vector3f): Float {
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
    @JvmStatic
    fun subCross(a: Vector3f, b: Vector3f, c: Vector3f, dst: Vector3f): Vector3f {
        val x0 = b.x - a.x
        val y0 = b.y - a.y
        val z0 = b.z - a.z
        val x1 = c.x - a.x
        val y1 = c.y - a.y
        val z1 = c.z - a.z
        val rx = y0 * z1 - z0 * y1
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return dst.set(rx, ry, rz)
    }

    /**
     * calculates (b-a) x (c-a)
     * without any allocations
     * */
    @JvmStatic
    fun subCross(a: Vector2f, b: Vector2f, c: Vector2f): Float {
        val x0 = b.x - a.x
        val y0 = b.y - a.y
        val x1 = c.x - a.x
        val y1 = c.y - a.y
        return x0 * y1 - y0 * x1
    }

    /**
     * calculates (b-a) x (c-a)
     * without any allocations
     * */
    @JvmStatic
    fun subCross(a: Vector3d, b: Vector3d, c: Vector3d, dst: Vector3d): Vector3d {
        val x0 = b.x - a.x
        val y0 = b.y - a.y
        val z0 = b.z - a.z
        val x1 = c.x - a.x
        val y1 = c.y - a.y
        val z1 = c.z - a.z
        val rx = y0 * z1 - y1 * z0
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return dst.set(rx, ry, rz)
    }

    @JvmStatic
    fun linePointTFactor(start: Vector3f, dir: Vector3f, px: Float, py: Float, pz: Float): Float {
        return max(dir.dot(px, py, pz) - dir.dot(start), 0f)
    }

    @JvmStatic
    fun linePointTFactor(start: Vector3d, dir: Vector3d, px: Double, py: Double, pz: Double): Double {
        return max(dir.dot(px, py, pz) - dir.dot(start), 0.0)
    }

    /**
     * 0 = far away, 1 = hitting center guaranteed
     * */
    @JvmStatic
    fun computeConeInterpolation(
        origin: Vector3f, direction: Vector3f, px: Float, py: Float, pz: Float,
        radiusAtOrigin: Float, radiusPerUnit: Float
    ): Float {
        val distance = abs(linePointTFactor(origin, direction, px, py, pz))
        val radius = max(0f, radiusAtOrigin + distance * radiusPerUnit)
        return min(radius / max(distance, 1e-38f), 1f) // 0 = far away, 1 = hitting center
    }

    /**
     * 0 = far away, 1 = hitting center guaranteed
     * */
    @JvmStatic
    fun computeConeInterpolation(
        origin: Vector3d, direction: Vector3d, px: Double, py: Double, pz: Double,
        radiusAtOrigin: Double, radiusPerUnit: Double
    ): Double {
        val distance = abs(linePointTFactor(origin, direction, px, py, pz))
        val radius = max(0.0, radiusAtOrigin + distance * radiusPerUnit)
        return min(radius / max(distance, 1e-308), 1.0) // 0 = far away, 1 = hitting center
    }

    @JvmStatic
    fun crossDot(
        a: Vector3f,
        b: Vector3f,
        d: Vector3f,
    ): Float {
        val cx = a.y * b.z - a.z * b.y
        val cy = a.z * b.x - a.x * b.z
        val cz = a.x * b.y - a.y * b.x
        return d.dot(cx, cy, cz)
    }

    @JvmStatic
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

    @JvmStatic
    fun Vector2f.getSideSign(b: Vector2f, c: Vector2f): Float {
        val bx = b.x - x
        val by = b.y - y
        val cx = c.x - x
        val cy = c.y - y
        return cx * by - cy * bx
    }

    @JvmStatic
    fun Vector2d.getSideSign(b: Vector2d, c: Vector2d): Double {
        val bx = b.x - x
        val by = b.y - y
        val cx = c.x - x
        val cy = c.y - y
        return cx * by - cy * bx
    }

    @JvmStatic
    fun Vector2f.isInsideTriangle(a: Vector2f, b: Vector2f, c: Vector2f): Boolean {

        var sum = 0

        if (getSideSign(a, b) >= 0f) sum++
        if (getSideSign(b, c) >= 0f) sum++
        if (getSideSign(c, a) >= 0f) sum++

        // left or right of all lines
        return sum == 0 || sum == 3
    }

    @JvmStatic
    fun Vector2d.isInsideTriangle(a: Vector2d, b: Vector2d, c: Vector2d): Boolean {
        val asX = x - a.x
        val asY = y - a.y
        val sAb = (b.x - a.x) * asY - (b.y - a.y) * asX > 0
        if ((c.x - a.x) * asY - (c.y - a.y) * asX > 0 == sAb) return false
        return (c.x - b.x) * (y - b.y) - (c.y - b.y) * (x - b.x) > 0 == sAb
    }

    @JvmStatic
    fun Vector2d.isInsideTriangle2(a: Vector2d, b: Vector2d, c: Vector2d): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0
    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    @JvmStatic
    fun rayTriangleIntersect(
        origin: Vector3d, direction: Vector3d,
        a: Vector3d, b: Vector3d, c: Vector3d,
        maxDistance: Double,
        allowBackside: Boolean
    ): Boolean {
        val t0 = JomlPools.vec3d.create()
        val t1 = JomlPools.vec3d.create()
        val dist = if (allowBackside) rayTriangleIntersection(origin, direction, a, b, c, maxDistance, t0, t1)
        else rayTriangleIntersectionFront(origin, direction, a, b, c, maxDistance, t0, t1)
        JomlPools.vec3d.sub(2)
        return dist.isFinite()
    }

    fun getBarycentrics(a: Vector2f, b: Vector2f, c: Vector2f, pt: Vector2f, dstUVW: Vector3f) {
        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v2x = pt.x - a.x
        val v2y = pt.y - a.y
        val d00 = sq(v0x, v0y)
        val d01 = v0x * v1x + v0y * v1y
        val d11 = sq(v1x, v1y)
        val d20 = v0x * v2x + v0y * v2y
        val d21 = v1x * v2x + v1y * v2y
        val div = d00 * d11 - d01 * d01
        if (abs(div) < 1e-38f) {
            dstUVW.set(1f, 0f, 0f)
        } else {
            val d = 1f / div
            val v = (d11 * d20 - d01 * d21) * d
            val w = (d00 * d21 - d01 * d20) * d
            val u = 1f - v - w
            dstUVW.set(u, v, w)
        }
    }

    fun getBarycentrics(a: Vector3f, b: Vector3f, c: Vector3f, pt: Vector3f, dstUVW: Vector3f) {
        val v0x = b.x - a.x
        val v0y = b.y - a.y
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1y = c.y - a.y
        val v1z = c.z - a.z
        val v2x = pt.x - a.x
        val v2y = pt.y - a.y
        val v2z = pt.z - a.z
        val d00 = sq(v0x, v0y, v0z)
        val d01 = v0x * v1x + v0y * v1y + v0z * v1z
        val d11 = sq(v1x, v1y, v1z)
        val d20 = v0x * v2x + v0y * v2y + v0z * v2z
        val d21 = v1x * v2x + v1y * v2y + v1z * v2z
        val d = 1f / (d00 * d11 - d01 * d01)
        val v = (d11 * d20 - d01 * d21) * d
        val w = (d00 * d21 - d01 * d20) * d
        val u = 1f - v - w
        dstUVW.set(u, v, w)
    }

    fun getParallelogramArea(a: Vector3f, b: Vector3f, c: Vector3f): Float {
        return crossLength(
            b.x - a.x, b.y - a.y, b.z - a.z,
            c.x - a.x, c.y - a.y, c.z - a.z
        )
    }

    fun getTriangleArea(a: Vector3f, b: Vector3f, c: Vector3f): Float {
        return getParallelogramArea(a, b, c) * 0.5f
    }

    const val thirdD = 1.0 / 3.0
    const val thirdF = thirdD.toFloat()
}