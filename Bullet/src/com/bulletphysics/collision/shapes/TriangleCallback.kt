package com.bulletphysics.collision.shapes

import org.joml.Vector3d

/**
 * TriangleCallback provides a callback for each overlapping triangle when calling
 * processAllTriangles.
 *
 * This callback is called by processAllTriangles for all [ConcaveShape] derived
 * classes, such as [BvhTriangleMeshShape], [StaticPlaneShape] and
 * [HeightfieldTerrainShape].
 *
 * @author jezek2
 */
fun interface TriangleCallback {
    fun processTriangle(
        a: Vector3d, b: Vector3d, c: Vector3d,
        partId: Int, triangleIndex: Int
    )
}
