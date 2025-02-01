package me.anno.maths.bvh

import java.nio.FloatBuffer

fun interface TrisFiller {
    fun fill(geometry: GeometryData, vertexIndex: Int)

    companion object {

        fun fillTris(
            roots: List<BLASNode>, buffers: List<GeometryData>,
            data: FloatBuffer, pixelsPerVertex: Int
        ): Int {
            return fillTris(roots, buffers) { geometry, vertexIndex ->
                val positions = geometry.positions
                val k = vertexIndex * 3
                data.put(positions, k, 3)
                data.put(0f) // unused padding
                if (pixelsPerVertex > 1) {
                    val normals = geometry.normals
                    val colors = geometry.vertexColors
                    val color = if (colors != null) colors[vertexIndex] else -1
                    data.put(normals, k, 3)
                    data.put(Float.fromBits(color))
                }
            }
        }

        fun fillTris(roots: List<BLASNode>, buffers: List<GeometryData>, callback: TrisFiller): Int {
            // write triangle into memory
            var triangleIndex = 0
            for (index in roots.indices) {
                val blasRoot = roots[index]
                blasRoot.forEach { blasNode ->
                    blasNode.triangleStartIndex = triangleIndex
                }
                val geometry = buffers[index]
                val indices = geometry.indices
                for (i in indices.indices step 3) {
                    callback.fill(geometry, indices[i])
                    callback.fill(geometry, indices[i + 1])
                    callback.fill(geometry, indices[i + 2])
                }
                triangleIndex += indices.size / 3
            }
            return triangleIndex
        }
    }
}