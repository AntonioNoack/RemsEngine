package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil
import com.bulletphysics.linearmath.VectorUtil.setCoord
import com.bulletphysics.util.ArrayPool
import cz.advel.stack.Stack
import org.joml.Vector3d
import java.util.*
import kotlin.math.sqrt

/**
 * PolyhedralConvexShape is an internal interface class for polyhedral convex shapes.
 *
 * @author jezek2
 */
abstract class PolyhedralConvexShape : ConvexInternalShape() {
    val localAabbMin: Vector3d = Vector3d(1.0, 1.0, 1.0)
    val localAabbMax: Vector3d = Vector3d(-1.0, -1.0, -1.0)
    var isLocalAabbValid: Boolean = false

    //	/** optional Hull is for optional Separating Axis Test Hull collision detection, see Hull.cpp */
    //	Hull optionalHull = null;
    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        var supVec = out
        supVec.set(0.0, 0.0, 0.0)

        var maxDot = -1e308

        val vec = Stack.newVec(dir)
        val lenSqr = vec.lengthSquared()
        if (lenSqr < 0.0001) {
            vec.set(1.0, 0.0, 0.0)
        } else {
            val invLen = 1.0 / sqrt(lenSqr)
            vec.mul(invLen)
        }

        val vtx = Stack.newVec()
        var newDot: Double

        for (i in 0 until this.numVertices) {
            getVertex(i, vtx)
            newDot = vec.dot(vtx)
            if (newDot > maxDot) {
                maxDot = newDot
                supVec = vtx
            }
        }
        out.set(supVec)
        Stack.subVec(2)
        return out
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>, outs: Array<Vector3d>, numVectors: Int
    ) {
        val vtx = Stack.newVec()
        var newDot: Double

        // JAVA NOTE: rewritten as code used W coord for temporary usage in Vector3
        val wcoords: DoubleArray = W_POOL.getFixed(numVectors)
        Arrays.fill(wcoords, -1e308)

        for (j in 0 until numVectors) {
            val vec = dirs[j]

            for (i in 0 until this.numVertices) {
                getVertex(i, vtx)
                newDot = vec.dot(vtx)
                //if (newDot > supportVerticesOut[j].w)
                if (newDot > wcoords[j]) {
                    //WARNING: don't swap next lines, the w component would get overwritten!
                    outs[j].set(vtx)
                    //supportVerticesOut[j].w = newDot;
                    wcoords[j] = newDot
                }
            }
        }

        W_POOL.release(wcoords)
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        // not yet, return box inertia

        val margin = margin

        val identity = Stack.newTrans()
        identity.setIdentity()
        val aabbMin = Stack.newVec()
        val aabbMax = Stack.newVec()
        getAabb(identity, aabbMin, aabbMax)

        val halfExtents = Stack.newVec()
        aabbMax.sub(aabbMin, halfExtents).mul(0.5)

        val lx = 2.0 * (halfExtents.x + margin)
        val ly = 2.0 * (halfExtents.y + margin)
        val lz = 2.0 * (halfExtents.z + margin)
        val x2 = lx * lx
        val y2 = ly * ly
        val z2 = lz * lz

        inertia.set(y2 + z2, x2 + z2, x2 + y2)
        inertia.mul(mass / 12.0)

        Stack.subVec(3)
        Stack.subTrans(1)
    }

    private fun getNonvirtualAabb(trans: Transform, aabbMin: Vector3d, aabbMax: Vector3d, margin: Double) {
        // lazy evaluation of local aabb
        assert(isLocalAabbValid)

        AabbUtil.transformAabb(localAabbMin, localAabbMax, margin, trans, aabbMin, aabbMax)
    }

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getNonvirtualAabb(t, aabbMin, aabbMax, margin)
    }

    fun getAabbBase(trans: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getNonvirtualAabb(trans, aabbMin, aabbMax, margin)
    }

    fun recalculateLocalAabb() {
        isLocalAabbValid = true

        //#if 1
        batchedUnitVectorGetSupportingVertexWithoutMargin(directions, supporting, 6)

        for (i in 0..2) {
            setCoord(localAabbMax, i, VectorUtil.getCoord(supporting[i], i) + margin)
            setCoord(localAabbMin, i, VectorUtil.getCoord(supporting[i + 3], i) - margin)
        }

        //#else
        //for (int i=0; i<3; i++) {
        //	Vector3d vec = Stack.newVec();
        //	vec.set(0.0, 0.0, 0.0);
        //	VectorUtil.setCoord(vec, i, 1.0);
        //	Vector3d tmp = localGetSupportingVertex(vec, Stack.newVec());
        //	VectorUtil.setCoord(localAabbMax, i, VectorUtil.getCoord(tmp, i) + collisionMargin);
        //	VectorUtil.setCoord(vec, i, -1.0);
        //	localGetSupportingVertex(vec, tmp);
        //	VectorUtil.setCoord(localAabbMin, i, VectorUtil.getCoord(tmp, i) - collisionMargin);
        //}
        //#endif
    }

    override fun setLocalScaling(scaling: Vector3d) {
        super.setLocalScaling(scaling)
        recalculateLocalAabb()
    }

    abstract val numVertices: Int
    abstract val numEdges: Int
    abstract val numPlanes: Int

    abstract fun getEdge(i: Int, pa: Vector3d, pb: Vector3d)
    abstract fun getVertex(i: Int, vtx: Vector3d)
    abstract fun getPlane(planeNormal: Vector3d, planeSupport: Vector3d, i: Int)

    @Suppress("unused")
    abstract fun isInside(pt: Vector3d, tolerance: Double): Boolean

    companion object {
        private val W_POOL = ArrayPool<DoubleArray>(Double::class.javaPrimitiveType!!)

        private val directions = arrayOf<Vector3d>(
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0),
            Vector3d(-1.0, 0.0, 0.0),
            Vector3d(0.0, -1.0, 0.0),
            Vector3d(0.0, 0.0, -1.0)
        )

        private val supporting = arrayOf<Vector3d>(
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(0.0, 0.0, 0.0)
        )
    }
}
