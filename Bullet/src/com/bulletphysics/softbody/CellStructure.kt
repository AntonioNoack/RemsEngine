package com.bulletphysics.softbody

import org.joml.Vector3f
import org.joml.Vector3i

abstract class CellStructure(
    val numCells: Vector3i,
    val numVerticesPerCell: Int,
) {
    abstract fun getVertex(cx: Int, cy: Int, cz: Int, localVertexIndex: Int): Int
    abstract fun getRestPose(cx: Int, cy: Int, cz: Int, localVertexIndex: Int, dst: Vector3f): Vector3f
    abstract fun getRestVolume(cx: Int, cy: Int, cz: Int): Float
}
