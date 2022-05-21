package org.joml

import kotlin.math.max
import kotlin.math.min

class AABBf(
    var minX: Float, var minY: Float, var minZ: Float,
    var maxX: Float, var maxY: Float, var maxZ: Float
) {

    constructor(base: AABBf) : this(base.minX, base.minY, base.minZ, base.maxX, base.maxY, base.maxZ)
    constructor(min: Float, max: Float) : this(min, min, min, max, max, max)
    constructor() : this(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

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

    fun union(other: Vector3f, dst: AABBf = this) =
        union(other.x, other.y, other.z, dst)

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

    fun transform(m: Matrix4fc, dest: AABBf = this): AABBf {
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
            val tx = m.m00() * x + m.m10() * y + m.m20() * z + m.m30()
            val ty = m.m01() * x + m.m11() * y + m.m21() * z + m.m31()
            val tz = m.m02() * x + m.m12() * y + m.m22() * z + m.m32()
            minx = min(tx, minx)
            miny = min(ty, miny)
            minz = min(tz, minz)
            maxx = max(tx, maxx)
            maxy = max(ty, maxy)
            maxz = max(tz, maxz)
        }
        dest.minX = minx
        dest.minY = miny
        dest.minZ = minz
        dest.maxX = maxx
        dest.maxY = maxy
        dest.maxZ = maxz
        return dest
    }

    fun testRay(px: Float, py: Float, pz: Float, dx: Float, dy: Float, dz: Float): Boolean {
        return Intersectionf.testRayAab(
            px, py, pz, dx, dy, dz,
            minX, minY, minZ, maxX, maxY, maxZ
        )
    }

}