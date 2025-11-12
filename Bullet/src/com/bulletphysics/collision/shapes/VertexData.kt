package com.bulletphysics.collision.shapes

import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

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

    abstract fun getVertex(index: Int, out: Vector3f): Vector3f
    abstract fun setVertex(index: Int, x: Float, y: Float, z: Float)

    fun getVertex(index: Int, out: Vector3d): Vector3d {
        val tmp = Stack.newVec3f()
        out.set(getVertex(index, tmp))
        Stack.subVec3f(1)
        return out
    }

    @Suppress("unused")
    fun setVertex(index: Int, value: Vector3f) {
        setVertex(index, value.x, value.y, value.z)
    }

    abstract fun getIndex(index: Int): Int

    fun getTriangle(firstIndex: Int, scale: Vector3f, dstTriangle: Array<Vector3d>) {
        for (i in 0 until 3) {
            getVertex(getIndex(firstIndex + i), dstTriangle[i])
            dstTriangle[i].mul(scale)
        }
    }
}
