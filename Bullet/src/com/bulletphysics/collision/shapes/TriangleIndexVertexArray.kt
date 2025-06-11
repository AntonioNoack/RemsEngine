package com.bulletphysics.collision.shapes

import java.nio.ByteBuffer

/**
 * TriangleIndexVertexArray allows to use multiple meshes, by indexing into existing
 * triangle/index arrays. Additional meshes can be added using [addIndexedMesh][.addIndexedMesh].
 *
 * No duplicate is made of the vertex/index data, it only indexes into external vertex/index
 * arrays. So keep those arrays around during the lifetime of this TriangleIndexVertexArray.
 *
 * @author jezek2
 */
class TriangleIndexVertexArray : StridingMeshInterface {

    val indexedMeshArray = ArrayList<IndexedMesh>()

    private val data = ByteBufferVertexData()

    @Suppress("unused")
    constructor()

    /**
     * Just to be backwards compatible.
     */
    constructor(
        numTriangles: Int,
        triangleIndexBase: ByteBuffer?,
        triangleIndexStride: Int,
        numVertices: Int,
        vertexBase: ByteBuffer?,
        vertexStride: Int
    ) {
        val mesh = IndexedMesh()

        mesh.numTriangles = numTriangles
        mesh.triangleIndexBase = triangleIndexBase
        mesh.triangleIndexStride = triangleIndexStride
        mesh.numVertices = numVertices
        mesh.vertexBase = vertexBase
        mesh.vertexStride = vertexStride

        addIndexedMesh(mesh)
    }

    @JvmOverloads
    fun addIndexedMesh(mesh: IndexedMesh, indexType: ScalarType? = ScalarType.INTEGER) {
        indexedMeshArray.add(mesh)
        mesh.indexType = indexType
    }

    override fun getLockedVertexIndexBase(subpart: Int): VertexData {
        val mesh = indexedMeshArray[subpart]

        data.vertexCount = mesh.numVertices
        data.vertexData = mesh.vertexBase
        //#ifdef BT_USE_DOUBLE_PRECISION
        //type = PHY_DOUBLE;
        //#else
        data.vertexType = ScalarType.FLOAT
        //#endif
        data.vertexStride = mesh.vertexStride

        data.indexCount = mesh.numTriangles * 3

        data.indexData = mesh.triangleIndexBase
        data.indexStride = mesh.triangleIndexStride / 3
        data.indexType = mesh.indexType
        return data
    }

    override fun getLockedReadOnlyVertexIndexBase(subpart: Int): VertexData {
        return getLockedVertexIndexBase(subpart)
    }

    /**
     * unLockVertexBase finishes the access to a subpart of the triangle mesh.
     * Make a call to unLockVertexBase when the read and write access (using getLockedVertexIndexBase) is finished.
     */
    override fun unLockVertexBase(subpart: Int) {
        data.vertexData = null
        data.indexData = null
    }

    override fun unLockReadOnlyVertexBase(subpart: Int) {
        unLockVertexBase(subpart)
    }

    /**
     * getNumSubParts returns the number of seperate subparts.
     * Each subpart has a continuous array of vertices and indices.
     */
    override val numSubParts
        get(): Int {
            return indexedMeshArray.size
        }

    override fun preallocateVertices(numVertices: Int) {
    }

    override fun preallocateIndices(numIndices: Int) {
    }
}
