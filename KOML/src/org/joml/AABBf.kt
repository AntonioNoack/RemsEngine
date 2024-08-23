package org.joml

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AABBf(
    var minX: Float, var minY: Float, var minZ: Float,
    var maxX: Float, var maxY: Float, var maxZ: Float
) : Vector() {

    constructor(base: AABBf) : this(base.minX, base.minY, base.minZ, base.maxX, base.maxY, base.maxZ)
    constructor(min: Float, max: Float) : this(min, min, min, max, max, max)
    constructor() : this(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
    constructor(base: AABBd) : this() {
        set(base)
    }

    override val numComponents: Int get() = 6
    override fun getComp(i: Int): Double = when (i) {
        0 -> minX
        1 -> minY
        2 -> minZ
        3 -> maxX
        4 -> maxY
        else -> maxZ
    }.toDouble()

    override fun setComp(i: Int, v: Double) {
        val vf = v.toFloat()
        when (i) {
            0 -> minX = vf
            1 -> minY = vf
            2 -> minZ = vf
            3 -> maxX = vf
            4 -> maxY = vf
            else -> maxZ = vf
        }
    }

    override fun toString(): String = "($minX,$minY,$minZ)-($maxX,$maxY,$maxZ)"

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
        return dst
    }

    fun union(other: AABBd, dst: AABBf = this): AABBf {
        dst.minX = min(minX, other.minX.toFloat())
        dst.minY = min(minY, other.minY.toFloat())
        dst.minZ = min(minZ, other.minZ.toFloat())
        dst.maxX = max(maxX, other.maxX.toFloat())
        dst.maxY = max(maxY, other.maxY.toFloat())
        dst.maxZ = max(maxZ, other.maxZ.toFloat())
        return dst
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

    fun union(point: Vector3d, dst: AABBf = this) =
        union(point.x.toFloat(), point.y.toFloat(), point.z.toFloat(), dst)

    fun union(x: Float, y: Float, z: Float, dst: AABBf = this): AABBf {
        dst.minX = min(minX, x)
        dst.minY = min(minY, y)
        dst.minZ = min(minZ, z)
        dst.maxX = max(maxX, x)
        dst.maxY = max(maxY, y)
        dst.maxZ = max(maxZ, z)
        return this
    }

    fun testPoint(v: Vector3f): Boolean = testPoint(v.x, v.y, v.z)
    fun testPoint(x: Float, y: Float, z: Float): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun testAABB(other: AABBf): Boolean = testAABB(
        other.minX, other.minY, other.minZ,
        other.maxX, other.maxY, other.maxZ
    )

    fun testAABB(
        otherMinX: Float, otherMinY: Float, otherMinZ: Float,
        otherMaxX: Float, otherMaxY: Float, otherMaxZ: Float,
    ): Boolean {
        return maxX >= otherMinX && maxY >= otherMinY && maxZ >= otherMinZ &&
                minX <= otherMaxX && minY <= otherMaxY && minZ <= otherMaxZ
    }

    fun testRay(px: Float, py: Float, pz: Float, dx: Float, dy: Float, dz: Float): Boolean =
        isRayIntersecting(px, py, pz, 1 / dx, 1 / dy, 1 / dz, Float.POSITIVE_INFINITY)

    fun testRay(px: Float, py: Float, pz: Float, dx: Float, dy: Float, dz: Float, margin: Float): Boolean =
        isRayIntersecting(px, py, pz, 1 / dx, 1 / dy, 1 / dz, margin, Float.POSITIVE_INFINITY)

    fun isEmpty(): Boolean = minX > maxX

    val centerX: Float get() = (minX + maxX) * 0.5f
    val centerY: Float get() = (minY + maxY) * 0.5f
    val centerZ: Float get() = (minZ + maxZ) * 0.5f

    val deltaX: Float get() = maxX - minX
    val deltaY: Float get() = maxY - minY
    val deltaZ: Float get() = maxZ - minZ
    val maxDelta: Float get() = max(deltaX, max(deltaY, deltaZ))
    val volume: Float get() = deltaX * deltaY * deltaZ

    fun print(): String = "($minX $minY $minZ) < ($maxX $maxY $maxZ)"

    fun getMin(dst: Vector3f = Vector3f()): Vector3f = dst.set(minX, minY, minZ)
    fun getMax(dst: Vector3f = Vector3f()): Vector3f = dst.set(maxX, maxY, maxZ)

    fun getCenter(dst: Vector3f): Vector3f = dst.set(centerX, centerY, centerZ)
    fun getCenter(dst: Vector3d): Vector3d = dst.set(centerX, centerY, centerZ)

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

    fun intersectionVolume(o: AABBf): Float {
        val dx = max(0f, min(maxX, o.maxX) - max(minX, o.minX))
        val dy = max(0f, min(maxY, o.maxY) - max(minY, o.minY))
        val dz = max(0f, min(maxZ, o.maxZ) - max(minZ, o.minZ))
        return dx * dy * dz
    }

    fun intersect(o: AABBf, dst: AABBf = this): AABBf {
        dst.minX = max(minX, o.minX)
        dst.minY = max(minY, o.minY)
        dst.minZ = max(minZ, o.minZ)
        dst.maxX = min(maxX, o.maxX)
        dst.maxY = min(maxY, o.maxY)
        dst.maxZ = min(maxZ, o.maxZ)
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
     * transforms this matrix, and places the result in dst
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
        if (radiusAtOrigin == 0f && radiusPerUnit == 0f) return testLine(start, end)

        val dx = end.x - start.x
        val dy = end.y - start.y
        val dz = end.z - start.z

        val tanAngleSqPlusOne = radiusPerUnit * radiusPerUnit + 1f

        // from https://github.com/mosra/magnum/blob/master/src/Magnum/Math/Intersection.h#L570-L610
        val offset = radiusAtOrigin / radiusPerUnit
        val maxDistSq = start.distanceSquared(end)
        if (offset * offset < maxDistSq) {
            // to do remove dynamic allocations (replace with stack)

            // move the cone forward/back
            // to do minimum distance is needed for this offset
            val mixF = -offset / sqrt(maxDistSq)
            val sx = start.x + dx * mixF
            val sy = start.y + dy * mixF
            val sz = start.z + dz * mixF

            val cx0 = centerX - sx
            val cy0 = centerY - sy
            val cz0 = centerZ - sz
            val dir = Vector3f(dx, dy, dz).normalize()
            val extends = Vector3f(deltaX, deltaY, deltaZ).mul(0.5f)
            val ai01 = listOf(Vector3f(), Vector3f())
            for (axis in 0 until 3) {

                val z = axis
                val x = (axis + 1) % 3
                val y = (axis + 2) % 3

                val cz = if (z == 0) cx0 else if (z == 1) cy0 else cz0
                val exz = extends[z]
                val cnz = dir[z] // could be zero
                if (abs(cnz) < 1e-15f) continue

                val t0 = (cz - exz) / cnz
                val t1 = (cz + exz) / cnz

                dir.mul(t0, ai01[0])
                dir.mul(t1, ai01[1])

                val exx = extends[x]
                val exy = extends[y]
                for (closestPoint in ai01) {
                    val cpx = closestPoint[x]
                    val cx = if (x == 0) cx0 else if (x == 1) cy0 else cz0
                    closestPoint[x] = if (cpx - cx > exx) cx + exx
                    else if (cpx - cx < -exx) cx - exx
                    else cx /* Else: normal intersects within x bounds */

                    val cpy = closestPoint[y]
                    val cy = if (y == 0) cx0 else if (y == 1) cy0 else cz0
                    closestPoint[y] = if (cpy - cy > exy) cy + exy
                    else if (cpy - cy < -exy) cy - exy
                    else cy /* Else: normal intersects within Y bounds */

                    /* Found a point in cone and aabb */
                    if (closestPoint.distanceSquared(sx, sy, sz) < maxDistSq &&
                        pointCone(closestPoint, dir, tanAngleSqPlusOne)
                    ) return true
                }
            }
            return false
        } else {
            // respect extra radius & move ray towards aabb
            val th = radiusAtPoint(start, radiusAtOrigin, radiusPerUnit, dx, dy, dz)
            return testRay(start.x, start.y, start.z, dx, dy, dz, th) &&
                    distanceSquared(start) <= start.distanceSquared(end)
        }
    }

    private fun pointCone(point: Vector3f, coneNormal: Vector3f, tanAngleSqPlusOne: Float): Boolean {
        val lenA = coneNormal.dot(point)
        return lenA >= 0f && point.lengthSquared() <= lenA * lenA * tanAngleSqPlusOne
    }

    fun testLine(
        start: Vector3f, dir: Vector3f,
        radiusAtOrigin: Float, radiusPerUnit: Float,
        maxDistance: Float,
    ): Boolean {
        if (isEmpty()) return false
        val th = radiusAtPoint(start, radiusAtOrigin, radiusPerUnit, dir.x, dir.y, dir.z)
        return testRay(start.x, start.y, start.z, dir.x, dir.y, dir.z, th) &&
                distanceSquared(start) <= maxDistance * maxDistance
    }

    private fun radiusAtPoint(
        start: Vector3f,
        radiusAtOrigin: Float,
        radiusPerUnit: Float,
        dx: Float, dy: Float, dz: Float
    ): Float {
        var radiusAtPoint = max(radiusAtOrigin, 0f)
        if (radiusPerUnit != 0f) {
            // should be the maximum/minimum depending on whether the cone gets thicker or thinner
            // we use the box center for simplicity for now
            val cx = (minX + maxX) * 0.5f - start.x
            val cy = (minY + maxY) * 0.5f - start.y
            val cz = (minZ + maxZ) * 0.5f - start.z
            val cos01 = max(Vector3f.angleCos(dx, dy, dz, cx, cy, cz), 0f)
            val radiusPointDist = cos01 * Vector3f.length(dx, dy, dz)
            radiusAtPoint = max(radiusAtOrigin + radiusPointDist * radiusPerUnit, 0f)
        }
        return radiusAtPoint
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
        val dx = deltaX
        val dy = deltaY
        val dz = deltaZ
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
        maxDistance: Float
    ): Boolean = isRayIntersecting(
        rayOrigin.x, rayOrigin.y, rayOrigin.z,
        invRayDirection.x, invRayDirection.y, invRayDirection.z,
        maxDistance
    )

    fun isRayIntersecting(
        px: Float, py: Float, pz: Float,
        invDx: Float, invDy: Float, invDz: Float,
        maxDistance: Float
    ): Boolean = isRayIntersecting(px, py, pz, invDx, invDy, invDz, 0f, maxDistance)

    fun isRayIntersecting(
        px: Float, py: Float, pz: Float,
        invDx: Float, invDy: Float, invDz: Float,
        margin: Float, maxDistance: Float
    ): Boolean = whereIsRayIntersecting(px, py, pz, invDx, invDy, invDz, margin) < maxDistance

    fun whereIsRayIntersecting(rayOrigin: Vector3f, invRayDirection: Vector3f, margin: Float): Float {
        return whereIsRayIntersecting(
            rayOrigin.x, rayOrigin.y, rayOrigin.z,
            invRayDirection.x, invRayDirection.y, invRayDirection.z,
            margin
        )
    }

    fun whereIsRayIntersecting(
        px: Float, py: Float, pz: Float,
        invDx: Float, invDy: Float, invDz: Float,
        margin: Float,
    ): Float {
        val sx0 = (minX - margin - px) * invDx
        val sy0 = (minY - margin - py) * invDy
        val sz0 = (minZ - margin - pz) * invDz
        val sx1 = (maxX + margin - px) * invDx
        val sy1 = (maxY + margin - py) * invDy
        val sz1 = (maxZ + margin - pz) * invDz
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

    override fun equals(other: Any?): Boolean {
        return other is AABBf &&
                other.minX == minX && other.minY == minY && other.minZ == minZ &&
                other.maxX == maxX && other.maxY == maxY && other.maxZ == maxZ
    }
}