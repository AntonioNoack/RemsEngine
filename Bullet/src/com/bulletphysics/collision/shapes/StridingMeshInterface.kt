package com.bulletphysics.collision.shapes

import cz.advel.stack.Stack
import org.joml.AABBd
import org.joml.Vector3d

/**
 * StridingMeshInterface is the abstract class for high performance access to
 * triangle meshes. It allows for sharing graphics and collision meshes.
 * Also, it provides locking/unlocking of graphics meshes that are in GPU memory.
 *
 * @author jezek2
 */
abstract class StridingMeshInterface {
    val scaling: Vector3d = Vector3d(1.0, 1.0, 1.0)

    fun internalProcessAllTriangles(callback: InternalTriangleIndexCallback) {
        val graphicsSubParts = this.numSubParts
        val triangle = arrayOf(Stack.newVec(), Stack.newVec(), Stack.newVec())

        val meshScaling = getScaling(Stack.newVec())

        for (part in 0 until graphicsSubParts) {
            val data = getLockedReadOnlyVertexIndexBase(part)
            var i = 0
            val cnt = data.indexCount / 3
            while (i < cnt) {
                data.getTriangle(i * 3, meshScaling, triangle)
                val (a, b, c) = triangle
                callback.internalProcessTriangleIndex(a, b, c, part, i)
                i++
            }
            unLockReadOnlyVertexBase(part)
        }
    }

    private class AabbCalculationCallback : InternalTriangleIndexCallback {
        val bounds = AABBd()

        override fun internalProcessTriangleIndex(
            a: Vector3d, b: Vector3d, c: Vector3d,
            partId: Int, triangleIndex: Int
        ) {
            bounds.union(a)
            bounds.union(b)
            bounds.union(c)
        }
    }

    fun calculateAabbBruteForce(aabbMin: Vector3d, aabbMax: Vector3d) {
        // first calculate the total aabb for all triangles
        val helper = AabbCalculationCallback()
        aabbMin.set(-1e308, -1e308, -1e308)
        aabbMax.set(1e308, 1e308, 1e308)
        internalProcessAllTriangles(helper)

        helper.bounds.getMin(aabbMin)
        helper.bounds.getMax(aabbMax)
    }

    /**
     * Get read and write access to a subpart of a triangle mesh.
     * This subpart has a continuous array of vertices and indices.
     * In this way the mesh can be handled as chunks of memory with striding
     * very similar to OpenGL vertexarray support.
     * Make a call to unLockVertexBase when the read and write access is finished.
     */
    abstract fun getLockedVertexIndexBase(subpart: Int /*=0*/): VertexData?

    abstract fun getLockedReadOnlyVertexIndexBase(subpart: Int /*=0*/): VertexData

    /**
     * unLockVertexBase finishes the access to a subpart of the triangle mesh.
     * Make a call to unLockVertexBase when the read and write access (using getLockedVertexIndexBase) is finished.
     */
    abstract fun unLockVertexBase(subpart: Int)

    abstract fun unLockReadOnlyVertexBase(subpart: Int)

    /**
     * getNumSubParts returns the number of seperate subparts.
     * Each subpart has a continuous array of vertices and indices.
     */
    abstract val numSubParts: Int

    abstract fun preallocateVertices(numVertices: Int)

    abstract fun preallocateIndices(numIndices: Int)

    fun getScaling(out: Vector3d): Vector3d {
        out.set(scaling)
        return out
    }

    fun setScaling(scaling: Vector3d) {
        this.scaling.set(scaling)
    }
}
