package com.bulletphysics.softbody

import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3f
import org.joml.Vector3i

class CuboidCellStructure(val halfExtends: Vector3f, val numVertices: Vector3i) :
    CellStructure(Vector3i(numVertices).sub(1), 8) {

    val cellVolume = halfExtends.product() * 8f

    fun idx(x: Int, y: Int, z: Int) = x + (y + z * numVertices.y) * numVertices.x

    override fun getVertex(cx: Int, cy: Int, cz: Int, localVertexIndex: Int): Int {
        val xi = cx + localVertexIndex.and(1)
        val yi = cy + localVertexIndex.shr(1).and(1)
        val zi = cz + localVertexIndex.shr(2).and(1)
        return idx(xi, yi, zi)
    }

    override fun getRestPose(cx: Int, cy: Int, cz: Int, localVertexIndex: Int, dst: Vector3f): Vector3f {
        return dst.set(
            if (localVertexIndex.hasFlag(1)) -halfExtends.x else halfExtends.x,
            if (localVertexIndex.hasFlag(2)) -halfExtends.y else halfExtends.y,
            if (localVertexIndex.hasFlag(4)) -halfExtends.z else halfExtends.z
        )
    }

    override fun getRestVolume(cx: Int, cy: Int, cz: Int): Float = cellVolume
}