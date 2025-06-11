package com.bulletphysics.collision.shapes

import java.nio.ByteBuffer

/**
 * IndexedMesh indexes into existing vertex and index arrays, in a similar way to
 * OpenGL's glDrawElements. Instead of the number of indices, we pass the number
 * of triangles.
 *
 * @author jezek2
 */
class IndexedMesh {
    @JvmField
	var numTriangles: Int = 0
    @JvmField
	var triangleIndexBase: ByteBuffer? = null
    @JvmField
	var triangleIndexStride: Int = 0
    @JvmField
	var numVertices: Int = 0
    @JvmField
	var vertexBase: ByteBuffer? = null
    @JvmField
	var vertexStride: Int = 0

    // The index type is set when adding an indexed mesh to the
    // TriangleIndexVertexArray, do not set it manually
	@JvmField
	var indexType: ScalarType? = null
}
