package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.ScalarUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector4d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setSub

/**
 * BoxShape is a box primitive around the origin, its sides axis aligned with length
 * specified by half extents, in local shape coordinates. When used as part of a
 * [CollisionObject] or [RigidBody] it will be an oriented box in world space.
 *
 * @author jezek2
 */
open class BoxShape(boxHalfExtents: Vector3d) : PolyhedralConvexShape() {
    init {
        val margin = Vector3d(margin, margin, margin)
        VectorUtil.mul(implicitShapeDimensions, boxHalfExtents, localScaling)
        implicitShapeDimensions.sub(margin)
    }

    fun getHalfExtentsWithMargin(out: Vector3d): Vector3d {
        val halfExtents = getHalfExtentsWithoutMargin(out)
        val margin = Stack.borrowVec()
        margin.set(this.margin, this.margin, this.margin)
        halfExtents.add(margin)
        return out
    }

    fun getHalfExtentsWithoutMargin(out: Vector3d): Vector3d {
        out.set(implicitShapeDimensions) // changed in Bullet 2.63: assume the scaling and margin are included
        return out
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.BOX_SHAPE_PROXYTYPE

    override fun localGetSupportingVertex(dir: Vector3d, out: Vector3d): Vector3d {
        val halfExtents = getHalfExtentsWithoutMargin(out)

        val margin: Double = margin
        halfExtents.x += margin
        halfExtents.y += margin
        halfExtents.z += margin

        out.set(
            ScalarUtil.select(dir.x, halfExtents.x, -halfExtents.x),
            ScalarUtil.select(dir.y, halfExtents.y, -halfExtents.y),
            ScalarUtil.select(dir.z, halfExtents.z, -halfExtents.z)
        )
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        val halfExtents = getHalfExtentsWithoutMargin(out)
        out.set(
            ScalarUtil.select(dir.x, halfExtents.x, -halfExtents.x),
            ScalarUtil.select(dir.y, halfExtents.y, -halfExtents.y),
            ScalarUtil.select(dir.z, halfExtents.z, -halfExtents.z)
        )
        return out
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>,
        outs: Array<Vector3d>,
        numVectors: Int
    ) {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec())

        for (i in 0 until numVectors) {
            val vec = dirs[i]
            outs[i].set(
                ScalarUtil.select(vec.x, halfExtents.x, -halfExtents.x),
                ScalarUtil.select(vec.y, halfExtents.y, -halfExtents.y),
                ScalarUtil.select(vec.z, halfExtents.z, -halfExtents.z)
            )
        }
    }

    override var margin: Double
        get() = super.margin
        set(value) {
            // correct the implicitShapeDimensions for the margin
            val oldMargin = Stack.newVec()
            oldMargin.set(super.margin, super.margin, super.margin)
            val implicitShapeDimensionsWithMargin = Stack.newVec()
            implicitShapeDimensionsWithMargin.setAdd(implicitShapeDimensions, oldMargin)

            super.margin = value
            val newMargin = Stack.newVec()
            newMargin.set(value, value, value)
            implicitShapeDimensions.setSub(implicitShapeDimensionsWithMargin, newMargin)
            Stack.subVec(3)
        }

    override fun setLocalScaling(scaling: Vector3d) {
        val oldMargin = Stack.newVec()
        oldMargin.set(margin, margin, margin)
        val implicitShapeDimensionsWithMargin = Stack.newVec()
        implicitShapeDimensionsWithMargin.setAdd(implicitShapeDimensions, oldMargin)
        val unScaledImplicitShapeDimensionsWithMargin = Stack.newVec()
        VectorUtil.div(unScaledImplicitShapeDimensionsWithMargin, implicitShapeDimensionsWithMargin, localScaling)

        super.setLocalScaling(scaling)

        VectorUtil.mul(implicitShapeDimensions, unScaledImplicitShapeDimensionsWithMargin, localScaling)
        implicitShapeDimensions.sub(oldMargin)
        Stack.subVec(3)
    }

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        AabbUtil.transformAabb(
            getHalfExtentsWithoutMargin(Stack.newVec()),
            margin, t, aabbMin, aabbMax
        )
        Stack.subVec(1)
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        val halfExtents = getHalfExtentsWithMargin(Stack.newVec())

        val lx = 2.0 * halfExtents.x
        val ly = 2.0 * halfExtents.y
        val lz = 2.0 * halfExtents.z

        inertia.set(
            mass / 12.0 * (ly * ly + lz * lz),
            mass / 12.0 * (lx * lx + lz * lz),
            mass / 12.0 * (lx * lx + ly * ly)
        )

        Stack.subVec(1)
    }

    override fun getPlane(planeNormal: Vector3d, planeSupport: Vector3d, i: Int) {
        // this plane might not be aligned...
        val plane = Vector4d()
        getPlaneEquation(plane, i)
        planeNormal.set(plane.x, plane.y, plane.z)
        val tmp = Stack.newVec()
        planeNormal.negate(tmp)
        localGetSupportingVertex(tmp, planeSupport)
    }

    override val numPlanes
        get(): Int {
            return 6
        }

    override val numVertices
        get(): Int {
            return 8
        }

    override val numEdges
        get(): Int {
            return 12
        }

    override fun getVertex(i: Int, vtx: Vector3d) {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec())

        vtx.set(
            halfExtents.x * (1 - (i and 1)) - halfExtents.x * (i and 1),
            halfExtents.y * (1 - ((i and 2) shr 1)) - halfExtents.y * ((i and 2) shr 1),
            halfExtents.z * (1 - ((i and 4) shr 2)) - halfExtents.z * ((i and 4) shr 2)
        )
    }

    fun getPlaneEquation(plane: Vector4d, i: Int) {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec())
        val axisValue = if ((i and 1) == 0) 1.0 else -1.0
        val axis = i shr 1
        when (axis) {
            0 -> plane.set(axisValue, 0.0, 0.0, -halfExtents.x)
            1 -> plane.set(0.0, axisValue, 0.0, -halfExtents.y)
            else -> plane.set(0.0, 0.0, axisValue, -halfExtents.z)
        }
    }

    override fun getEdge(i: Int, pa: Vector3d, pb: Vector3d) {
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
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec())
        return (pt.x <= (halfExtents.x + tolerance)) &&
                (pt.x >= (-halfExtents.x - tolerance)) &&
                (pt.y <= (halfExtents.y + tolerance)) &&
                (pt.y >= (-halfExtents.y - tolerance)) &&
                (pt.z <= (halfExtents.z + tolerance)) &&
                (pt.z >= (-halfExtents.z - tolerance))
    }

    override val numPreferredPenetrationDirections get() = 6

    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3d) {
        val axis = index shr 1
        val value = if ((index and 1) == 0) 1.0 else -1.0
        penetrationVector.set(0.0, 0.0, 0.0)
        VectorUtil.setCoord(penetrationVector, axis, value)
    }
}
