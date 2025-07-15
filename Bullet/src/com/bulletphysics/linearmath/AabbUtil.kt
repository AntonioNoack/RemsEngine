package com.bulletphysics.linearmath

import cz.advel.stack.Stack
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Utility functions for axis aligned bounding boxes (AABB).
 *
 * @author jezek2
 */
object AabbUtil {
    fun aabbExpand(aabbMin: Vector3d, aabbMax: Vector3d, expansionMin: Vector3d, expansionMax: Vector3d) {
        aabbMin.add(expansionMin)
        aabbMax.add(expansionMax)
    }

    fun outcode(p: Vector3d, halfExtent: Vector3d): Int {
        return (if (p.x < -halfExtent.x) 0x01 else 0x0) or
                (if (p.x > halfExtent.x) 0x08 else 0x0) or
                (if (p.y < -halfExtent.y) 0x02 else 0x0) or
                (if (p.y > halfExtent.y) 0x10 else 0x0) or
                (if (p.z < -halfExtent.z) 0x4 else 0x0) or
                (if (p.z > halfExtent.z) 0x20 else 0x0)
    }

    fun rayAabb(
        rayFrom: Vector3d,
        rayTo: Vector3d,
        aabbMin: Vector3d,
        aabbMax: Vector3d,
        param: DoubleArray,
        normal: Vector3d
    ): Boolean {
        val aabbHalfExtent = Stack.newVec()
        val aabbCenter = Stack.newVec()
        val source = Stack.newVec()
        val target = Stack.newVec()
        val r = Stack.newVec()
        val hitNormal = Stack.newVec()

        aabbMax.sub(aabbMin, aabbHalfExtent)
        aabbHalfExtent.mul(0.5)

        aabbMax.add(aabbMin, aabbCenter)
        aabbCenter.mul(0.5)

        rayFrom.sub(aabbCenter, source)
        rayTo.sub(aabbCenter, target)

        val sourceOutcode = outcode(source, aabbHalfExtent)
        val targetOutcode = outcode(target, aabbHalfExtent)
        var hit = false
        if ((sourceOutcode and targetOutcode) == 0x0) {
            var lambdaEnter = 0.0
            var lambdaExit = param[0]
            target.sub(source, r)

            var normSign = 1.0
            hitNormal.set(0.0)
            var bit = 1

            repeat(2) {
                for (i in 0..2) {
                    if ((sourceOutcode and bit) != 0) {
                        val lambda = (-source[i] - aabbHalfExtent[i] * normSign) / r[i]
                        if (lambdaEnter <= lambda) {
                            lambdaEnter = lambda
                            hitNormal.set(0.0)
                            hitNormal[i] = normSign
                        }
                    } else if ((targetOutcode and bit) != 0) {
                        val lambda = (-source[i] - aabbHalfExtent[i] * normSign) / r[i]
                        lambdaExit = min(lambdaExit, lambda)
                    }
                    bit = bit shl 1
                }
                normSign = -1.0
            }
            if (lambdaEnter <= lambdaExit) {
                param[0] = lambdaEnter
                normal.set(hitNormal)
                hit = true
            }
        }
        Stack.subVec(6)
        return hit
    }

    /**
     * Conservative test for overlap between two AABBs.
     */
    fun testAabbAgainstAabb2(aabbMin1: Vector3d, aabbMax1: Vector3d, aabbMin2: Vector3d, aabbMax2: Vector3d): Boolean {
        return !(aabbMin1.x > aabbMax2.x) && !(aabbMax1.x < aabbMin2.x) &&
                !(aabbMin1.z > aabbMax2.z) && !(aabbMax1.z < aabbMin2.z) &&
                !(aabbMin1.y > aabbMax2.y) && !(aabbMax1.y < aabbMin2.y)
    }

    /**
     * Conservative test for overlap between triangle and AABB.
     */
    fun testTriangleAgainstAabb2(vertices: Array<Vector3d>, aabbMin: Vector3d, aabbMax: Vector3d): Boolean {
        val p1 = vertices[0]
        val p2 = vertices[1]
        val p3 = vertices[2]

        if (min(min(p1.x, p2.x), p3.x) > aabbMax.x) return false
        if (max(max(p1.x, p2.x), p3.x) < aabbMin.x) return false

        if (min(min(p1.z, p2.z), p3.z) > aabbMax.z) return false
        if (max(max(p1.z, p2.z), p3.z) < aabbMin.z) return false

        if (min(min(p1.y, p2.y), p3.y) > aabbMax.y) return false
        if (max(max(p1.y, p2.y), p3.y) < aabbMin.y) return false

        return true
    }

    fun transformAabb(
        halfExtents: Vector3d, margin: Double, t: Transform,
        aabbMinOut: Vector3d, aabbMaxOut: Vector3d
    ) {
        val ex = halfExtents.x + margin
        val ey = halfExtents.y + margin
        val ez = halfExtents.z + margin

        val basis = t.basis
        val dx = absDot(basis.m00, basis.m10, basis.m20, ex, ey, ez)
        val dy = absDot(basis.m01, basis.m11, basis.m21, ex, ey, ez)
        val dz = absDot(basis.m02, basis.m12, basis.m22, ex, ey, ez)

        val center = t.origin
        center.sub(dx, dy, dz, aabbMinOut)
        center.add(dx, dy, dz, aabbMaxOut)
    }

    @JvmStatic
    private fun absDot(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double): Double {
        return abs(ax) * bx + abs(ay) * by + abs(az) * bz
    }

    fun transformAabb(
        localAabbMin: Vector3d, localAabbMax: Vector3d,
        margin: Double, trans: Transform,
        aabbMinOut: Vector3d, aabbMaxOut: Vector3d
    ) {

        // assert(localAabbMin.x <= localAabbMax.x)
        // assert(localAabbMin.y <= localAabbMax.y)
        // assert(localAabbMin.z <= localAabbMax.z)

        val center = Stack.newVec()
        localAabbMax.add(localAabbMin, center)
        center.mul(0.5)
        trans.transform(center)

        val ex = ((localAabbMax.x - localAabbMin.x) * 0.5) + margin
        val ey = ((localAabbMax.y - localAabbMin.y) * 0.5) + margin
        val ez = ((localAabbMax.z - localAabbMin.z) * 0.5) + margin

        val basis = trans.basis
        val dx = absDot(basis.m00, basis.m10, basis.m20, ex, ey, ez)
        val dy = absDot(basis.m01, basis.m11, basis.m21, ex, ey, ez)
        val dz = absDot(basis.m02, basis.m12, basis.m22, ex, ey, ez)

        center.sub(dx, dy, dz, aabbMinOut)
        center.add(dx, dy, dz, aabbMaxOut)

        Stack.subVec(1)
    }
}
