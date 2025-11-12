package com.bulletphysics.linearmath

import cz.advel.stack.Stack
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
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
        param: FloatArray,
        normal: Vector3f
    ): Boolean {
        val aabbHalfExtent = Stack.newVec3d()
        val aabbCenter = Stack.newVec3d()
        val source = Stack.newVec3d()
        val target = Stack.newVec3d()
        val r = Stack.newVec3d()
        val hitNormal = Stack.newVec3d()

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
            var lambdaEnter = 0f
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
                            lambdaEnter = lambda.toFloat()
                            hitNormal.set(0.0)
                            hitNormal[i] = normSign
                        }
                    } else if ((targetOutcode and bit) != 0) {
                        val lambda = (-source[i] - aabbHalfExtent[i] * normSign) / r[i]
                        lambdaExit = min(lambdaExit, lambda.toFloat())
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
        Stack.subVec3d(6)
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
    fun testTriangleAgainstAabb2(
        a: Vector3d, b: Vector3d, c: Vector3d,
        bounds: AABBd
    ): Boolean {
        if (min(min(a.x, b.x), c.x) > bounds.maxX) return false
        if (max(max(a.x, b.x), c.x) < bounds.minX) return false

        if (min(min(a.z, b.z), c.z) > bounds.maxZ) return false
        if (max(max(a.z, b.z), c.z) < bounds.minZ) return false

        if (min(min(a.y, b.y), c.y) > bounds.maxY) return false
        if (max(max(a.y, b.y), c.y) < bounds.minY) return false
        return true
    }

    fun transformAabb(
        halfExtents: Vector3f, margin: Float, t: Transform,
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
    private fun absDot(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        return abs(ax) * bx + abs(ay) * by + abs(az) * bz
    }

    fun transformAabb(
        localAabbMin: Vector3f, localAabbMax: Vector3f,
        margin: Float, trans: Transform,
        aabbMinOut: Vector3d, aabbMaxOut: Vector3d
    ) {
        val center = Stack.newVec3d()
            .set(localAabbMin)
            .add(localAabbMax)
            .mul(0.5)
        trans.transformPosition(center)

        val halfExtendsX = ((localAabbMax.x - localAabbMin.x) * 0.5f) + margin
        val halfExtendsY = ((localAabbMax.y - localAabbMin.y) * 0.5f) + margin
        val halfExtendsZ = ((localAabbMax.z - localAabbMin.z) * 0.5f) + margin

        val basis = trans.basis
        val dx = absDot(basis.m00, basis.m10, basis.m20, halfExtendsX, halfExtendsY, halfExtendsZ)
        val dy = absDot(basis.m01, basis.m11, basis.m21, halfExtendsX, halfExtendsY, halfExtendsZ)
        val dz = absDot(basis.m02, basis.m12, basis.m22, halfExtendsX, halfExtendsY, halfExtendsZ)

        center.sub(dx, dy, dz, aabbMinOut)
        center.add(dx, dy, dz, aabbMaxOut)

        Stack.subVec3d(1)
    }
}
