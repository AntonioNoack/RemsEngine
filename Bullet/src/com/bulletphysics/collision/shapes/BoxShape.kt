package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.ScalarUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

/**
 * BoxShape is a box primitive around the origin, its sides axis aligned with length
 * specified by half extents, in local shape coordinates. When used as part of a
 * [com.bulletphysics.collision.dispatch.CollisionObject] or [com.bulletphysics.dynamics.RigidBody] it will be an oriented box in world space.
 *
 * @author jezek2
 */
open class BoxShape(boxHalfExtents: Vector3f) : PolyhedralConvexShape() {

    companion object {
        private const val INV_3 = 1f / 3f

        fun boxInertia(hx: Float, hy: Float, hz: Float, mass: Float, dst: Vector3f): Vector3f {
            val lx = hx * hx
            val ly = hy * hy
            val lz = hz * hz
            return dst.set(ly + lz, lx + lz, lx + ly)
                .mul(mass * INV_3)
        }
    }

    init {
        boxHalfExtents.sub(margin, implicitShapeDimensions)
    }

    override fun getVolume(): Float {
        val size = implicitShapeDimensions
        return abs(size.x * size.y * size.z)
    }

    fun getHalfExtentsWithMargin(out: Vector3f): Vector3f {
        return getHalfExtentsWithoutMargin(out).add(margin)
    }

    fun getHalfExtentsWithoutMargin(out: Vector3f): Vector3f {
        // changed in Bullet 2.63: assume the scaling and margin are included
        return out.set(implicitShapeDimensions)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CUBOID

    override fun localGetSupportingVertex(dir: Vector3f, out: Vector3f): Vector3f {
        val halfExtents = getHalfExtentsWithoutMargin(out).add(margin)
        out.set(
            ScalarUtil.select(dir.x, halfExtents.x, -halfExtents.x),
            ScalarUtil.select(dir.y, halfExtents.y, -halfExtents.y),
            ScalarUtil.select(dir.z, halfExtents.z, -halfExtents.z)
        )
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        val halfExtents = getHalfExtentsWithoutMargin(out)
        out.set(
            ScalarUtil.select(dir.x, halfExtents.x, -halfExtents.x),
            ScalarUtil.select(dir.y, halfExtents.y, -halfExtents.y),
            ScalarUtil.select(dir.z, halfExtents.z, -halfExtents.z)
        )
        return out
    }

    override var margin: Float
        get() = super.margin
        set(value) {
            // correct the implicitShapeDimensions for the margin
            val oldMargin = Stack.newVec3f()
            oldMargin.set(super.margin)
            val implicitShapeDimensionsWithMargin = Stack.newVec3f()
            implicitShapeDimensions.add(oldMargin, implicitShapeDimensionsWithMargin)

            super.margin = value
            val newMargin = Stack.newVec3f()
            newMargin.set(value, value, value)
            implicitShapeDimensionsWithMargin.sub(newMargin, implicitShapeDimensions)
            Stack.subVec3f(3)
        }

    override var localScaling: Vector3f
        get() = super.localScaling
        set(scaling) {

            val oldMargin = Stack.newVec3f()
            oldMargin.set(margin, margin, margin)
            val implicitShapeDimensionsWithMargin = Stack.newVec3f()
            implicitShapeDimensions.add(oldMargin, implicitShapeDimensionsWithMargin)
            val unScaledImplicitShapeDimensionsWithMargin = Stack.newVec3f()
            VectorUtil.div(unScaledImplicitShapeDimensionsWithMargin, implicitShapeDimensionsWithMargin, localScaling)

            super.localScaling = scaling

            VectorUtil.mul(implicitShapeDimensions, unScaledImplicitShapeDimensionsWithMargin, localScaling)
            implicitShapeDimensions.sub(oldMargin)
            Stack.subVec3f(3)
        }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        AabbUtil.transformAabb(
            getHalfExtentsWithoutMargin(Stack.newVec3f()),
            margin, t, aabbMin, aabbMax
        )
        Stack.subVec3f(1)
    }

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        val halfExtents = getHalfExtentsWithMargin(Stack.newVec3f())
        boxInertia(halfExtents.x, halfExtents.y, halfExtents.z, mass, inertia)
        Stack.subVec3f(1)
        return inertia
    }

    override val numVertices get(): Int = 8
    override val numEdges get(): Int = 12

    override fun getVertex(i: Int, vtx: Vector3f) {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec3f())
        vtx.set(
            halfExtents.x * (1 - (i and 1)) - halfExtents.x * (i and 1),
            halfExtents.y * (1 - ((i and 2) shr 1)) - halfExtents.y * ((i and 2) shr 1),
            halfExtents.z * (1 - ((i and 4) shr 2)) - halfExtents.z * ((i and 4) shr 2)
        )
        Stack.subVec3f(1)
    }

    override fun getEdge(i: Int, pa: Vector3f, pb: Vector3f) {
        var edgeVert0 = 0
        var edgeVert1 = 0
        when (i) {
            0 -> edgeVert1 = 1
            1 -> edgeVert1 = 2
            2 -> {
                edgeVert0 = 1
                edgeVert1 = 3
            }
            3 -> {
                edgeVert0 = 2
                edgeVert1 = 3
            }
            4 -> edgeVert1 = 4
            5 -> {
                edgeVert0 = 1
                edgeVert1 = 5
            }
            6 -> {
                edgeVert0 = 2
                edgeVert1 = 6
            }
            7 -> {
                edgeVert0 = 3
                edgeVert1 = 7
            }
            8 -> {
                edgeVert0 = 4
                edgeVert1 = 5
            }
            9 -> {
                edgeVert0 = 4
                edgeVert1 = 6
            }
            10 -> {
                edgeVert0 = 5
                edgeVert1 = 7
            }
            11 -> {
                edgeVert0 = 6
                edgeVert1 = 7
            }
            else -> assert(false)
        }

        getVertex(edgeVert0, pa)
        getVertex(edgeVert1, pb)
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.borrowVec3f())
        return (pt.x <= (halfExtents.x + tolerance)) &&
                (pt.x >= (-halfExtents.x - tolerance)) &&
                (pt.y <= (halfExtents.y + tolerance)) &&
                (pt.y >= (-halfExtents.y - tolerance)) &&
                (pt.z <= (halfExtents.z + tolerance)) &&
                (pt.z >= (-halfExtents.z - tolerance))
    }

    override val numPreferredPenetrationDirections get() = 6

    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3f) {
        val axis = index shr 1
        val value = if ((index and 1) == 0) 1f else -1f
        penetrationVector.set(0f)
        penetrationVector[axis] = value
    }
}
