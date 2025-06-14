package org.joml

import org.joml.JomlMath.hash
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Suppress("unused")
class AABBd(
    var minX: Double, var minY: Double, var minZ: Double,
    var maxX: Double, var maxY: Double, var maxZ: Double
) : Vector {

    constructor(base: AABBd) :
            this(base.minX, base.minY, base.minZ, base.maxX, base.maxY, base.maxZ)

    constructor(min: Double, max: Double) :
            this(min, min, min, max, max, max)

    constructor(min: Vector3d, max: Vector3d) :
            this(min.x, min.y, min.z, max.x, max.y, max.z)

    constructor() :
            this(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

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
    }

    override fun setComp(i: Int, v: Double) {
        when (i) {
            0 -> minX = v
            1 -> minY = v
            2 -> minZ = v
            3 -> maxX = v
            4 -> maxY = v
            else -> maxZ = v
        }
    }

    override fun toString(): String = "($minX,$minY,$minZ)-($maxX,$maxY,$maxZ)"

    fun setMin(v: Vector3d): AABBd = setMin(v.x, v.y, v.z)
    fun setMax(v: Vector3d): AABBd = setMax(v.x, v.y, v.z)

    fun setMin(x: Double, y: Double, z: Double): AABBd {
        minX = x
        minY = y
        minZ = z
        return this
    }

    fun setMax(x: Double, y: Double, z: Double): AABBd {
        maxX = x
        maxY = y
        maxZ = z
        return this
    }

    fun set(v: Vector3d): AABBd {
        return set(v.x, v.y, v.z)
    }

    fun set(x: Double, y: Double, z: Double): AABBd {
        return setMin(x, y, z).setMax(x, y, z)
    }

    fun getMin(dim: Int) =
        if (dim == 0) minX
        else (if (dim == 1) minY else minZ)

    fun getMax(dim: Int) =
        if (dim == 0) maxX
        else (if (dim == 1) maxY else maxZ)

    fun union(other: AABBd, dst: AABBd = this): AABBd {
        return union(
            other.minX, other.minY, other.minZ,
            other.maxX, other.maxY, other.maxZ, dst
        )
    }

    fun union(other: AABBf, dst: AABBd = this): AABBd {
        return union(
            other.minX.toDouble(), other.minY.toDouble(), other.minZ.toDouble(),
            other.maxX.toDouble(), other.maxY.toDouble(), other.maxZ.toDouble(), dst
        )
    }

    fun union(other: Vector3d, dst: AABBd = this): AABBd =
        union(other.x, other.y, other.z, dst)

    fun union(other: Vector3f, dst: AABBd = this): AABBd =
        union(other.x, other.y, other.z, dst)

    fun union(x: Float, y: Float, z: Float, dst: AABBd = this): AABBd =
        union(x.toDouble(), y.toDouble(), z.toDouble(), dst)

    fun union(x: Double, y: Double, z: Double, dst: AABBd = this): AABBd {
        return dst
            .setMin(min(minX, x), min(minY, y), min(minZ, z))
            .setMax(max(maxX, x), max(maxY, y), max(maxZ, z))
    }

    fun union(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double, dst: AABBd = this
    ): AABBd {
        return dst
            .setMin(min(minX, x0), min(minY, y0), min(minZ, z0))
            .setMax(max(maxX, x1), max(maxY, y1), max(maxZ, z1))
    }

    fun testPoint(v: Vector3d): Boolean {
        return v.x in minX..maxX && v.y in minY..maxY && v.z in minZ..maxZ
    }

    fun testPoint(x: Double, y: Double, z: Double): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun testAABB(other: AABBd): Boolean = testAABB(
        other.minX, other.minY, other.minZ,
        other.maxX, other.maxY, other.maxZ
    )

    fun testAABB(
        otherMinX: Double, otherMinY: Double, otherMinZ: Double,
        otherMaxX: Double, otherMaxY: Double, otherMaxZ: Double,
    ): Boolean {
        return maxX >= otherMinX && maxY >= otherMinY && maxZ >= otherMinZ &&
                minX <= otherMaxX && minY <= otherMaxY && minZ <= otherMaxZ
    }

    fun translate(dx: Double, dy: Double, dz: Double, dst: AABBd = this): AABBd {
        return dst.setMin(minX + dx, minY + dy, minZ + dz)
            .setMax(maxX + dx, maxY + dy, maxZ + dz)
    }

    fun transform(m: Matrix4d, dst: AABBd = this): AABBd {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = if (i.and(1) != 0) this.minX else this.maxX
            val y = if (i.and(2) != 0) this.minY else this.maxY
            val z = if (i.and(4) != 0) this.minZ else this.maxZ
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minX = min(tx, minX)
            minY = min(ty, minY)
            minZ = min(tz, minZ)
            maxX = max(tx, maxX)
            maxY = max(ty, maxY)
            maxZ = max(tz, maxZ)
        }
        return dst.setMin(minX, minY, minZ).setMax(maxX, maxY, maxZ)
    }

    fun testRay(px: Double, py: Double, pz: Double, dx: Double, dy: Double, dz: Double) =
        isRayIntersecting(px, py, pz, 1 / dx, 1 / dy, 1 / dz, 0.0, Double.POSITIVE_INFINITY)

    fun isEmpty(): Boolean = minX > maxX

    val centerX: Double get() = (minX + maxX) * 0.5
    val centerY: Double get() = (minY + maxY) * 0.5
    val centerZ: Double get() = (minZ + maxZ) * 0.5

    val deltaX: Double get() = maxX - minX
    val deltaY: Double get() = maxY - minY
    val deltaZ: Double get() = maxZ - minZ
    val maxDelta: Double get() = max(deltaX, max(deltaY, deltaZ))
    val volume: Double get() = deltaX * deltaY * deltaZ

    fun print(): String = "($minX $minY $minZ) < ($maxX $maxY $maxZ)"

    fun getMin(dst: Vector3d = Vector3d()): Vector3d = dst.set(minX, minY, minZ)
    fun getMax(dst: Vector3d = Vector3d()): Vector3d = dst.set(maxX, maxY, maxZ)

    fun getMin(dst: Vector3f): Vector3f = dst.set(minX, minY, minZ)
    fun getMax(dst: Vector3f): Vector3f = dst.set(maxX, maxY, maxZ)

    fun getCenter(dst: Vector3f): Vector3f = dst.set(centerX, centerY, centerZ)
    fun getCenter(dst: Vector3d): Vector3d = dst.set(centerX, centerY, centerZ)

    fun set(o: AABBd): AABBd {
        minX = o.minX
        minY = o.minY
        minZ = o.minZ
        maxX = o.maxX
        maxY = o.maxY
        maxZ = o.maxZ
        return this
    }

    fun clear(): AABBd {
        minX = Double.POSITIVE_INFINITY
        minY = Double.POSITIVE_INFINITY
        minZ = Double.POSITIVE_INFINITY
        maxX = Double.NEGATIVE_INFINITY
        maxY = Double.NEGATIVE_INFINITY
        maxZ = Double.NEGATIVE_INFINITY
        return this
    }

    fun all(): AABBd {
        minX = Double.NEGATIVE_INFINITY
        minY = Double.NEGATIVE_INFINITY
        minZ = Double.NEGATIVE_INFINITY
        maxX = Double.POSITIVE_INFINITY
        maxY = Double.POSITIVE_INFINITY
        maxZ = Double.POSITIVE_INFINITY
        return this
    }

    fun set(src: AABBf): AABBd {
        minX = src.minX.toDouble()
        minY = src.minY.toDouble()
        minZ = src.minZ.toDouble()
        maxX = src.maxX.toDouble()
        maxY = src.maxY.toDouble()
        maxZ = src.maxZ.toDouble()
        return this
    }

    fun intersect(o: AABBd, dst: AABBd = this): AABBd {
        dst.minX = max(minX, o.minX)
        dst.minY = max(minY, o.minY)
        dst.minZ = max(minZ, o.minZ)
        dst.maxX = min(maxX, o.maxX)
        dst.maxY = min(maxY, o.maxY)
        dst.maxZ = min(maxZ, o.maxZ)
        return dst
    }

    fun intersectionVolume(o: AABBd): Double {
        val dx = max(0.0, min(maxX, o.maxX) - max(minX, o.minX))
        val dy = max(0.0, min(maxY, o.maxY) - max(minY, o.minY))
        val dz = max(0.0, min(maxZ, o.maxZ) - max(minZ, o.minZ))
        return dx * dy * dz
    }

    fun transformAABB(transform: Matrix4x3, dst: AABBd = this): AABBd {
        return transform(transform, dst)
    }

    fun transform(trans: Matrix4x3d, dst: AABBd = this): AABBd {
        if (isEmpty()) return dst.clear()
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = if (i.and(1) != 0) this.minX else this.maxX
            val y = if (i.and(2) != 0) this.minY else this.maxY
            val z = if (i.and(4) != 0) this.minZ else this.maxZ
            val tx = trans.m00 * x + trans.m10 * y + trans.m20 * z + trans.m30
            val ty = trans.m01 * x + trans.m11 * y + trans.m21 * z + trans.m31
            val tz = trans.m02 * x + trans.m12 * y + trans.m22 * z + trans.m32
            minX = min(tx, minX)
            minY = min(ty, minY)
            minZ = min(tz, minZ)
            maxX = max(tx, maxX)
            maxY = max(ty, maxY)
            maxZ = max(tz, maxZ)
        }
        return dst.setMin(minX, minY, minZ).setMax(maxX, maxY, maxZ)
    }

    fun transform(trans: Matrix4x3, dst: AABBd = this): AABBd {
        if (isEmpty()) return dst.clear()
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = if (i.and(1) != 0) this.minX else this.maxX
            val y = if (i.and(2) != 0) this.minY else this.maxY
            val z = if (i.and(4) != 0) this.minZ else this.maxZ
            val tx = trans.m00 * x + trans.m10 * y + trans.m20 * z + trans.m30
            val ty = trans.m01 * x + trans.m11 * y + trans.m21 * z + trans.m31
            val tz = trans.m02 * x + trans.m12 * y + trans.m22 * z + trans.m32
            minX = min(tx, minX)
            minY = min(ty, minY)
            minZ = min(tz, minZ)
            maxX = max(tx, maxX)
            maxY = max(ty, maxY)
            maxZ = max(tz, maxZ)
        }
        return dst.setMin(minX, minY, minZ).setMax(maxX, maxY, maxZ)
    }

    /**
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun transformUnion(m: Matrix4x3d, base: AABBd, dst: AABBd = base): AABBd {
        if (isEmpty()) return dst.set(base)
        var minX = base.minX
        var minY = base.minY
        var minZ = base.minZ
        var maxX = base.maxX
        var maxY = base.maxY
        var maxZ = base.maxZ
        for (i in 0..7) {
            val x = if (i.and(1) != 0) this.minX else this.maxX
            val y = if (i.and(2) != 0) this.minY else this.maxY
            val z = if (i.and(4) != 0) this.minZ else this.maxZ
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minX = min(tx, minX)
            minY = min(ty, minY)
            minZ = min(tz, minZ)
            maxX = max(tx, maxX)
            maxY = max(ty, maxY)
            maxZ = max(tz, maxZ)
        }
        return dst.setMin(minX, minY, minZ).setMax(maxX, maxY, maxZ)
    }

    /**
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun transformUnion(m: Matrix4x3, base: AABBd, dst: AABBd = base): AABBd {
        if (isEmpty()) return dst.set(base)
        var minX = base.minX
        var minY = base.minY
        var minZ = base.minZ
        var maxX = base.maxX
        var maxY = base.maxY
        var maxZ = base.maxZ
        for (i in 0..7) {
            val x = if (i.and(1) != 0) this.minX else this.maxX
            val y = if (i.and(2) != 0) this.minY else this.maxY
            val z = if (i.and(4) != 0) this.minZ else this.maxZ
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minX = min(tx, minX)
            minY = min(ty, minY)
            minZ = min(tz, minZ)
            maxX = max(tx, maxX)
            maxY = max(ty, maxY)
            maxZ = max(tz, maxZ)
        }
        return dst.setMin(minX, minY, minZ).setMax(maxX, maxY, maxZ)
    }

    /**
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun transformUnion(m: Matrix4x3d, base: AABBf, dst: AABBd): AABBd {
        if (isEmpty()) return dst.set(base)
        var minX = base.minX.toDouble()
        var minY = base.minY.toDouble()
        var minZ = base.minZ.toDouble()
        var maxX = base.maxX.toDouble()
        var maxY = base.maxY.toDouble()
        var maxZ = base.maxZ.toDouble()
        for (i in 0..7) {
            val x = if (i.and(1) != 0) this.minX else this.maxX
            val y = if (i.and(2) != 0) this.minY else this.maxY
            val z = if (i.and(4) != 0) this.minZ else this.maxZ
            val tx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30
            val ty = m.m01 * x + m.m11 * y + m.m21 * z + m.m31
            val tz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32
            minX = min(tx, minX)
            minY = min(ty, minY)
            minZ = min(tz, minZ)
            maxX = max(tx, maxX)
            maxY = max(ty, maxY)
            maxZ = max(tz, maxZ)
        }
        return dst.setMin(minX, minY, minZ).setMax(maxX, maxY, maxZ)
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

    /**
     * a test whether the line start-end crosses the aabb
     * end may be far away, and it still works
     * start should not be far away -> order matters!
     * can deliver a few false-positives (in favor of not delivering false-negatives)
     * */
    fun testLine(start: Vector3d, dir: Vector3f, length: Double): Boolean {
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
        return testRay(sx, sy, sz, dir.x.toDouble(), dir.y.toDouble(), dir.z.toDouble()) &&
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

    fun addMargin(r: Double, dst: AABBd = this): AABBd {
        return addMargin(r, r, r, dst)
    }

    fun addMargin(rx: Double, ry: Double, rz: Double, dst: AABBd = this): AABBd {
        return dst
            .setMin(minX - rx, minY - ry, minZ - rz)
            .setMax(maxX + rx, maxY + ry, maxZ + rz)
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
        margin: Double, maxDistance: Double
    ): Boolean = isRayIntersecting(
        rayOrigin.x, rayOrigin.y, rayOrigin.z,
        invRayDirection.x, invRayDirection.y, invRayDirection.z,
        margin, maxDistance
    )

    fun isRayIntersecting(
        rx: Double, ry: Double, rz: Double,
        rdx: Double, rdy: Double, rdz: Double,
        margin: Double, maxDistance: Double
    ): Boolean {
        val dist = whereIsRayIntersecting(rx, ry, rz, rdx, rdy, rdz, margin)
        return dist < maxDistance
    }

    fun whereIsRayIntersecting(rayOrigin: Vector3d, invRayDirection: Vector3d, margin: Double): Double {
        return whereIsRayIntersecting(
            rayOrigin.x, rayOrigin.y, rayOrigin.z,
            invRayDirection.x, invRayDirection.y, invRayDirection.z,
            margin
        )
    }

    fun whereIsRayIntersecting(
        px: Double, py: Double, pz: Double,
        invDx: Double, invDy: Double, invDz: Double,
        margin: Double,
    ): Double {
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
        val near = max(max(nearX, max(nearY, nearZ)), 0.0)
        return if (far >= near) near else Double.POSITIVE_INFINITY
    }

    override fun equals(other: Any?): Boolean {
        return other is AABBd &&
                other.minX == minX && other.minY == minY && other.minZ == minZ &&
                other.maxX == maxX && other.maxY == maxY && other.maxZ == maxZ
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + hash(minX)
        result = 31 * result + hash(minY)
        result = 31 * result + hash(minZ)
        result = 31 * result + hash(maxX)
        result = 31 * result + hash(maxY)
        result = 31 * result + hash(maxZ)
        return result
    }
}