package me.anno.utils.types

import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import org.joml.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Triangles {

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
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
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
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
        origin: Vector3d, direction: Vector3d,
        a: Vector3d, b: Vector3d, c: Vector3d,
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
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
        radiusAtOrigin: Float, radiusPerUnit: Float,
        maxDistance: Float,
        dstPosition: Vector3f,
        dstNormal: Vector3f,
    ): Float {
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        val d = n.dot(a)
        val cx = (a.x + b.x + c.x) * thirdF
        val cy = (a.y + b.y + c.y) * thirdF
        val cz = (a.z + b.z + c.z) * thirdF
        val f = computeConeInterpolation(origin, direction, cx, cy, cz, radiusAtOrigin, radiusPerUnit)
        val ox = mix(origin.x, cx, f)
        val oy = mix(origin.y, cy, f)
        val oz = mix(origin.z, cz, f)
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
        origin: Vector3d, direction: Vector3d,
        a: Vector3d, b: Vector3d, c: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        maxDistance: Double,
        dstPosition: Vector3d,
        dstNormal: Vector3d,
    ): Double {
        // compute triangle normal
        val n = subCross(a, b, c, dstNormal) // to keep the magnitude of the calculations under control
        // compute distance
        val d = n.dot(a)
        val cx = (a.x + b.x + c.x) * thirdD
        val cy = (a.y + b.y + c.y) * thirdD
        val cz = (a.z + b.z + c.z) * thirdD
        val f = computeConeInterpolation(origin, direction, cx, cy, cz, radiusAtOrigin, radiusPerUnit)
        val ox = mix(origin.x, cx, f)
        val oy = mix(origin.y, cy, f)
        val oz = mix(origin.z, cz, f)
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
     * calculates ((b-a) x (c-a)) * n
     * without any allocations
     * */
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
     * calculates ((b-a) x (c-a)) * n
     * without any allocations
     * */
    fun subCrossDot(a: Vector3d, b: Vector3d, c: Vector3d, n: Vector3d): Double {
        val x0 = b.x - a.x
        val y0 = b.y - a.y
        val z0 = b.z - a.z
        val x1 = c.x - a.x
        val y1 = c.y - a.y
        val z1 = c.z - a.z
        val rx = y0 * z1 - y1 * z0
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        return n.dot(rx, ry, rz)
    }

    /**
     * calculates (b-a) x (c-a)
     * without any allocations
     * */
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

    @Suppress("unused")
    fun linePointDistance(start: Vector3f, dir: Vector3f, px: Float, py: Float, pz: Float): Float {
        val tmp = JomlPools.vec3f.borrow()
        return tmp.set(start).sub(px, py, pz)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3d, dir: Vector3d, px: Double, py: Double, pz: Double): Double {
        val tmp = JomlPools.vec3d.borrow()
        return tmp.set(start).sub(px, py, pz)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3f, dir: Vector3f, p: Vector3f): Float {
        val tmp = JomlPools.vec3f.borrow()
        return tmp.set(start).sub(p)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3d, dir: Vector3d, p: Vector3d): Double {
        val tmp = JomlPools.vec3d.borrow()
        return tmp.set(start).sub(p)
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3f, dir: Vector3f, p: Vector3d): Float {
        val tmp = JomlPools.vec3f.borrow()
        return tmp.set(start).sub(p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
            .cross(dir).length()
    }

    @Suppress("unused")
    fun linePointDistance(start: Vector3d, dir: Vector3d, p: Vector3f): Double {
        val tmp = JomlPools.vec3d.borrow()
        return tmp.set(start).sub(p)
            .cross(dir).length()
    }

    fun linePointTFactor(start: Vector3f, dir: Vector3f, px: Float, py: Float, pz: Float): Float {
        return max(dir.dot(px, py, pz) - dir.dot(start), 0f)
    }

    fun linePointTFactor(start: Vector3d, dir: Vector3d, px: Double, py: Double, pz: Double): Double {
        return max(dir.dot(px, py, pz) - dir.dot(start), 0.0)
    }

    /**
     * 0 = far away, 1 = hitting center guaranteed
     * */
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
    fun computeConeInterpolation(
        origin: Vector3d, direction: Vector3d, px: Double, py: Double, pz: Double,
        radiusAtOrigin: Double, radiusPerUnit: Double
    ): Double {
        val distance = abs(linePointTFactor(origin, direction, px, py, pz))
        val radius = max(0.0, radiusAtOrigin + distance * radiusPerUnit)
        return min(radius / max(distance, 1e-308), 1.0) // 0 = far away, 1 = hitting center
    }

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

    fun Vector2f.getSideSign(b: Vector2f, c: Vector2f): Float {
        val bx = b.x - x
        val by = b.y - y
        val cx = c.x - x
        val cy = c.y - y
        return cx * by - cy * bx
    }

    fun Vector2d.getSideSign(b: Vector2d, c: Vector2d): Double {
        val bx = b.x - x
        val by = b.y - y
        val cx = c.x - x
        val cy = c.y - y
        return cx * by - cy * bx
    }

    fun Vector2f.isInsideTriangle(a: Vector2f, b: Vector2f, c: Vector2f): Boolean {
        val asX = x - a.x
        val asY = y - a.y
        val sAb = (b.x - a.x) * asY - (b.y - a.y) * asX > 0
        if ((c.x - a.x) * asY - (c.y - a.y) * asX > 0 == sAb) return false
        return (c.x - b.x) * (y - b.y) - (c.y - b.y) * (x - b.x) > 0 == sAb
    }

    fun Vector2f.isInsideTriangle2(a: Vector2f, b: Vector2f, c: Vector2f): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }

    fun Vector2d.isInsideTriangle(a: Vector2d, b: Vector2d, c: Vector2d): Boolean {
        val asX = x - a.x
        val asY = y - a.y
        val sAb = (b.x - a.x) * asY - (b.y - a.y) * asX > 0
        if ((c.x - a.x) * asY - (c.y - a.y) * asX > 0 == sAb) return false
        return (c.x - b.x) * (y - b.y) - (c.y - b.y) * (x - b.x) > 0 == sAb
    }

    fun Vector2d.isInsideTriangle2(a: Vector2d, b: Vector2d, c: Vector2d): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
        maxDistance: Float,
        allowBackside: Boolean
    ): Pair<Vector3f, Float>? {
        val ba = b - a
        val ca = c - a
        val n = ba.cross(ca, Vector3f())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        return if (t in 0f..maxDistance) {
            val q = Vector3f(direction).mul(t).add(origin)
            var sum = 0
            if (subCrossDot(a, b, q, n) < 0f) sum++
            if (subCrossDot(b, c, q, n) < 0f) sum++
            if (subCrossDot(c, a, q, n) < 0f) sum++
            if (sum == 0 || (allowBackside && sum == 3)) q to t else null
        } else null
    }

    fun rayTriangleIntersect(
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
        maxDistance: Float,
        allowBackside: Boolean
    ): Boolean {
        val n = subCross(a, b, c, JomlPools.vec3f.borrow())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        return if (t in 0f..maxDistance) {
            val q = origin + direction * t
            var sum = 0
            if (subCrossDot(a, b, q, n) < 0f) sum++
            if (subCrossDot(b, c, q, n) < 0f) sum++
            if (subCrossDot(c, a, q, n) < 0f) sum++
            sum == 0 || (allowBackside && sum == 3)
        } else false
    }

    fun rayTriangleIntersect(
        origin: Vector3d, direction: Vector3d,
        a: Vector3d, b: Vector3d, c: Vector3d,
        maxDistance: Double,
        allowBackside: Boolean
    ): Boolean {
        val n = subCross(a, b, c, JomlPools.vec3d.borrow())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        return if (t in 0.0..maxDistance) {
            val q = origin + direction * t
            var sum = 0
            if (subCrossDot(a, b, q, n) < 0.0) sum++
            if (subCrossDot(b, c, q, n) < 0.0) sum++
            if (subCrossDot(c, a, q, n) < 0.0) sum++
            sum == 0 || (allowBackside && sum == 3)
        } else false
    }

    const val thirdD = 1.0 / 3.0
    const val thirdF = thirdD.toFloat()

}