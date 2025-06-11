package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.VectorUtil
import org.joml.Vector3d

/**
 * Allows accessing vertex data.
 *
 * @author jezek2
 */
abstract class VertexData {
    @JvmField
    var vertexCount: Int = 0

    @JvmField
    var indexCount: Int = 0

    abstract fun getVertex(index: Int, out: Vector3d): Vector3d
    abstract fun setVertex(index: Int, x: Double, y: Double, z: Double)

    @Suppress("unused")
    fun setVertex(index: Int, value: Vector3d) {
        setVertex(index, value.x, value.y, value.z)
    }

    abstract fun getIndex(index: Int): Int

    fun getTriangle(firstIndex: Int, scale: Vector3d, dstTriangle: Array<Vector3d>) {
        for (i in 0 until 3) {
            getVertex(getIndex(firstIndex + i), dstTriangle[i])
            VectorUtil.mul(dstTriangle[i], dstTriangle[i], scale)
        }
    }
}
