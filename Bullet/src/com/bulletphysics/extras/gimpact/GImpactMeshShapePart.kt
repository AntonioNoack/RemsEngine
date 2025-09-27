package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.StridingMeshInterface
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3d

/**
 * This class manages a sub part of a mesh supplied by the StridingMeshInterface interface.
 *
 *
 *
 *
 * - Simply create this shape by passing the StridingMeshInterface to the constructor
 * GImpactMeshShapePart, then you must call updateBound() after creating the mesh<br></br>
 * - When making operations with this shape, you must call **lock** before accessing
 * to the trimesh primitives, and then call **unlock**<br></br>
 * - You can handle deformable meshes with this shape, by calling postUpdate() every time
 * when changing the mesh vertices.
 *
 * @author jezek2
 */
class GImpactMeshShapePart : GImpactShapeInterface {
    var trimeshPrimitiveManager: TrimeshPrimitiveManager = TrimeshPrimitiveManager()

    private val collided = IntArrayList()

    constructor() {
        boxSet.primitiveManager = trimeshPrimitiveManager
    }

    constructor(meshInterface: StridingMeshInterface?, part: Int) {
        trimeshPrimitiveManager.meshInterface = meshInterface
        trimeshPrimitiveManager.part = part
        boxSet.primitiveManager = trimeshPrimitiveManager
    }

    override fun childrenHasTransform(): Boolean {
        return false
    }

    override fun lockChildShapes() {
        val dummyManager = boxSet.primitiveManager as TrimeshPrimitiveManager
        dummyManager.lock()
    }

    override fun unlockChildShapes() {
        val dummymanager = boxSet.primitiveManager as TrimeshPrimitiveManager
        dummymanager.unlock()
    }

    override val numChildShapes: Int
        get() = trimeshPrimitiveManager.primitiveCount

    override fun getChildShape(index: Int): CollisionShape {
        throw NotImplementedError()
    }

    override fun getChildTransform(index: Int): Transform {
        throw NotImplementedError()
    }

    override val primitiveManager: PrimitiveManagerBase
        get() = this.trimeshPrimitiveManager

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        lockChildShapes()

        //#define CALC_EXACT_INERTIA 1
        //#ifdef CALC_EXACT_INERTIA
        inertia.set(0.0, 0.0, 0.0)

        var i = this.vertexCount
        val pointmass = mass / i.toDouble()

        val pointInertia = Stack.newVec()
        while ((i--) != 0) {
            getVertex(i, pointInertia)
            getPointInertia(pointInertia, pointmass, pointInertia)
            inertia.add(pointInertia)
        }
        Stack.subVec(1)

        unlockChildShapes()
        return inertia
    }

    override val gImpactShapeType: ShapeType
        get() = ShapeType.TRIMESH_SHAPE_PART

    override fun needsRetrieveTriangles(): Boolean {
        return true
    }

    override fun needsRetrieveTetrahedrons(): Boolean {
        return false
    }

    override fun getBulletTriangle(primIndex: Int, triangle: TriangleShapeEx) {
        trimeshPrimitiveManager.getBulletTriangle(primIndex, triangle)
    }

    override fun getBulletTetrahedron(primIndex: Int, tetrahedron: TetrahedronShapeEx) {
        // not supported/implemented
    }

    val vertexCount: Int
        get() = trimeshPrimitiveManager.vertexCount

    fun getVertex(vertexIndex: Int, vertex: Vector3d) {
        trimeshPrimitiveManager.getVertex(vertexIndex, vertex)
    }

    override var margin: Double
        get() = trimeshPrimitiveManager.margin
        set(value) {
            trimeshPrimitiveManager.margin = value
            postUpdate()
        }

    override fun setLocalScaling(scaling: Vector3d) {
        trimeshPrimitiveManager.scale.set(scaling)
        postUpdate()
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        out.set(trimeshPrimitiveManager.scale)
        return out
    }

    val part: Int
        get() = trimeshPrimitiveManager.part

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        lockChildShapes()
        val box = AABB()
        box.min.set(aabbMin)
        box.max.set(aabbMax)

        collided.clear()
        boxSet.boxQuery(box, collided)

        if (collided.size == 0) {
            unlockChildShapes()
            return
        }

        val part = this.part
        val triangle = PrimitiveTriangle()
        var i = collided.size
        while ((i--) != 0) {
            getPrimitiveTriangle(collided[i], triangle)
            callback.processTriangle(triangle.vertices, part, collided[i])
        }
        unlockChildShapes()
    }

    companion object {
        private fun getPointInertia(point: Vector3d, mass: Double, out: Vector3d): Vector3d {
            val x2 = point.x * point.x
            val y2 = point.y * point.y
            val z2 = point.z * point.z
            out.set(mass * (y2 + z2), mass * (x2 + z2), mass * (x2 + y2))
            return out
        }
    }
}
