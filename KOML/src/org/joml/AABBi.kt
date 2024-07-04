package org.joml

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Suppress("unused")
class AABBi(
    var minX: Int, var minY: Int, var minZ: Int,
    var maxX: Int, var maxY: Int, var maxZ: Int
) : Vector() {

    constructor(base: AABBi) : this(base.minX, base.minY, base.minZ, base.maxX, base.maxY, base.maxZ)
    constructor(min: Int, max: Int) : this(min, min, min, max, max, max)
    constructor() : this(Int.MAX_VALUE, Int.MIN_VALUE)
    constructor(base: AABBf) : this() {
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
        val vf = v.toInt()
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

    fun setMin(v: Vector3i): AABBi = setMin(v.x, v.y, v.z)
    fun setMax(v: Vector3i): AABBi = setMax(v.x, v.y, v.z)

    fun setMin(x: Int, y: Int, z: Int): AABBi {
        minX = x
        minY = y
        minZ = z
        return this
    }

    fun setMax(x: Int, y: Int, z: Int): AABBi {
        maxX = x
        maxY = y
        maxZ = z
        return this
    }

    fun getMin(dim: Int) =
        if (dim == 0) minX
        else (if (dim == 1) minY else minZ)

    fun getMax(dim: Int) =
        if (dim == 0) maxX
        else (if (dim == 1) maxY else maxZ)

    fun union(other: AABBi, dst: AABBi = this): AABBi {
        dst.minX = min(minX, other.minX)
        dst.minY = min(minY, other.minY)
        dst.minZ = min(minZ, other.minZ)
        dst.maxX = max(maxX, other.maxX)
        dst.maxY = max(maxY, other.maxY)
        dst.maxZ = max(maxZ, other.maxZ)
        return dst
    }

    fun union(other: AABBf, dst: AABBi = this): AABBi {
        dst.minX = min(minX, other.minX.toInt())
        dst.minY = min(minY, other.minY.toInt())
        dst.minZ = min(minZ, other.minZ.toInt())
        dst.maxX = max(maxX, other.maxX.toInt())
        dst.maxY = max(maxY, other.maxY.toInt())
        dst.maxZ = max(maxZ, other.maxZ.toInt())
        return dst
    }

    fun union(other: Vector3i, dst: AABBi = this) =
        union(other.x, other.y, other.z, dst)

    fun union(x: Int, y: Int, z: Int, dst: AABBi = this): AABBi {
        dst.minX = min(minX, x)
        dst.minY = min(minY, y)
        dst.minZ = min(minZ, z)
        dst.maxX = max(maxX, x)
        dst.maxY = max(maxY, y)
        dst.maxZ = max(maxZ, z)
        return dst
    }

    fun testPoint(v: Vector3i): Boolean {
        return v.x in minX..maxX && v.y in minY..maxY && v.z in minZ..maxZ
    }

    fun testPoint(x: Int, y: Int, z: Int): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun testAABB(other: AABBi): Boolean {
        return maxX >= other.minX && maxY >= other.minY && maxZ >= other.minZ &&
                minX <= other.maxX && minY <= other.maxY && minZ <= other.maxZ
    }

    fun testAABB(
        otherMinX: Int, otherMinY: Int, otherMinZ: Int,
        otherMaxX: Int, otherMaxY: Int, otherMaxZ: Int,
    ): Boolean {
        return maxX >= otherMinX && maxY >= otherMinY && maxZ >= otherMinZ &&
                minX <= otherMaxX && minY <= otherMaxY && minZ <= otherMaxZ
    }

    fun testRay(px: Double, py: Double, pz: Double, dx: Double, dy: Double, dz: Double) =
        isRayIntersecting(px, py, pz, 1 / dx, 1 / dy, 1 / dz)

    fun isEmpty(): Boolean = minX > maxX

    val centerX: Double get() = (minX + maxX) * 0.5
    val centerY: Double get() = (minY + maxY) * 0.5
    val centerZ: Double get() = (minZ + maxZ) * 0.5

    val deltaX: Int get() = maxX - minX
    val deltaY: Int get() = maxY - minY
    val deltaZ: Int get() = maxZ - minZ
    val volume: Int get() = deltaX * deltaY * deltaZ

    fun print(): String = "($minX $minY $minZ) < ($maxX $maxY $maxZ)"

    fun getMin(dst: Vector3i = Vector3i()): Vector3i = dst.set(minX, minY, minZ)
    fun getMax(dst: Vector3i = Vector3i()): Vector3i = dst.set(maxX, maxY, maxZ)

    fun set(o: AABBi): AABBi {
        minX = o.minX
        minY = o.minY
        minZ = o.minZ
        maxX = o.maxX
        maxY = o.maxY
        maxZ = o.maxZ
        return this
    }

    fun clear(): AABBi {
        minX = Int.MAX_VALUE
        minY = Int.MAX_VALUE
        minZ = Int.MAX_VALUE
        maxX = Int.MIN_VALUE
        maxY = Int.MIN_VALUE
        maxZ = Int.MIN_VALUE
        return this
    }

    fun all(): AABBi {
        minX = Int.MIN_VALUE
        minY = Int.MIN_VALUE
        minZ = Int.MIN_VALUE
        maxX = Int.MAX_VALUE
        maxY = Int.MAX_VALUE
        maxZ = Int.MAX_VALUE
        return this
    }

    fun set(src: AABBf): AABBi {
        minX = src.minX.toInt()
        minY = src.minY.toInt()
        minZ = src.minZ.toInt()
        maxX = src.maxX.toInt()
        maxY = src.maxY.toInt()
        maxZ = src.maxZ.toInt()
        return this
    }

    fun intersect(o: AABBi, dst: AABBi = this): AABBi {
        dst.minX = max(minX, o.minX)
        dst.minY = max(minY, o.minY)
        dst.minZ = max(minZ, o.minZ)
        dst.maxX = min(maxX, o.maxX)
        dst.maxY = min(maxY, o.maxY)
        dst.maxZ = min(maxZ, o.maxZ)
        return dst
    }

    fun intersectionVolume(o: AABBi): Long {
        val dx = max(0, min(maxX, o.maxX) - max(minX, o.minX)).toLong()
        val dy = max(0, min(maxY, o.maxY) - max(minY, o.minY)).toLong()
        val dz = max(0, min(maxZ, o.maxZ) - max(minZ, o.minZ)).toLong()
        return dx * dy * dz
    }

    /**
     * a test whether the line start-end crosses the aabb
     * end may be far away, and it still works
     * start should not be far away -> order matters!
     * can deliver a few false-positives (in favor of not delivering false-negatives)
     * */
    fun testLine(start: Vector3d, dir: Vector3d, length: Double): Boolean {
        if (isEmpty()) return false
        // bring the line towards the aabb center, so the JOML check actually works correctly for huge numbers
        // -> no, that's incorrect!!!
        // to do at maximum, project the point to the front of the bbx
        /*var ox = (aabb.minX + aabb.maxX) * 0.5
        var oy = (aabb.minY + aabb.maxY) * 0.5
        var oz = (aabb.minZ + aabb.maxZ) * 0.5
        if (ox.isNaN()) ox = start.x // can happen if aabb is -Inf..Inf
        if (oy.isNaN()) oy = start.y
        if (oz.isNaN()) oz = start.z*/
        val c = 0.0 // linePointTFactor(start, dir, ox, oy, oz)
        val sx = start.x + c * dir.x
        val sy = start.y + c * dir.y
        val sz = start.z + c * dir.z
        // println("$sx $sy $sz from $start, $dir X $aabb -> ${aabb.testRay(sx,sy,sz,dir.x,dir.y,dir.z)} && ${distanceSquared(aabb, start)} <= ${dir.lengthSquared()} * $length² = ${dir.lengthSquared() * length * length}")
        return testRay(sx, sy, sz, dir.x, dir.y, dir.z) &&
                distanceSquared(start) <= dir.lengthSquared() * length * length
    }

    fun distance(v: Vector3d): Double {
        return sqrt(distanceSquared(v))
    }

    fun distanceSquared(v: Vector3d): Double {
        val dx = max(max(minX - v.x, v.x - maxX), 0.0)
        val dy = max(max(minY - v.y, v.y - maxY), 0.0)
        val dz = max(max(minZ - v.z, v.z - maxZ), 0.0)
        return dx * dx + dy * dy + dz * dz
    }

    fun addMargin(r: Int) {
        minX -= r
        minY -= r
        minZ -= r
        maxX += r
        maxY += r
        maxZ += r
    }

    fun collideFront(pos: Vector3d, dir: Vector3d): Double {
        val dx = (if (dir.x < 0.0) maxX else minX) - pos.x
        val dy = (if (dir.y < 0.0) maxY else minY) - pos.y
        val dz = (if (dir.z < 0.0) maxZ else minZ) - pos.z
        return max(max(dx / dir.x, dy / dir.y), dz / dir.z)
    }

    fun collideBack(pos: Vector3d, dir: Vector3d): Double {
        val dx = (if (dir.x > 0.0) maxX else minX) - pos.x
        val dy = (if (dir.y > 0.0) maxY else minY) - pos.y
        val dz = (if (dir.z > 0.0) maxZ else minZ) - pos.z
        return min(min(dx / dir.x, dy / dir.y), dz / dir.z)
    }

    fun isRayIntersecting(
        rayOrigin: Vector3d,
        invRayDirection: Vector3d,
        maxDistance: Double = Double.POSITIVE_INFINITY
    ): Boolean = isRayIntersecting(
        rayOrigin.x, rayOrigin.y, rayOrigin.z,
        invRayDirection.x, invRayDirection.y, invRayDirection.z,
        maxDistance
    )

    fun isRayIntersecting(
        rx: Double, ry: Double, rz: Double,
        rdx: Double, rdy: Double, rdz: Double,
        maxDistance: Double = Double.POSITIVE_INFINITY
    ): Boolean {
        val dist = whereIsRayIntersecting(rx, ry, rz, rdx, rdy, rdz)
        return dist < maxDistance
    }

    fun whereIsRayIntersecting(rayOrigin: Vector3d, invRayDirection: Vector3d): Double {
        return whereIsRayIntersecting(
            rayOrigin.x, rayOrigin.y, rayOrigin.z,
            invRayDirection.x, invRayDirection.y, invRayDirection.z
        )
    }

    fun whereIsRayIntersecting(
        rx: Double, ry: Double, rz: Double,
        rdx: Double, rdy: Double, rdz: Double,
    ): Double {
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
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return if (far >= near) near else Double.POSITIVE_INFINITY
    }

    override fun equals(other: Any?): Boolean {
        return other is AABBi &&
                other.minX == minX && other.minY == minY && other.minZ == minZ &&
                other.maxX == maxX && other.maxY == maxY && other.maxZ == maxZ
    }
}