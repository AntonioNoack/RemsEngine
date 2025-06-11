package com.bulletphysics.linearmath

import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setSub
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

        aabbHalfExtent.setSub(aabbMax, aabbMin)
        aabbHalfExtent.mul(0.5)

        aabbCenter.setAdd(aabbMax, aabbMin)
        aabbCenter.mul(0.5)

        source.setSub(rayFrom, aabbCenter)
        target.setSub(rayTo, aabbCenter)

        val sourceOutcode = outcode(source, aabbHalfExtent)
        val targetOutcode = outcode(target, aabbHalfExtent)
        var hit = false
        if ((sourceOutcode and targetOutcode) == 0x0) {
            var lambdaEnter = 0.0
            var lambdaExit = param[0]
            r.setSub(target, source)

            var normSign = 1.0
            hitNormal.set(0.0, 0.0, 0.0)
            var bit = 1

            repeat(2) {
                for (i in 0..2) {
                    if ((sourceOutcode and bit) != 0) {
                        val lambda = (-getCoord(source, i) - getCoord(aabbHalfExtent, i) * normSign) / getCoord(r, i)
                        if (lambdaEnter <= lambda) {
                            lambdaEnter = lambda
                            hitNormal.set(0.0, 0.0, 0.0)
                            setCoord(hitNormal, i, normSign)
                        }
                    } else if ((targetOutcode and bit) != 0) {
                        val lambda = (-getCoord(source, i) - getCoord(aabbHalfExtent, i) * normSign) / getCoord(r, i)
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
        val halfExtentsWithMargin = Stack.newVec()
        halfExtentsWithMargin.x = halfExtents.x + margin
        halfExtentsWithMargin.y = halfExtents.y + margin
        halfExtentsWithMargin.z = halfExtents.z + margin

        val globalAxis = Stack.newVec()
        val extent = Stack.newVec()

        val basis = t.basis
        basis.getRow(0, globalAxis)
        globalAxis.absolute()
        extent.x = globalAxis.dot(halfExtentsWithMargin)

        basis.getRow(1, globalAxis)
        globalAxis.absolute()
        extent.y = globalAxis.dot(halfExtentsWithMargin)

        basis.getRow(2, globalAxis)
        globalAxis.absolute()
        extent.z = globalAxis.dot(halfExtentsWithMargin)

        val center = t.origin
        center.sub(extent, aabbMinOut)
        center.add(extent, aabbMaxOut)

        Stack.subVec(3)
    }

    fun transformAabb(
        localAabbMin: Vector3d, localAabbMax: Vector3d,
        margin: Double, trans: Transform,
        aabbMinOut: Vector3d, aabbMaxOut: Vector3d
    ) {

        assert(localAabbMin.x <= localAabbMax.x)
        assert(localAabbMin.y <= localAabbMax.y)
        assert(localAabbMin.z <= localAabbMax.z)

        val localHalfExtents = Stack.newVec()
        localHalfExtents.setSub(localAabbMax, localAabbMin)
        localHalfExtents.mul(0.5).add(margin)

        val center = Stack.newVec()
        center.setAdd(localAabbMax, localAabbMin)
        center.mul(0.5)
        trans.transform(center)

        val extent = Stack.newVec()
        val globalAxis = Stack.newVec()

        val basis = trans.basis
        basis.getRow(0, globalAxis)
        globalAxis.absolute()
        extent.x = globalAxis.dot(localHalfExtents)

        basis.getRow(1, globalAxis)
        globalAxis.absolute()
        extent.y = globalAxis.dot(localHalfExtents)

        basis.getRow(2, globalAxis)
        globalAxis.absolute()
        extent.z = globalAxis.dot(localHalfExtents)

        center.sub(extent, aabbMinOut)
        center.add(extent, aabbMaxOut)

        Stack.subVec(4)
    }
}
