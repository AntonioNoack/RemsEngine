package me.anno.utils.types

import me.anno.utils.maths.Maths
import org.joml.*
import kotlin.math.abs
import kotlin.math.max

object AABBs {

    fun AABBd.isEmpty() = minX > maxX
    fun AABBf.isEmpty() = minX > maxX

    fun AABBf.avgX() = (minX + maxX) * 0.5f
    fun AABBf.avgY() = (minY + maxY) * 0.5f
    fun AABBf.avgZ() = (minZ + maxZ) * 0.5f

    fun AABBd.avgX() = (minX + maxX) * 0.5
    fun AABBd.avgY() = (minY + maxY) * 0.5
    fun AABBd.avgZ() = (minZ + maxZ) * 0.5

    fun AABBf.deltaX() = maxX - minX
    fun AABBf.deltaY() = maxY - minY
    fun AABBf.deltaZ() = maxZ - minZ

    fun AABBf.print() = "($minX $minY $minZ) < ($maxX $maxY $maxZ)"
    fun AABBd.print() = "($minX $minY $minZ) < ($maxX $maxY $maxZ)"

    // crazy... why is this not in the library???
    fun AABBf.set(o: AABBf): AABBf {
        minX = o.minX
        minY = o.minY
        minZ = o.minZ
        maxX = o.maxX
        maxY = o.maxY
        maxZ = o.maxZ
        return this
    }

    fun AABBf.set(o: AABBd): AABBf {
        minX = o.minX.toFloat()
        minY = o.minY.toFloat()
        minZ = o.minZ.toFloat()
        maxX = o.maxX.toFloat()
        maxY = o.maxY.toFloat()
        maxZ = o.maxZ.toFloat()
        return this
    }

    fun AABBd.set(o: AABBd): AABBd {
        minX = o.minX
        minY = o.minY
        minZ = o.minZ
        maxX = o.maxX
        maxY = o.maxY
        maxZ = o.maxZ
        return this
    }

    fun AABBf.clear(): AABBf {
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        minZ = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        maxZ = Float.NEGATIVE_INFINITY
        return this
    }

    fun AABBd.clear(): AABBd {
        minX = Double.POSITIVE_INFINITY
        minY = Double.POSITIVE_INFINITY
        minZ = Double.POSITIVE_INFINITY
        maxX = Double.NEGATIVE_INFINITY
        maxY = Double.NEGATIVE_INFINITY
        maxZ = Double.NEGATIVE_INFINITY
        return this
    }

    fun AABBd.all(): AABBd {
        minX = Double.NEGATIVE_INFINITY
        minY = Double.NEGATIVE_INFINITY
        minZ = Double.NEGATIVE_INFINITY
        maxX = Double.POSITIVE_INFINITY
        maxY = Double.POSITIVE_INFINITY
        maxZ = Double.POSITIVE_INFINITY
        return this
    }

    fun AABBd.set(src: AABBf): AABBd {
        minX = src.minX.toDouble()
        minY = src.minY.toDouble()
        minZ = src.minZ.toDouble()
        maxX = src.maxX.toDouble()
        maxY = src.maxY.toDouble()
        maxZ = src.maxZ.toDouble()
        return this
    }

    fun transformAABB(that: AABBd, transform: Matrix4x3d, dst: AABBd = that): AABBd {
        return that.transform(transform, dst)
    }

    fun AABBd.transform(trans: Matrix4x3d, dst: AABBd): AABBd {
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
    fun AABBd.transformUnion(m: Matrix4x3d, base: AABBd, dst: AABBd): AABBd {
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
    fun AABBd.transformUnion(m: Matrix4x3d, base: AABBf, dst: AABBd): AABBd {
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
     * transforms this matrix, then unions it with base, and places the result in dst
     * */
    fun AABBf.transformUnion(m: Matrix4x3d, base: AABBd, dst: AABBd = base): AABBd {
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
    fun AABBf.transformProjectUnion(m: Matrix4f, base: AABBf, dst: AABBf = base): AABBf {
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
            val x = mx + (i and 1).toFloat() * dx
            val y = my + ((i shr 1) and 1).toFloat() * dy
            val z = mz + ((i shr 2) and 1).toFloat() * dz
            val tw = m.m03() * x + m.m13() * y + m.m23() * z + m.m33()
            val tx = (m.m00() * x + m.m10() * y + m.m20() * z + m.m30()) / tw
            val ty = (m.m01() * x + m.m11() * y + m.m21() * z + m.m31()) / tw
            val tz = (m.m02() * x + m.m12() * y + m.m22() * z + m.m32()) / tw
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
    fun AABBf.transformUnion(m: Matrix4x3d, base: AABBd, scale: Double, dst: AABBd = base): AABBd {
        val mx = minX.toDouble() * scale
        val my = minY.toDouble() * scale
        val mz = minZ.toDouble() * scale
        val dx = this.maxX * scale - mx
        val dy = this.maxY * scale - my
        val dz = this.maxZ * scale - mz
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
    fun testLineAABB(aabb: AABBd, start: Vector3d, end: Vector3d): Boolean {
        if (aabb.isEmpty()) return false
        // this didn't work because of the end
        // the end typically won't be reached anyways
        // test the end just by distance: distance(aabb,start) must be <= distance(start,end)
        return aabb.testRay(start.x, start.y, start.z, end.x - start.x, end.y - start.y, end.z - start.z) &&
                distanceSquared(aabb, start) <= start.distanceSquared(end)
    }

    // pseudo-distance from aabb to v
    fun distanceSquared(aabb: AABBd, v: Vector3d): Double {
        if (aabb.testPoint(v)) return 0.0
        val cx = (aabb.minX + aabb.maxX) * 0.5
        val cy = (aabb.minY + aabb.maxY) * 0.5
        val cz = (aabb.minZ + aabb.maxZ) * 0.5
        val ex = (aabb.maxX - aabb.minX) * 0.5
        val ey = (aabb.maxY - aabb.minY) * 0.5
        val ez = (aabb.maxZ - aabb.minZ) * 0.5
        return if (ex.isFinite() && ey.isFinite() && ez.isFinite()) {
            val dx = max(abs(cx - v.x) - ex, 0.0)
            val dy = max(abs(cy - v.y) - ey, 0.0)
            val dz = max(abs(cz - v.z) - ez, 0.0)
            Maths.sq(dx, dy, dz)
        } else 0.0
    }

    // pseudo-distance from aabb to v
    fun distanceSquared(aabb: AABBf, v: Vector3f): Float {
        if (aabb.testPoint(v)) return 0f
        val cx = (aabb.minX + aabb.maxX) * 0.5f
        val cy = (aabb.minY + aabb.maxY) * 0.5f
        val cz = (aabb.minZ + aabb.maxZ) * 0.5f
        val ex = (aabb.maxX - aabb.minX) * 0.5f
        val ey = (aabb.maxY - aabb.minY) * 0.5f
        val ez = (aabb.maxZ - aabb.minZ) * 0.5f
        return if (ex.isFinite() && ey.isFinite() && ez.isFinite()) {
            val dx = max(abs(cx - v.x) - ex, 0f)
            val dy = max(abs(cy - v.y) - ey, 0f)
            val dz = max(abs(cz - v.z) - ez, 0f)
            Maths.sq(dx, dy, dz)
        } else 0f
    }

    fun testLineAABB(aabb: AABBf, start: Vector3f, end: Vector3f): Boolean {
        if (aabb.isEmpty()) return false
        return aabb.testRay(start.x, start.y, start.z, end.x - start.x, end.y - start.y, end.z - start.z) &&
                distanceSquared(aabb, start) <= start.distanceSquared(end)
    }


}