package org.joml

import kotlin.math.max
import kotlin.math.min

class AABBd(
    var minX: Double, var minY: Double, var minZ: Double,
    var maxX: Double, var maxY: Double, var maxZ: Double
) {

    constructor(base: AABBd) : this(base.minX, base.minY, base.minZ, base.maxX, base.maxY, base.maxZ)
    constructor(min: Double, max: Double) : this(min, min, min, max, max, max)
    constructor() : this(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

    fun setMin(v: Vector3d) =
        setMin(v.x, v.y, v.z)

    fun setMax(v: Vector3d) =
        setMax(v.x, v.y, v.z)

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

    fun getMin(dim: Int) =
        if (dim == 0) minX
        else (if (dim == 1) minY else minZ)

    fun getMax(dim: Int) =
        if (dim == 0) maxX
        else (if (dim == 1) maxY else maxZ)


    fun union(other: AABBd, dst: AABBd = this): AABBd {
        dst.minX = min(minX, other.minX)
        dst.minY = min(minY, other.minY)
        dst.minZ = min(minZ, other.minZ)
        dst.maxX = max(maxX, other.maxX)
        dst.maxY = max(maxY, other.maxY)
        dst.maxZ = max(maxZ, other.maxZ)
        return this
    }

    fun union(other: Vector3d, dst: AABBd = this) =
        union(other.x, other.y, other.z, dst)

    fun union(x: Double, y: Double, z: Double, dst: AABBd = this): AABBd {
        dst.minX = min(minX, x)
        dst.minY = min(minY, y)
        dst.minZ = min(minZ, z)
        dst.maxX = max(maxX, x)
        dst.maxY = max(maxY, y)
        dst.maxZ = max(maxZ, z)
        return this
    }

    fun testPoint(v: Vector3d): Boolean {
        return v.x in minX..maxX && v.y in minY..maxY && v.z in minZ..maxZ
    }

    fun testPoint(x: Double, y: Double, z: Double): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun testAABB(other: AABBd): Boolean {
        return maxX >= other.minX &&
                maxY >= other.minY &&
                maxZ >= other.minZ &&
                minX <= other.maxX &&
                minY <= other.maxY &&
                minZ <= other.maxZ
    }


    fun transform(m: Matrix4dc, dest: AABBd = this): AABBd {
        val dx = maxX - minX
        val dy = maxY - minY
        val dz = maxZ - minZ
        var minx = Double.POSITIVE_INFINITY
        var miny = Double.POSITIVE_INFINITY
        var minz = Double.POSITIVE_INFINITY
        var maxx = Double.NEGATIVE_INFINITY
        var maxy = Double.NEGATIVE_INFINITY
        var maxz = Double.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x = minX + (i and 1).toDouble() * dx
            val y = minY + (i shr 1 and 1).toDouble() * dy
            val z = minZ + (i shr 2 and 1).toDouble() * dz
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

    fun testRay(px: Double, py: Double, pz: Double, dx: Double, dy: Double, dz: Double): Boolean {
        return Intersectiond.testRayAab(
            px, py, pz, dx, dy, dz,
            minX, minY, minZ, maxX, maxY, maxZ
        )
    }

}