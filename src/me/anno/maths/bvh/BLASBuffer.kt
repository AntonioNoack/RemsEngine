package me.anno.maths.bvh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.maths.bvh.BLASFiller.Companion.fillBLAS

object BLASBuffer {
    val blasAttr = bind(
        Attribute("min", 3),
        Attribute("v0", 1),
        Attribute("max", 3),
        Attribute("v1", 1)
    )

    fun createBLASBuffer(blasRoots: List<BLASNode>): ComputeBuffer {
        val numNodes = blasRoots.sumOf { blasRoot -> blasRoot.countNodes() }
        val data = ComputeBuffer("BLAS", blasAttr, numNodes)
        val nioBuffer = data.getOrCreateNioBuffer().asFloatBuffer()
        fillBLAS(blasRoots, triangleIndexMultiplier = 3, nioBuffer)
        data.cpuSideChanged()
        return data
    }
}