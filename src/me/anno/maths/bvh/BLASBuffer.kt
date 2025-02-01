package me.anno.maths.bvh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.maths.bvh.BLASFiller.Companion.fillBLAS

object BLASBuffer {
    val blasAttr = listOf(
        Attribute("min", 3),
        Attribute("v0", 1),
        Attribute("max", 3),
        Attribute("v1", 1)
    )

    fun createBLASBuffer(nodes: List<BLASNode>): ComputeBuffer {
        val numNodes = nodes.sumOf { it.countNodes() }
        val data = ComputeBuffer("BLAS", blasAttr, numNodes)
        val nioBuffer = data.nioBuffer!!.asFloatBuffer()
        fillBLAS(nodes, 3, nioBuffer)
        data.isUpToDate = false
        return data
    }
}