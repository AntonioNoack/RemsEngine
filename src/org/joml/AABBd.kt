package org.joml

import me.anno.maths.Maths
import me.anno.utils.types.Floats.f3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
class AABBd(
    var minX: Double, var minY: Double, var minZ: Double,
    var maxX: Double, var maxY: Double, var maxZ: Double
) {

    constructor(base: AABBd) : this(base.minX, base.minY, base.minZ, base.maxX, base.maxY, base.maxZ)
    constructor(min: Double, max: Double) : this(min, min, min, max, max, max)
    constructor() : this(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

    override fun toString() = "" +
            "(${minX.f3()},${minY.f3()},${minZ.f3()})-" +
            "(${maxX.f3()},${maxY.f3()},${maxZ.f3()})"

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

    fun isEmpty() = minX > maxX

    fun avgX() = (minX + maxX) * 0.5
    fun avgY() = (minY + maxY) * 0.5
    fun avgZ() = (minZ + maxZ) * 0.5

    fun deltaX() = maxX - minX
    fun deltaY() = maxY - minY
    fun deltaZ() = maxZ - minZ
    fun volume() = deltaX() * deltaY() * deltaZ()

    fun print() = "($minX $minY $minZ) < ($maxX $maxY $maxZ)"

    fun getMin2(dst: Vector3d = Vector3d()): Vector3d = dst.set(minX, minY, minZ)
    fun getMax2(dst: Vector3d = Vector3d()): Vector3d = dst.set(maxX, maxY, maxZ)

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

    fun transformAABB(transform: Matrix4x3d, dst: AABBd = this): AABBd {
        return transform(transform, dst)
    }

    fun transform(trans: Matrix4x3d, dst: AABBd): AABBd {
        val dx: Double = this.maxX - this.minX
        val dy: Double = this.maxY - this.minY
        val dz: Double = this.maxZ - this.minZ
        var minx = Double.POSITIVE_INFINITY
        var miny = Double.POSITIVE_INFINITY
        var minz = Double.POSITIVE_INFINITY
        var maxx = Double.NEGATIVE_INFINITY
        var maxy = Double.NEGATIVE_INFINITY
        var maxz = Double.NEGATIVE_INFINITY
        for (i in 0..7) {
            val x: Double = this.minX + (i and 1).toDouble() * dx
            val y: Double = this.minY + ((i shr 1) and 1).toDouble() * dy
            val z: Double = this.minZ + ((i shr 2) and 1).toDouble() * dz
            val tx = trans.m00() * x + trans.m10() * y + trans.m20() * z + trans.m30()
            val ty = trans.m01() * x + trans.m11() * y + trans.m21() * z + trans.m31()
            val tz = trans.m02() * x + trans.m12() * y + trans.m22() * z + trans.m32()
            minx = Math.min(tx, minx)
            miny = Math.min(ty, miny)
            minz = Math.min(tz, minz)
            maxx = Math.max(tx, maxx)
            maxy = Math.max(ty, maxy)
            maxz = Math.max(tz, maxz)
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
    fun transformUnion(m: Matrix4x3d, base: AABBd, dst: AABBd): AABBd {
        val dx: Double = this.maxX - this.minX
        val dy: Double = this.maxY - this.minY
        val dz: Double = this.maxZ - this.minZ
        var minx = base.minX
        var miny = base.minY
        var minz = base.minZ
        var maxx = base.maxX
        var maxy = base.maxY
        var maxz = base.maxZ
        for (i in 0..7) {
            val x: Double = this.minX + (i and 1).toDouble() * dx
            val y: Double = this.minY + (i shr 1 and 1).toDouble() * dy
            val z: Double = this.minZ + (i shr 2 and 1).toDouble() * dz
            val tx = m.m00() * x + m.m10() * y + m.m20() * z + m.m30()
            val ty = m.m01() * x + m.m11() * y + m.m21() * z + m.m31()
            val tz = m.m02() * x + m.m12() * y + m.m22() * z + m.m32()
            minx = Math.min(tx, minx)
            miny = Math.min(ty, miny)
            minz = Math.min(tz, minz)
            maxx = Math.max(tx, maxx)
            maxy = Math.max(ty, maxy)
            maxz = Math.max(tz, maxz)
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
    fun transformUnion(m: Matrix4x3d, base: AABBf, dst: AABBd): AABBd {
        val mx = minX
        val my = minY
        val mz = minZ
        val dx = this.maxX - mx
        val dy = this.maxY - my
        val dz = this.maxZ - mz
        var minx = base.minX.toDouble()
        var miny = base.minY.toDouble()
        var minz = base.minZ.toDouble()
        var maxx = base.maxX.toDouble()
        var maxy = base.maxY.toDouble()
        var maxz = base.maxZ.toDouble()
        for (i in 0..7) {
            val x = mx + (i and 1).toDouble() * dx
            val y = my + ((i shr 1) and 1).toDouble() * dy
            val z = mz + ((i shr 2) and 1).toDouble() * dz
            val tx = m.m00() * x + m.m10() * y + m.m20() * z + m.m30()
            val ty = m.m01() * x + m.m11() * y + m.m21() * z + m.m31()
            val tz = m.m02() * x + m.m12() * y + m.m22() * z + m.m32()
            minx = Math.min(tx, minx)
            miny = Math.min(ty, miny)
            minz = Math.min(tz, minz)
            maxx = Math.max(tx, maxx)
            maxy = Math.max(ty, maxy)
            maxz = Math.max(tz, maxz)
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

    // pseudo-distance from aabb to v
    fun distanceSquared(v: Vector3d): Double {
        if (testPoint(v)) return 0.0
        val cx = (minX + maxX) * 0.5
        val cy = (minY + maxY) * 0.5
        val cz = (minZ + maxZ) * 0.5
        val ex = (maxX - minX) * 0.5
        val ey = (maxY - minY) * 0.5
        val ez = (maxZ - minZ) * 0.5
        return if (ex.isFinite() && ey.isFinite() && ez.isFinite()) {
            val dx = max(abs(cx - v.x) - ex, 0.0)
            val dy = max(abs(cy - v.y) - ey, 0.0)
            val dz = max(abs(cz - v.z) - ez, 0.0)
            Maths.sq(dx, dy, dz)
        } else 0.0
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

}