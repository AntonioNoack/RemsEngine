package com.bulletphysics.collision.broadphase

import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setSub
import kotlin.math.abs

/**
 * Dbvt implementation by Nathanael Presson
 * @author jezek2
 */
class DbvtAabbMm {
    val min = Vector3d()
    val max = Vector3d()

    fun set(src: DbvtAabbMm) {
        min.set(src.min)
        max.set(src.max)
    }

    fun getCenter(out: Vector3d): Vector3d {
        return min.add(max, out).mul(0.5) // new API
    }

    fun addMargin(e: Vector3d) {
        min.sub(e)
        max.add(e)
    }

    fun addSignedMargin(e: Vector3d) {
        if (e.x > 0) {
            max.x += e.x
        } else {
            min.x += e.x
        }

        if (e.y > 0) {
            max.y += e.y
        } else {
            min.y += e.y
        }

        if (e.z > 0) {
            max.z += e.z
        } else {
            min.z += e.z
        }
    }

    fun contains(a: DbvtAabbMm): Boolean {
        return ((min.x <= a.min.x) &&
                (min.y <= a.min.y) &&
                (min.z <= a.min.z) &&
                (max.x >= a.max.x) &&
                (max.y >= a.max.y) &&
                (max.z >= a.max.z))
    }

    companion object {
        fun swap(p1: DbvtAabbMm, p2: DbvtAabbMm) {
            val tmp = Stack.borrowVec()

            tmp.set(p1.min)
            p1.min.set(p2.min)
            p2.min.set(tmp)

            tmp.set(p1.max)
            p1.max.set(p2.max)
            p2.max.set(tmp)
        }

        fun fromCenterExtents(c: Vector3d, e: Vector3d, out: DbvtAabbMm): DbvtAabbMm {
            out.min.setSub(c, e)
            out.max.setAdd(c, e)
            return out
        }

        fun fromCenterRadius(c: Vector3d, r: Double, out: DbvtAabbMm): DbvtAabbMm {
            val tmp = Stack.newVec()
            tmp.set(r, r, r)
            return fromCenterExtents(c, tmp, out)
        }

        fun fromMinMax(mi: Vector3d, mx: Vector3d, out: DbvtAabbMm): DbvtAabbMm {
            out.min.set(mi)
            out.max.set(mx)
            return out
        }

        fun intersect(a: DbvtAabbMm, b: DbvtAabbMm): Boolean {
            return ((a.min.x <= b.max.x) &&
                    (a.max.x >= b.min.x) &&
                    (a.min.y <= b.max.y) &&
                    (a.max.y >= b.min.y) &&
                    (a.min.z <= b.max.z) &&
                    (a.max.z >= b.min.z))
        }

        fun proximity(a: DbvtAabbMm, b: DbvtAabbMm): Double {
            val ai = a.min
            val ax = a.max
            val bi = b.min
            val bx = b.max
            return abs((ai.x + ax.x) - (bi.x + bx.x)) + abs((ai.y + ax.y) - (bi.y + bx.y)) + abs((ai.z + ax.z) - (bi.z + bx.z))
        }

        fun union(a: DbvtAabbMm, b: DbvtAabbMm, dst: DbvtAabbMm) {
            setMin(dst.min, a.min, b.min)
            setMax(dst.max, a.max, b.max)
        }

        fun notEqual(a: DbvtAabbMm, b: DbvtAabbMm): Boolean {
            return ((a.min.x != b.min.x) ||
                    (a.min.y != b.min.y) ||
                    (a.min.z != b.min.z) ||
                    (a.max.x != b.max.x) ||
                    (a.max.y != b.max.y) ||
                    (a.max.z != b.max.z))
        }
    }
}
