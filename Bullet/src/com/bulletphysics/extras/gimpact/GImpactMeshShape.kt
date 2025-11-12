package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.StridingMeshInterface
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

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

    override var localScaling: Vector3f
        get() = super.localScaling
        set(value) {
            super.localScaling = value
            for (i in meshParts.indices) {
                meshParts[i].localScaling = value
            }
            needsUpdate = true
        }

    override var margin: Float
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

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {

        val partialMass = mass / meshPartCount

        inertia.set(0f)
        val partialInertia = Stack.newVec3f()
        for (i in 0 until meshPartCount) {
            getMeshPart(i).calculateLocalInertia(partialMass, partialInertia)
            inertia.add(partialInertia)
        }
        Stack.subVec3f(1)

        return inertia
    }

    override val primitiveManager: PrimitiveManagerBase
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

    override val gImpactShapeType: ShapeType
        get() = ShapeType.TRIMESH_SHAPE

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        for (i in meshParts.indices) {
            meshParts[i].processAllTriangles(callback, aabbMin, aabbMax)
        }
    }

    fun buildMeshParts(meshInterface: StridingMeshInterface) {
        for (i in 0 until meshInterface.numSubParts) {
            meshParts.add(GImpactMeshShapePart(meshInterface, i))
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
