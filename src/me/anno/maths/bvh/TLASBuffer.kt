package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.maths.bvh.TLASTexture.putBounds

object TLASBuffer {

    val tlasAttr0 = bind(
        Attribute("min", 3),
        Attribute("v0", 1),
        Attribute("max", 3),
        Attribute("v1", 1),
    )

    val tlasAttr1 = bind(
        Attribute("worldToLocal", 12),
        Attribute("localToWorld", 12),
    )

    fun createTLASBuffer(root: TLASNode): Pair<ComputeBuffer, ComputeBuffer> {

        GFX.checkIsGFXThread()
        var nodeId = 0
        root.forEach { node ->
            node.nodeId = nodeId++
        }

        val numNodes = nodeId
        val baseBuffer = ComputeBuffer("TLAS-base", tlasAttr0, numNodes)
        val transformBuffer = ComputeBuffer("TLAS-trans", tlasAttr1, root.countTLASLeaves())
        val baseData = baseBuffer.getOrCreateNioBuffer()
        val transformData = transformBuffer.getOrCreateNioBuffer()
        val f0 = baseData.asFloatBuffer()
        val f1 = transformData.asFloatBuffer()

        var leafId = 3
        root.forEach { node ->

            val v0: Int
            val v1: Int

            if (node is TLASBranch) {
                v0 = node.n1.nodeId - node.nodeId
                v1 = node.axis
            } else {
                node as TLASLeaf
                // offset is like an ID
                v0 = node.blas.nodeId
                v1 = leafId++ // = max axis + 1 + transform index
            }

            putBounds(f0, node.bounds, v0, v1)

            if (node is TLASLeaf) {
                node.worldToLocal.putInto(f1)
                node.localToWorld.putInto(f1)
            }
        }
        baseData.position(f0.position() * 4)
        transformData.position(f1.position() * 4)
        return Pair(baseBuffer, transformBuffer)
    }
}