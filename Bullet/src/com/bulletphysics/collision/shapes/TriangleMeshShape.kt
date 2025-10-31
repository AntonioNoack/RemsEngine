package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.AABBd
import org.joml.Vector3d

/**
 * Concave triangle mesh abstract class. Use [BvhTriangleMeshShape] as concreteimplementation.
 *
 * @author jezek2
 */
abstract class TriangleMeshShape(val meshInterface: StridingMeshInterface) : ConcaveShape() {
    val localAabbMin: Vector3d = Vector3d()
    val localAabbMax: Vector3d = Vector3d()

    fun localGetSupportingVertex(vec: Vector3d, out: Vector3d): Vector3d {
        val identity = Stack.newTrans()
        identity.setIdentity()
        val supportCallback = SupportVertexCallback(vec, identity)

        val aabbMin = Stack.newVec()
        val aabbMax = Stack.newVec()
        aabbMax.set(1e308, 1e308, 1e308)
        aabbMin.set(-1e308, -1e308, -1e308)

        processAllTriangles(supportCallback, aabbMin, aabbMax)
        supportCallback.getSupportVertexLocal(out)

        Stack.subVec(2)
        Stack.subTrans(1)

        return out
    }

    fun localGetSupportingVertexWithoutMargin(vec: Vector3d, out: Vector3d): Vector3d {
        assert(false)
        return localGetSupportingVertex(vec, out)
    }

    fun recalculateLocalAabb() {
        for (i in 0..2) {
            val vec = Stack.newVec()
            vec.set(0.0, 0.0, 0.0)
            vec[i] = 1.0
            val tmp = localGetSupportingVertex(vec, Stack.newVec())
            localAabbMax[i] = tmp[i] + margin
            vec[i] = -1.0
            localGetSupportingVertex(vec, tmp)
            localAabbMin[i] = tmp[i] - margin
        }
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        AabbUtil.transformAabb(
            localAabbMin, localAabbMax, margin,
            t, aabbMin, aabbMax
        )
    }

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        val filterCallback = FilteredCallback(callback, aabbMin, aabbMax)
        meshInterface.internalProcessAllTriangles(filterCallback)
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        // moving concave objects not supported
        return inertia.set(0.0)
    }

    override fun setLocalScaling(scaling: Vector3d) {
        meshInterface.setScaling(scaling)
        recalculateLocalAabb()
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        return meshInterface.getScaling(out)
    }

    fun getLocalAabbMin(out: Vector3d): Vector3d {
        out.set(localAabbMin)
        return out
    }

    fun getLocalAabbMax(out: Vector3d): Vector3d {
        out.set(localAabbMax)
        return out
    }

    /**///////////////////////////////////////////////////////////////////////// */
    private class SupportVertexCallback(supportVecWorld: Vector3d, trans: Transform) : TriangleCallback {
        private val supportVertexLocal = Vector3d(0.0, 0.0, 0.0)
        val worldTrans: Transform = Transform()
        var maxDot: Double = -1e308
        val supportVecLocal: Vector3d = Vector3d()

        init {
            worldTrans.set(trans)
            worldTrans.basis.transformTranspose(supportVecWorld, supportVecLocal)
        }

        private fun processVertex(v: Vector3d) {
            val dotA = supportVecLocal.dot(v)
            if (dotA > maxDot) {
                maxDot = dotA
                supportVertexLocal.set(v)
            }
        }

        override fun processTriangle(a: Vector3d, b: Vector3d, c: Vector3d, partId: Int, triangleIndex: Int) {
            processVertex(a)
            processVertex(b)
            processVertex(c)
        }

        fun getSupportVertexLocal(out: Vector3d): Vector3d {
            out.set(supportVertexLocal)
            return out
        }
    }

    private class FilteredCallback(
        private val callback: TriangleCallback,
        aabbMin: Vector3d, aabbMax: Vector3d
    ) : InternalTriangleIndexCallback {

        private val bounds = AABBd(aabbMin, aabbMax)
        override fun internalProcessTriangleIndex(
            a: Vector3d, b: Vector3d, c: Vector3d,
            partId: Int, triangleIndex: Int
        ) {
            if (AabbUtil.testTriangleAgainstAabb2(a, b, c, bounds)) {
                // check aabb in triangle-space, before doing this
                callback.processTriangle(a, b, c, partId, triangleIndex)
            }
        }
    }
}
