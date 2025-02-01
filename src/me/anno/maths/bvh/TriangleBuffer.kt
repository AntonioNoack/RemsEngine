package me.anno.maths.bvh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.maths.bvh.TrisFiller.Companion.fillTris

object TriangleBuffer {

    val triangleAttr1 = listOf(
        Attribute("pos", 3),
        Attribute("pad0", 1),
    )

    val triangleAttr2 = triangleAttr1 + listOf(
        Attribute("nor", 3),
        Attribute("color", 1)
    )

    fun createTriangleBuffer(roots: List<BLASNode>, pixelsPerVertex: Int): ComputeBuffer {
        // to do if there are too many triangles, use a texture array?
        // 8k x 8k = 64M pixels = 64M vertices = 21M triangles
        // but that'd also need 64M * 16byte/vertex = 1GB of VRAM
        // to do most meshes don't need such high precision, maybe use u8 or u16 or fp16
        val buffers = roots.map { it.findGeometryData() } // positions without index
        // RGB is not supported by compute shaders (why ever...), so use RGBA
        val numTriangles = buffers.sumOf { it.indices.size / 3 }
        val attr = when (pixelsPerVertex) {
            1 -> triangleAttr1
            2 -> triangleAttr2
            else -> throw NotImplementedError("px/vertex")
        }
        val buffer = ComputeBuffer("BLAS", attr, numTriangles * 3)
        // write triangle into memory
        fillTris(roots, buffers, buffer.nioBuffer!!.asFloatBuffer(), pixelsPerVertex)
        buffer.isUpToDate = false
        return buffer
    }
}