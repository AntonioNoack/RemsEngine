package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Base class for gimpact shapes.
 *
 * @author jezek2
 */
abstract class GImpactShapeInterface : ConcaveShape() {
    val localAABB: AABB = AABB()
    var needsUpdate: Boolean
    var boxSet = GImpactBvh() // optionally boxset

    init {
        localAABB.invalidate()
        needsUpdate = true
    }

    /**
     * Performs refit operation.
     *
     * Updates the entire Box set of this shape.
     *
     * postUpdate() must be called for attempts to calculating the box set, else this function will do nothing.
     *
     * if m_needs_update == true, then it calls calcLocalAABB();
     */
    fun updateBound() {
        if (!needsUpdate) {
            return
        }
        calcLocalAABB()
        needsUpdate = false
    }

    /**
     * If the Bounding box is not updated, then this class attemps to calculate it.
     *
     *
     * Calls updateBound() for update the box set.
     */
    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val aabb = AABB(localAABB)
        aabb.applyTransform(t)
        aabbMin.set(aabb.min)
        aabbMax.set(aabb.max)
    }

    /**
     * Tells to this object that is needed to refit the box set.
     */
    open fun postUpdate() {
        needsUpdate = true
    }

    /**
     * Obtains the local box, which is the global calculated box of the total of subshapes.
     */
    fun getLocalBox(out: AABB): AABB {
        out.set(localAABB)
        return out
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONCAVE_GIMPACT_TRIANGLE_MESH

    /**
     * You must call updateBound() for update the box set.
     */
    override var localScaling: Vector3f
        get() = super.localScaling
        set(value) {
            super.localScaling = value
            postUpdate()
        }

    override var margin: Float
        get() = collisionMargin
        set(value) {
            collisionMargin = value
            for (i in 0 until numChildShapes) {
                getChildShape(i).margin = value
            }
            needsUpdate = true
        }

    /**
     * Base method for determining which kind of GIMPACT shape we get.
     */
    abstract val gImpactShapeType: ShapeType

    /**
     * Determines if this class has a hierarchy structure for sorting its primitives.
     */
    fun hasBoxSet(): Boolean {
        return boxSet.nodeCount != 0
    }

    /**
     * Obtains the primitive manager.
     */
    abstract val primitiveManager: PrimitiveManagerBase?

    /**
     * Gets the number of children.
     */
    abstract val numChildShapes: Int

    /**
     * If true, then its children must get transforms.
     */
    abstract fun childrenHasTransform(): Boolean

    /**
     * Determines if this shape has triangles.
     */
    abstract fun needsRetrieveTriangles(): Boolean

    /**
     * Determines if this shape has tetrahedrons.
     */
    abstract fun needsRetrieveTetrahedrons(): Boolean

    abstract fun getBulletTriangle(primIndex: Int, triangle: TriangleShapeEx)

    abstract fun getBulletTetrahedron(primIndex: Int, tetrahedron: TetrahedronShapeEx)

    /**
     * Call when reading child shapes.
     */
    open fun lockChildShapes() {
    }

    open fun unlockChildShapes() {
    }

    /**
     * If this trimesh.
     */
    fun getPrimitiveTriangle(index: Int, triangle: PrimitiveTriangle) {
        this.primitiveManager!!.getPrimitiveTriangle(index, triangle)
    }

    /**
     * Use this function for perfofm refit in bounding boxes.
     */
    open fun calcLocalAABB() {
        lockChildShapes()
        if (boxSet.nodeCount == 0) {
            boxSet.buildSet()
        } else {
            boxSet.update()
        }
        unlockChildShapes()

        boxSet.getGlobalBox(localAABB)
    }

    /**
     * Retrieves the bound from a child.
     */
    open fun getChildAabb(childIndex: Int, t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val childAabb = AABB()
        this.primitiveManager!!.getPrimitiveBox(childIndex, childAabb)
        childAabb.applyTransform(t)
        aabbMin.set(childAabb.min)
        aabbMax.set(childAabb.max)
    }

    /**
     * Gets the children.
     */
    abstract fun getChildShape(index: Int): CollisionShape

    /**
     * Gets the children transform.
     */
    abstract fun getChildTransform(index: Int): Transform

    /**
     * Function for retrieve triangles. It gives the triangles in local space.
     */
    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
    }
}
