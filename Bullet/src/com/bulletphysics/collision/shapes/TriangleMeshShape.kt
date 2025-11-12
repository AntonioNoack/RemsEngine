package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Concave triangle mesh abstract class. Use [BvhTriangleMeshShape] as concreteimplementation.
 *
 * @author jezek2
 */
abstract class TriangleMeshShape(val meshInterface: StridingMeshInterface) : ConcaveShape() {
    val localAabbMin = Vector3f()
    val localAabbMax = Vector3f()

    fun localGetSupportingVertex(vec: Vector3f, out: Vector3f): Vector3f {
        val identity = Stack.newTrans()
        identity.setIdentity()
        val supportCallback = SupportVertexCallback(vec, identity)

        val aabbMin = Stack.newVec3d()
        val aabbMax = Stack.newVec3d()
        aabbMax.set(1e308, 1e308, 1e308)
        aabbMin.set(-1e308, -1e308, -1e308)

        processAllTriangles(supportCallback, aabbMin, aabbMax)
        supportCallback.getSupportVertexLocal(out)

        Stack.subVec3d(2)
        Stack.subTrans(1)

        return out
    }

    fun localGetSupportingVertexWithoutMargin(vec: Vector3f, out: Vector3f): Vector3f {
        return localGetSupportingVertex(vec, out)
    }

    fun recalculateLocalAabb() {
        val vec = Stack.newVec3f().set(0f)
        val tmp = Stack.newVec3f()
        for (i in 0..2) {
            vec[i] = 1f
            localGetSupportingVertex(vec, tmp)
            localAabbMax[i] = tmp[i] + margin
            vec[i] = -1f
            localGetSupportingVertex(vec, tmp)
            localAabbMin[i] = tmp[i] - margin
        }
        Stack.subVec3f(2)
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

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // moving concave objects not supported
        return inertia.set(0f)
    }

    /**///////////////////////////////////////////////////////////////////////// */
    private class SupportVertexCallback(supportVecWorld: Vector3f, trans: Transform) : TriangleCallback {
        private val supportVertexLocal = Vector3f(0f)
        val worldTrans: Transform = Transform()
        var maxDot = -1e308
        val supportVecLocal = Vector3f()

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

        fun getSupportVertexLocal(out: Vector3f): Vector3f {
            return out.set(supportVertexLocal)
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
