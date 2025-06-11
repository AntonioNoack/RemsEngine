package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.MatrixUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * Concave triangle mesh abstract class. Use [BvhTriangleMeshShape] as concreteimplementation.
 *
 * @author jezek2
 */
abstract class TriangleMeshShape constructor(val meshInterface: StridingMeshInterface?) : ConcaveShape() {
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
            setCoord(vec, i, 1.0)
            val tmp = localGetSupportingVertex(vec, Stack.newVec())
            setCoord(localAabbMax, i, getCoord(tmp, i) + margin)
            setCoord(vec, i, -1.0)
            localGetSupportingVertex(vec, tmp)
            setCoord(localAabbMin, i, getCoord(tmp, i) - margin)
        }
    }

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        AabbUtil.transformAabb(
            localAabbMin, localAabbMax, margin,
            t, aabbMin, aabbMax
        )
    }

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        val filterCallback = FilteredCallback(callback, aabbMin, aabbMax)

        meshInterface!!.internalProcessAllTriangles(filterCallback, aabbMin, aabbMax)
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        // moving concave objects not supported
        assert(false)
        inertia.set(0.0, 0.0, 0.0)
    }


    override fun setLocalScaling(scaling: Vector3d) {
        meshInterface!!.setScaling(scaling)
        recalculateLocalAabb()
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        return meshInterface!!.getScaling(out)
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
            this.worldTrans.set(trans)
            MatrixUtil.transposeTransform(supportVecLocal, supportVecWorld, worldTrans.basis)
        }

        override fun processTriangle(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
            for (i in 0..2) {
                val dot = supportVecLocal.dot(triangle[i])
                if (dot > maxDot) {
                    maxDot = dot
                    supportVertexLocal.set(triangle[i])
                }
            }
        }

        fun getSupportVertexWorldSpace(out: Vector3d): Vector3d {
            out.set(supportVertexLocal)
            worldTrans.transform(out)
            return out
        }

        fun getSupportVertexLocal(out: Vector3d): Vector3d {
            out.set(supportVertexLocal)
            return out
        }
    }

    private class FilteredCallback(var callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) :
        InternalTriangleIndexCallback {
        val aabbMin: Vector3d = Vector3d()
        val aabbMax: Vector3d = Vector3d()

        init {
            this.aabbMin.set(aabbMin)
            this.aabbMax.set(aabbMax)
        }

        override fun internalProcessTriangleIndex(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
            if (AabbUtil.testTriangleAgainstAabb2(triangle, aabbMin, aabbMax)) {
                // check aabb in triangle-space, before doing this
                callback.processTriangle(triangle, partId, triangleIndex)
            }
        }
    }
}
