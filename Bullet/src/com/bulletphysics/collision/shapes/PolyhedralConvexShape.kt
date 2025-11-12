package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sqrt

/**
 * PolyhedralConvexShape is an internal interface class for polyhedral convex shapes.
 *
 * @author jezek2
 */
abstract class PolyhedralConvexShape : ConvexInternalShape() {
    val localAabbMin = Vector3f(1.0, 1.0, 1.0)
    val localAabbMax = Vector3f(-1.0, -1.0, -1.0)
    var isLocalAabbValid = false

    //	/** optional Hull is for optional Separating Axis Test Hull collision detection, see Hull.cpp */
    //	Hull optionalHull = null;
    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        var supVec = out
        supVec.set(0.0, 0.0, 0.0)

        var maxDot = -1e38f

        val vec = Stack.newVec3f(dir)
        val lenSqr = vec.lengthSquared()
        if (lenSqr < 0.0001f) {
            vec.set(1f, 0f, 0f)
        } else {
            val invLen = 1f / sqrt(lenSqr)
            vec.mul(invLen)
        }

        val vtx = Stack.newVec3f()
        for (i in 0 until this.numVertices) {
            getVertex(i, vtx)
            val newDot = vec.dot(vtx)
            if (newDot > maxDot) {
                maxDot = newDot
                supVec = vtx
            }
        }
        out.set(supVec)
        Stack.subVec3f(2)
        return out
    }

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // not yet, return box inertia

        val margin = margin

        val identity = Stack.newTrans()
        identity.setIdentity()
        val aabbMin = Stack.newVec3d()
        val aabbMax = Stack.newVec3d()
        getBounds(identity, aabbMin, aabbMax)

        val halfExtents = Stack.newVec3d()
        aabbMax.sub(aabbMin, halfExtents).mul(0.5)

        val lx = 2f * (halfExtents.x + margin)
        val ly = 2f * (halfExtents.y + margin)
        val lz = 2f * (halfExtents.z + margin)
        val x2 = lx * lx
        val y2 = ly * ly
        val z2 = lz * lz

        inertia.set(y2 + z2, x2 + z2, x2 + y2)
        inertia.mul(mass / 12f)

        Stack.subVec3d(3)
        Stack.subTrans(1)

        return inertia
    }

    private fun getNonvirtualAabb(trans: Transform, aabbMin: Vector3d, aabbMax: Vector3d, margin: Float) {
        // lazy evaluation of local aabb
        assert(isLocalAabbValid)

        AabbUtil.transformAabb(localAabbMin, localAabbMax, margin, trans, aabbMin, aabbMax)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getNonvirtualAabb(t, aabbMin, aabbMax, margin)
    }

    fun getAabbBase(trans: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getNonvirtualAabb(trans, aabbMin, aabbMax, margin)
    }

    fun recalculateLocalAabb() {
        isLocalAabbValid = true
        batchedUnitVectorGetSupportingVertexWithoutMargin(directions, supporting, 6)
        localAabbMin.set(
            supporting[0].x,
            supporting[1].y,
            supporting[2].z,
        ).sub(margin)
        localAabbMax.set(
            supporting[3].x,
            supporting[4].y,
            supporting[5].z,
        ).add(margin)
    }

    override var localScaling: Vector3f
        get() = super.localScaling
        set(value) {
            super.localScaling = value
            recalculateLocalAabb()
        }

    abstract val numVertices: Int
    abstract val numEdges: Int

    abstract fun getEdge(i: Int, pa: Vector3f, pb: Vector3f)
    abstract fun getVertex(i: Int, vtx: Vector3f)

    @Suppress("unused")
    abstract fun isInside(pt: Vector3d, tolerance: Double): Boolean

    companion object {

        private val directions = arrayOf<Vector3f>(
            Vector3f(-1.0, 0.0, 0.0),
            Vector3f(0.0, -1.0, 0.0),
            Vector3f(0.0, 0.0, -1.0),
            Vector3f(1.0, 0.0, 0.0),
            Vector3f(0.0, 1.0, 0.0),
            Vector3f(0.0, 0.0, 1.0),
        )

        private val supporting = arrayOf<Vector3f>(
            Vector3f(0.0, 0.0, 0.0),
            Vector3f(0.0, 0.0, 0.0),
            Vector3f(0.0, 0.0, 0.0),
            Vector3f(0.0, 0.0, 0.0),
            Vector3f(0.0, 0.0, 0.0),
            Vector3f(0.0, 0.0, 0.0)
        )
    }
}
