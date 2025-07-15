package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.StridingMeshInterface
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * @author jezek2
 */
class GImpactMeshShape(meshInterface: StridingMeshInterface) : GImpactShapeInterface() {

    var meshParts = ArrayList<GImpactMeshShapePart>()

    init {
        buildMeshParts(meshInterface)
    }

    val meshPartCount: Int
        get() = meshParts.size

    fun getMeshPart(index: Int): GImpactMeshShapePart {
        return meshParts[index]
    }

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
        for (i in meshParts.indices) {
            meshParts[i].setLocalScaling(scaling)
        }
        needsUpdate = true
    }

    override var margin: Double
        get() = collisionMargin
        set(value) {
            collisionMargin = value
            for (i in meshParts.indices) {
                meshParts[i].margin = margin
            }
            needsUpdate = true
        }

    override fun postUpdate() {
        for (i in meshParts.indices) {
            meshParts[i].postUpdate()
        }
        needsUpdate = true
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {

        val partialMass = mass / meshPartCount.toDouble()

        inertia.set(0.0)
        val partialInertia = Stack.newVec()
        for (i in 0 until meshPartCount) {
            getMeshPart(i).calculateLocalInertia(partialMass, partialInertia)
            inertia.add(partialInertia)
        }
        Stack.subVec(1)

        return inertia
    }

    override val primitiveManager: PrimitiveManagerBase?
        get() = throw NotImplementedError()

    override val numChildShapes: Int
        get() = 0

    override fun childrenHasTransform(): Boolean {
        return false
    }

    override fun needsRetrieveTriangles(): Boolean {
        return false
    }

    override fun needsRetrieveTetrahedrons(): Boolean {
        return false
    }

    override fun getBulletTriangle(primIndex: Int, triangle: TriangleShapeEx) {
        // not implemented
    }

    override fun getBulletTetrahedron(primIndex: Int, tetrahedron: TetrahedronShapeEx) {
        // not implemented
    }

    override fun lockChildShapes() {
    }

    override fun unlockChildShapes() {
    }

    override fun getChildAabb(childIndex: Int, t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        // doesn't have children
    }

    override fun getChildShape(index: Int): CollisionShape {
        throw NotImplementedError()
    }

    override fun getChildTransform(index: Int): Transform {
        throw NotImplementedError()
    }

    override val gImpactShapeType: ShapeType?
        get() = ShapeType.TRIMESH_SHAPE

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        for (i in meshParts.indices) {
            meshParts[i]
                .processAllTriangles(callback, aabbMin, aabbMax)
        }
    }

    fun buildMeshParts(meshInterface: StridingMeshInterface) {
        var i = 0
        val len = meshInterface.numSubParts
        while (i < len) {
            meshParts.add(GImpactMeshShapePart(meshInterface, i))
            i++
        }
    }

    override fun calcLocalAABB() {
        val tmpAABB = AABB()

        localAABB.invalidate()
        for (i in meshParts.indices) {
            val part = meshParts[i]
            part.updateBound()
            localAABB.merge(part.getLocalBox(tmpAABB))
        }
    }
}
