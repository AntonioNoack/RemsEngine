package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.StridingMeshInterface
import com.bulletphysics.collision.shapes.VertexData
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * @author jezek2
 */
class TrimeshPrimitiveManager : PrimitiveManagerBase {

    @JvmField
    var margin: Float

    @JvmField
    var meshInterface: StridingMeshInterface?

    @JvmField
    val scale = Vector3f()

    @JvmField
    var part: Int

    var lockCount: Int = 0

    private val tmpIndices = IntArray(3)

    private var vertexData: VertexData? = null

    constructor() {
        meshInterface = null
        part = 0
        margin = 0.01f
        scale.set(1f)
    }

    constructor(manager: TrimeshPrimitiveManager) {
        meshInterface = manager.meshInterface
        part = manager.part
        margin = manager.margin
        scale.set(manager.scale)
    }

    constructor(meshInterface: StridingMeshInterface, part: Int) {
        this.meshInterface = meshInterface
        this.part = part
        scale.set(meshInterface.scaling)
        margin = 0.1f
    }

    fun lock() {
        if (lockCount > 0) {
            lockCount++
            return
        }
        vertexData = meshInterface!!.getLockedReadOnlyVertexIndexBase(part)

        lockCount = 1
    }

    fun unlock() {
        if (lockCount == 0) {
            return
        }
        if (lockCount > 1) {
            --lockCount
            return
        }
        meshInterface!!.unLockReadOnlyVertexBase(part)
        vertexData = null
        lockCount = 0
    }

    override val isTrimesh: Boolean
        get() = true

    override val primitiveCount: Int
        get() = vertexData!!.indexCount / 3

    val vertexCount: Int
        get() = vertexData!!.vertexCount

    fun getIndices(faceIndex: Int, out: IntArray) {
        val vertexData = vertexData!!
        out[0] = vertexData.getIndex(faceIndex * 3)
        out[1] = vertexData.getIndex(faceIndex * 3 + 1)
        out[2] = vertexData.getIndex(faceIndex * 3 + 2)
    }

    fun getVertex(vertexIndex: Int, dst: Vector3f) {
        vertexData!!.getVertex(vertexIndex, dst)
        dst.mul(scale)
    }

    fun getVertex(vertexIndex: Int, dst: Vector3d) {
        val tmp = Stack.borrowVec3f()
        getVertex(vertexIndex, tmp)
        dst.set(tmp)
    }

    override fun getPrimitiveBox(primitiveIndex: Int, dst: AABB) {
        val triangle = PrimitiveTriangle()
        getPrimitiveTriangle(primitiveIndex, triangle)
        dst.calcFromTriangleMargin(
            triangle.vertices[0], triangle.vertices[1], triangle.vertices[2],
            triangle.margin.toDouble()
        )
    }

    override fun getPrimitiveTriangle(primIndex: Int, triangle: PrimitiveTriangle) {
        getIndices(primIndex, tmpIndices)
        getVertex(tmpIndices[0], triangle.vertices[0])
        getVertex(tmpIndices[1], triangle.vertices[1])
        getVertex(tmpIndices[2], triangle.vertices[2])
        triangle.margin = margin
    }

    fun getBulletTriangle(primIndex: Int, triangle: TriangleShapeEx) {
        getIndices(primIndex, tmpIndices)
        getVertex(tmpIndices[0], triangle.vertices[0])
        getVertex(tmpIndices[1], triangle.vertices[1])
        getVertex(tmpIndices[2], triangle.vertices[2])
        triangle.margin = margin
    }
}
