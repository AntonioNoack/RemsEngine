package me.anno.utils.types

import org.joml.AABBd
import org.joml.AABBf
import org.joml.Math
import org.joml.Matrix4x3d

object AABBs {

    fun AABBd.isEmpty() = minX > maxX

    fun AABBd.reset() {
        minX = Double.POSITIVE_INFINITY
        minY = Double.POSITIVE_INFINITY
        minZ = Double.POSITIVE_INFINITY
        maxX = Double.NEGATIVE_INFINITY
        maxY = Double.NEGATIVE_INFINITY
        maxZ = Double.NEGATIVE_INFINITY
    }

    fun AABBd.set(src: AABBf) {
        minX = src.minX.toDouble()
        minY = src.minY.toDouble()
        minZ = src.minZ.toDouble()
        maxX = src.maxX.toDouble()
        maxY = src.maxY.toDouble()
        maxZ = src.maxZ.toDouble()
    }

    fun AABBd.transform(m: Matrix4x3d, dst: AABBd): AABBd {
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

}