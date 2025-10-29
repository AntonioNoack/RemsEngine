package com.bulletphysics.collision.shapes

import org.joml.Vector3d

/**
 * Callback for internal processing of triangles.
 *
 * @see StridingMeshInterface.internalProcessAllTriangles
 *
 * @author jezek2
 */
interface InternalTriangleIndexCallback {
    fun internalProcessTriangleIndex(a: Vector3d, b: Vector3d, c: Vector3d, partId: Int, triangleIndex: Int)
}
