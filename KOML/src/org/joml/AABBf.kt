package org.joml

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AABBf(
    var minX: Float, var minY: Float, var minZ: Float,
    var maxX: Float, var maxY: Float, var maxZ: Float
) {


    constructor(base: AABBf) : this(base.minX, base.minY, base.minZ, base.maxX, base.maxY, base.maxZ)
    constructor(min: Float, max: Float) : this(min, min, min, max, max, max)
    constructor() : this(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
    constructor(base: AABBd) : this() {
        set(base)
    }

    override fun toString() = "($minX,$minY,$minZ)-($maxX,$maxY,$maxZ)"

    fun setMin(v: Vector3f) =
        setMin(v.x, v.y, v.z)

    fun setMax(v: Vector3f) =
        setMax(v.x, v.y, v.z)

    fun setMin(x: Float, y: Float, z: Float): AABBf {
        minX = x
        minY = y
        minZ = z
        return this
    }

    fun setMax(x: Float, y: Float, z: Float): AABBf {
        maxX = x
        maxY = y
        maxZ = z
        return this
    }

    fun union(other: AABBf, dst: AABBf = this): AABBf {
        dst.minX = min(minX, other.minX)
        dst.minY = min(minY, other.minY)
        dst.minZ = min(minZ, other.minZ)
        dst.maxX = max(maxX, other.maxX)
        dst.maxY = max(maxY, other.maxY)
        dst.maxZ = max(maxZ, other.maxZ)
        return this
    }

    fun getMin(dim: Int) =
        if (dim == 0) minX
        else (if (dim == 1) minY else minZ)

    fun getMax(dim: Int) =
        if (dim == 0) maxX
        else (if (dim == 1) maxY else maxZ)

    fun union(point: Vector2f, dst: AABBf = this) =
        union(point.x, point.y, 0f, dst)

    fun union(point: Vector3f, dst: AABBf = this) =
        union(point.x, point.y, point.z, dst)

    fun union(x: Float, y: Float, z: Float, dst: AABBf = this): AABBf {
        dst.minX = min(minX, x)
        dst.minY = min(minY, y)
        dst.minZ = min(minZ, z)
        dst.maxX = max(maxX, x)
        dst.maxY = max(maxY, y)
        dst.maxZ = max(maxZ, z)
        return this
    }

    fun testPoint(v: Vector3f): Boolean {
        return v.x in minX..maxX && v.y in minY..maxY && v.z in minZ..maxZ
    }

    fun testPoint(x: Float, y: Float, z: Float): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun testAABB(other: AABBf): Boolean {
        return maxX >= other.minX &&
                maxY >= other.minY &&
                maxZ >= other.minZ &&
                minX <= other.maxX &&
                minY <= other.maxY &&
                minZ <= other.maxZ
    }

    fun testRay(px: Float, py: Float, pz: Float, dx: Float, dy: Float, dz: Float) =
        isRayIntersecting(px, py, pz, 1 / dx, 1 / dy, 1 / dz)

    fun isEmpty() = minX > maxX

    fun avgX() = (minX + maxX) * 0.5f
    fun avgY() = (minY + maxY) * 0.5f
    fun avgZ() = (minZ + maxZ) * 0.5f

    fun deltaX() = maxX - minX
    fun deltaY() = maxY - minY
    fun deltaZ() = maxZ - minZ
    fun volume() = deltaX() * deltaY() * deltaZ()

    fun print() = "($minX $minY $minZ) < ($maxX $maxY $maxZ)"

    fun getMin2(dst: Vector3f = Vector3f()): Vector3f = dst.set(minX, minY, minZ)
    fun getMax2(dst: Vector3f = Vector3f()): Vector3f = dst.set(maxX, maxY, maxZ)

    // crazy... why was this not in the library???
    fun set(o: AABBf): AABBf {
        minX = o.minX
        minY = o.minY
        minZ = o.minZ
        maxX = o.maxX
        maxY = o.maxY
        maxZ = o.maxZ
        return this
    }

    fun set(o: AABBd): AABBf {
        minX = o.minX.toFloat()
        minY = o.minY.toFloat()
        minZ = o.minZ.toFloat()
        maxX = o.maxX.toFloat()
        maxY = o.maxY.toFloat()
        maxZ = o.maxZ.toFloat()
        return this
    }

    fun clear(): AABBf {
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        minZ = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        maxZ = Float.NEGATIVE_INFINITY
        return this
    }

    fun all(): AABBf {
        minX = Float.NEGATIVE_INFINITY
        minY = Float.NEGATIVE_INFINITY
        minZ = Float.NEGATIVE_INFINITY
        maxX = Float.POSITIVE_INFINITY
        maxY = Float.POSITIVE_INFINITY
        maxZ = Float.POSITIVE_INFINITY
        return this
    }

    fun allX(): AABBf {
        minX = Float.NEGATIVE_INFINITY
        maxX = Float.POSITIVE_INFINITY
        return this
    }

    fun allY(): AABBf {
        minY = Float.NEGATIVE_INFINITY
        maxY = Float.POSITIVE_INFINITY
        return this
    }

    fun allZ(): AABBf {
        minZ = Float.NEGATIVE_INFINITY
        maxZ = Float.POSITIVE_INFINITY
        return this
    }

    fun intersect(other: AABBf, dst: AABBf = this): AABBf {
        dst.minX = max(minX, other.minX)
        dst.minY = max(minY, other.minY)
        dst.minZ = max(minZ, other.minZ)
        dst.maxX = min(maxX, other.maxX)
        dst.maxY = min(maxY, other.maxY)
        dst.maxZ = min(maxZ, other.maxZ)
        return dst
    }

    fun transform(m: Matrix4f, dst: AABBf = this): AABBf {
        if (isEmpty()) return dst.clear()
        val dx = maxX - minX
        val dy = maxY - minY
        val dz = maxZ - minZ
        var minx = Float.POSITIVE_INFINITY
        var miny = Float.POSITIVE_INFINITY
        var minz = Float.POSITIVE_INFINITY
        var maxx = Float.NEGATIVE_INFINITY
        var maxy = Float.NEGATIVE_INFINITY
        var maxz = Float.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = minX + (i and 1).toFloat() * dx
            val y = minY + (i shr 1 and 1).toFloat() * dy
            val z = minZ + (i shr 2 and 1).toFloat() * dz
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }

    /**
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun transform(m: Matrix4x3d, dst: AABBd): AABBd {
        if (isEmpty()) return dst.clear()
        val mx = minX.toDouble()
        val my = minY.toDouble()
        val mz = minZ.toDouble()
        val dx = this.maxX - mx
        val dy = this.maxY - my
        val dz = this.maxZ - mz
        var minx = Double.POSITIVE_INFINITY
        var miny = Double.POSITIVE_INFINITY
        var minz = Double.POSITIVE_INFINITY
        var maxx = Double.NEGATIVE_INFINITY
        var maxy = Double.NEGATIVE_INFINITY
        var maxz = Double.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = mx + (i and 1).toDouble() * dx
            val y = my + ((i shr 1) and 1).toDouble() * dy
            val z = mz + ((i shr 2) and 1).toDouble() * dz
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }

    /**
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun transformUnion(m: Matrix4x3d, base: AABBd, dst: AABBd = base): AABBd {
        if (isEmpty()) return dst.set(base)
        val mx = minX.toDouble()
        val my = minY.toDouble()
        val mz = minZ.toDouble()
        val dx = this.maxX - mx
        val dy = this.maxY - my
        val dz = this.maxZ - mz
        var minx = base.minX
        var miny = base.minY
        var minz = base.minZ
        var maxx = base.maxX
        var maxy = base.maxY
        var maxz = base.maxZ
        for (i in 0..7) {
            val x = mx + (i and 1).toDouble() * dx
            val y = my + ((i shr 1) and 1).toDouble() * dy
            val z = mz + ((i shr 2) and 1).toDouble() * dz
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }

    /**
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun transformUnion(m: Matrix4x3f, base: AABBf, dst: AABBf = base): AABBf {
        if (isEmpty()) return dst.set(base)
        val mx = minX
        val my = minY
        val mz = minZ
        val dx = this.maxX - mx
        val dy = this.maxY - my
        val dz = this.maxZ - mz
        var minx = base.minX
        var miny = base.minY
        var minz = base.minZ
        var maxx = base.maxX
        var maxy = base.maxY
        var maxz = base.maxZ
        for (i in 0..7) {
            val x = mx + (i and 1) * dx
            val y = my + ((i shr 1) and 1) * dy
            val z = mz + ((i shr 2) and 1) * dz
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }

    /**
     * transforms this matrix, and places the result in dst
     * */
    fun transform(m: Matrix4x3f, dst: AABBf = this): AABBf {
        if (isEmpty()) return dst.clear()
        val mx = minX
        val my = minY
        val mz = minZ
        val dx = this.maxX - mx
        val dy = this.maxY - my
        val dz = this.maxZ - mz
        var minx = Float.POSITIVE_INFINITY
        var miny = Float.POSITIVE_INFINITY
        var minz = Float.POSITIVE_INFINITY
        var maxx = Float.NEGATIVE_INFINITY
        var maxy = Float.NEGATIVE_INFINITY
        var maxz = Float.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = mx + (i and 1) * dx
            val y = my + ((i shr 1) and 1) * dy
            val z = mz + ((i shr 2) and 1) * dz
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }

    fun transformProject(m: Matrix4f, dst: AABBf = this): AABBf {
        if (isEmpty()) return dst.clear()
        val mx = minX
        val my = minY
        val mz = minZ
        val xx = maxX
        val xy = maxY
        val xz = maxZ
        var minx = Float.POSITIVE_INFINITY
        var miny = Float.POSITIVE_INFINITY
        var minz = Float.POSITIVE_INFINITY
        var maxx = Float.NEGATIVE_INFINITY
        var maxy = Float.NEGATIVE_INFINITY
        var maxz = Float.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = if ((i.and(1) != 0)) xx else mx
            val y = if ((i.and(2) != 0)) xy else my
            val z = if ((i.and(4) != 0)) xz else mz
            val tw = m.m03 * x + m.m13 * y + m.m23 * z + m.m33
            val tx = (m.m00 * x + m.m10 * y + m.m20 * z + m.m30) / tw
            val ty = (m.m01 * x + m.m11 * y + m.m21 * z + m.m31) / tw
            val tz = (m.m02 * x + m.m12 * y + m.m22 * z + m.m32) / tw
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }

    /**
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun transformProjectUnion(m: Matrix4f, base: AABBf, dst: AABBf = base): AABBf {
        if (isEmpty()) return dst.set(base)
        val mx = minX
        val my = minY
        val mz = minZ
        val xx = maxX
        val xy = maxY
        val xz = maxZ
        var minx = base.minX
        var miny = base.minY
        var minz = base.minZ
        var maxx = base.maxX
        var maxy = base.maxY
        var maxz = base.maxZ
        for (i in 0..7) {
            val x = if ((i.and(1) != 0)) xx else mx
            val y = if ((i.and(2) != 0)) xy else my
            val z = if ((i.and(4) != 0)) xz else mz
            val tw = m.m03 * x + m.m13 * y + m.m23 * z + m.m33
            val tx = (m.m00 * x + m.m10 * y + m.m20 * z + m.m30) / tw
            val ty = (m.m01 * x + m.m11 * y + m.m21 * z + m.m31) / tw
            val tz = (m.m02 * x + m.m12 * y + m.m22 * z + m.m32) / tw
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }


    /**
     * transforms this aabb, then unions it with base, and places the result in dst
     * */
    fun transformUnion(transform: Matrix4x3d, base: AABBd, scale: Double, dst: AABBd = base): AABBd {
        if (isEmpty()) return dst.set(base)
        val mx = minX.toDouble() * scale
        val my = minY.toDouble() * scale
        val mz = minZ.toDouble() * scale
        val xx = maxX * scale
        val xy = maxY * scale
        val xz = maxZ * scale
        var minx = base.minX
        var miny = base.minY
        var minz = base.minZ
        var maxx = base.maxX
        var maxy = base.maxY
        var maxz = base.maxZ
        for (i in 0..7) {
            val x = if ((i.and(1) != 0)) xx else mx
            val y = if ((i.and(2) != 0)) xy else my
            val z = if ((i.and(4) != 0)) xz else mz
            val tx = transform.m00 * x + transform.m10 * y + transform.m20 * z + transform.m30
            val ty = transform.m01 * x + transform.m11 * y + transform.m21 * z + transform.m31
            val tz = transform.m02 * x + transform.m12 * y + transform.m22 * z + transform.m32
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dst.minX = minx
        dst.minY = miny
        dst.minZ = minz
        dst.maxX = maxx
        dst.maxY = maxy
        dst.maxZ = maxz
        return dst
    }

    /**
     * a test whether the line start-end crosses the aabb
     * end may be far away, and it still works
     * start should not be far away -> order matters!
     * can deliver a few false-positives (in favor of not delivering false-negatives)
     * */
    fun testLine(start: Vector3f, dir: Vector3f, length: Float): Boolean {
        if (isEmpty()) return false
        // no!!!, see double version
        // bring the line towards the aabb center, so the JOML check actually works correctly for huge numbers
        /*var ox = (aabb.minX + aabb.maxX) * 0.5f
        var oy = (aabb.minY + aabb.maxY) * 0.5f
        var oz = (aabb.minZ + aabb.maxZ) * 0.5f
        if (ox.isNaN()) ox = start.x
        if (oy.isNaN()) oy = start.y
        if (oz.isNaN()) oz = start.z*/
        val c = 0f // linePointTFactor(start, dir, ox, oy, oz)
        val sx = start.x + c * dir.x
        val sy = start.y + c * dir.y
        val sz = start.z + c * dir.z
        return testRay(sx, sy, sz, dir.x, dir.y, dir.z) &&
                distanceSquared(start) <= dir.lengthSquared() * length * length
    }

    fun distance(v: Vector3f): Float {
        return sqrt(distanceSquared(v))
    }

    fun distanceSquared(v: Vector3f): Float {
        val dx = max(max(minX - v.x, v.x - maxX), 0f)
        val dy = max(max(minY - v.y, v.y - maxY), 0f)
        val dz = max(max(minZ - v.z, v.z - maxZ), 0f)
        return dx * dx + dy * dy + dz * dz
    }

    fun testLine(start: Vector3f, end: Vector3f): Boolean {
        if (isEmpty()) return false
        return testRay(start.x, start.y, start.z, end.x - start.x, end.y - start.y, end.z - start.z) &&
                distanceSquared(start) <= start.distanceSquared(end)
    }

    fun testLine(
        start: Vector3f,
        end: Vector3f,
        radiusAtOrigin: Float,
        radiusPerUnit: Float
    ): Boolean {
        if (isEmpty()) return false
        // todo respect extra radius & move ray towards aabb
        return testRay(start.x, start.y, start.z, end.x - start.x, end.y - start.y, end.z - start.z) &&
                distanceSquared(start) <= start.distanceSquared(end)
    }

    fun testLine(
        start: Vector3f,
        dir: Vector3f,
        radiusAtOrigin: Float,
        radiusPerUnit: Float,
        maxDistance: Float,
    ): Boolean {
        if (isEmpty()) return false
        /*
        // todo respect extra radius & move ray towards aabb
        // todo or general aabb-cone intersection
        for (i in 0 until 8) {
            val ox = if (i.and(1) != 0) aabb.minX else aabb.maxX
            val oy = if (i.and(2) != 0) aabb.minY else aabb.maxY
            val oz = if (i.and(4) != 0) aabb.minZ else aabb.maxZ
        }
        val c = clamp(linePointTFactor(start, dir, ox, oy, oz), 0f, maxDistance)
        val sx = start.x + c * dir.x
        val sy = start.y + c * dir.y
        val sz = start.z + c * dir.z*/
        return testRay(start.x, start.y, start.z, dir.x, dir.y, dir.z) &&
                distanceSquared(start) <= maxDistance * maxDistance
    }

    fun scale(sx: Float, sy: Float = sx, sz: Float = sx) {
        minX *= sx
        minY *= sy
        minZ *= sz
        maxX *= sx
        maxY *= sy
        maxZ *= sz
    }

    fun collideFront(pos: Vector3f, dir: Vector3f): Float {
        val dx = (if (dir.x < 0f) maxX else minX) - pos.x
        val dy = (if (dir.y < 0f) maxY else minY) - pos.y
        val dz = (if (dir.z < 0f) maxZ else minZ) - pos.z
        return max(max(dx / dir.x, dy / dir.y), dz / dir.z)
    }

    fun collideBack(pos: Vector3f, dir: Vector3f): Float {
        val dx = (if (dir.x > 0f) maxX else minX) - pos.x
        val dy = (if (dir.y > 0f) maxY else minY) - pos.y
        val dz = (if (dir.z > 0f) maxZ else minZ) - pos.z
        return min(min(dx / dir.x, dy / dir.y), dz / dir.z)
    }

    fun maxDim(): Int {
        val dx = deltaX()
        val dy = deltaY()
        val dz = deltaZ()
        return when {
            dx >= max(dy, dz) -> 0
            dy >= dz -> 1
            else -> 2
        }
    }

    fun toDouble(dst: AABBd = AABBd()): AABBd {
        return dst
            .setMin(minX.toDouble(), minY.toDouble(), minZ.toDouble())
            .setMax(maxX.toDouble(), maxY.toDouble(), maxZ.toDouble())
    }

    fun addMargin(r: Float) {
        minX -= r
        minY -= r
        minZ -= r
        maxX += r
        maxY += r
        maxZ += r
    }

    fun isRayIntersecting(
        rayOrigin: Vector3f,
        invRayDirection: Vector3f,
        maxDistance: Float = Float.POSITIVE_INFINITY
    ) = isRayIntersecting(
        rayOrigin.x, rayOrigin.y, rayOrigin.z,
        invRayDirection.x, invRayDirection.y, invRayDirection.z,
        maxDistance
    )

    fun isRayIntersecting(
        rx: Float, ry: Float, rz: Float,
        rdx: Float, rdy: Float, rdz: Float,
        maxDistance: Float = Float.POSITIVE_INFINITY
    ): Boolean {
        val sx0 = (minX - rx) * rdx
        val sy0 = (minY - ry) * rdy
        val sz0 = (minZ - rz) * rdz
        val sx1 = (maxX - rx) * rdx
        val sy1 = (maxY - ry) * rdy
        val sz1 = (maxZ - rz) * rdz
        val nearX = min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = min(farX, min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return far >= near && near < maxDistance
    }

    fun whereIsRayIntersecting(rayOrigin: Vector3f, invRayDirection: Vector3f): Float {
        val rx = rayOrigin.x
        val ry = rayOrigin.y
        val rz = rayOrigin.z
        val rdx = invRayDirection.x
        val rdy = invRayDirection.y
        val rdz = invRayDirection.z
        val sx0 = (minX - rx) * rdx
        val sy0 = (minY - ry) * rdy
        val sz0 = (minZ - rz) * rdz
        val sx1 = (maxX - rx) * rdx
        val sy1 = (maxY - ry) * rdy
        val sz1 = (maxZ - rz) * rdz
        val nearX = min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = min(farX, min(farY, farZ))
        val near = max(max(nearX, max(nearY, nearZ)), 0f)
        return if (far >= near) near else Float.POSITIVE_INFINITY
    }

}