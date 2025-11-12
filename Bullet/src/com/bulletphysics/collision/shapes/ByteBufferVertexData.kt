package com.bulletphysics.collision.shapes

import java.nio.ByteBuffer
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * @author jezek2
 */
class ByteBufferVertexData : VertexData() {

    @JvmField
    var vertexData: ByteBuffer? = null

    @JvmField
    var vertexStride: Int = 0

    @JvmField
    var vertexType: ScalarType? = null

    @JvmField
    var indexData: ByteBuffer? = null

    @JvmField
    var indexStride: Int = 0

    @JvmField
    var indexType: ScalarType? = null

    override fun getVertex(index: Int, out: Vector3f): Vector3f {
        val off = index * vertexStride
        val vertexData = vertexData!!
        out.x = vertexData.getFloat(off)
        out.y = vertexData.getFloat(off + 4)
        out.z = vertexData.getFloat(off + 8)
        return out
    }

    override fun setVertex(index: Int, x: Float, y: Float, z: Float) {
        val off = index * vertexStride
        val vertexData = vertexData!!
        vertexData.putFloat(off, x)
        vertexData.putFloat(off + 4, y)
        vertexData.putFloat(off + 8, z)
    }

    override fun getIndex(index: Int): Int {
        val indexData = indexData!!
        return when (indexType) {
            ScalarType.SHORT -> indexData.getShort(index * indexStride).toInt() and 0xFFFF
            ScalarType.INTEGER -> indexData.getInt(index * indexStride)
            else -> throw IllegalStateException("Indices type must be short or integer")
        }
    }
}
