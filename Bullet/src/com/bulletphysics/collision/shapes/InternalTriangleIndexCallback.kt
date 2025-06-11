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
    fun internalProcessTriangleIndex(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int)
}
