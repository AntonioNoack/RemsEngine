package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.BVHBuilder.createTexture
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4x3f
import java.nio.FloatBuffer

abstract class TLASNode(bounds: AABBf) : BVHNode(bounds) {

    abstract fun countTLASLeaves(): Int
    abstract fun forEach(run: (TLASNode) -> Unit)
    abstract fun collectMeshes(result: MutableCollection<BLASNode>)

    // we can create one wasteful texture, or two efficient ones...
    // since we have a binding limit, I'd go with a single one for now
    fun createTLASTexture(
        pixelsPerNode: Int = PIXELS_PER_TLAS_NODE,
        addExtraData: ((TLASNode, FloatBuffer) -> Unit)? = null
    ): Texture2D {

        GFX.checkIsGFXThread()
        var nodeId = 0
        forEach {
            it.nodeId = nodeId++
        }
        val numNodes = nodeId
        // branch:
        //   aabb + next -> 7 floats
        // leaf:
        //   aabb + 2x 4x3 matrix + child id -> 32 floats = 8 pixels
        val texture = createTexture("tlas", numNodes, pixelsPerNode)
        val buffer = Pools.byteBufferPool[texture.width * texture.height * 16, false, false]
        val data = buffer.asFloatBuffer()

        forEach { node ->

            val v0: Int
            val v1: Int

            if (node is TLASBranch) {
                v0 = node.n1.nodeId - node.nodeId
                v1 = node.axis
            } else {
                node as TLASLeaf
                // offset is like an id
                v0 = node.blas.nodeId
                v1 = 3 // = max axis + 1
            }

            val b = node.bounds
            data.put(b.minX)
            data.put(b.minY)
            data.put(b.minZ)
            data.put(Float.fromBits(v0))

            data.put(b.maxX)
            data.put(b.maxY)
            data.put(b.maxZ)
            data.put(Float.fromBits(v1))

            fun writeMatrix(m: Matrix4x3f) {
                // send data column major
                // as that's the way for the constructor it seems
                m.putInto(data)
            }

            if (node is TLASLeaf) {
                writeMatrix(node.worldToLocal)
                writeMatrix(node.localToWorld)
            } else {
                // skip 24 elements
                data.position(data.position() + 24)
            }

            addExtraData?.invoke(node, data)

        }

        LOGGER.info("Filled TLAS ${(data.position().toFloat() / data.capacity()).formatPercent()}%")
        texture.createRGBA(data, buffer, false)
        Pools.byteBufferPool.returnBuffer(buffer)
        return texture
    }

    fun createTLASBuffer(): Pair<ComputeBuffer, ComputeBuffer> {

        GFX.checkIsGFXThread()
        var nodeId = 0
        var numLeafs = 0
        forEach {
            it.nodeId = nodeId++
            if (it is TLASLeaf) numLeafs++
        }

        val numNodes = nodeId
        val baseBuffer = ComputeBuffer("TLAS-base", tlasAttr0, numNodes)
        val transformBuffer = ComputeBuffer("TLAS-trans", tlasAttr1, numLeafs)
        val baseData = baseBuffer.nioBuffer!!
        val transformData = transformBuffer.nioBuffer!!
        val f0 = baseData.asFloatBuffer()
        val f1 = transformData.asFloatBuffer()

        fun writeMatrix(m: Matrix4x3f) {
            // send data column major
            // as that's the way for the constructor it seems

            f1.put(m.m00)
            f1.put(m.m01)
            f1.put(m.m02)

            f1.put(m.m10)
            f1.put(m.m11)
            f1.put(m.m12)

            f1.put(m.m20)
            f1.put(m.m21)
            f1.put(m.m22)

            f1.put(m.m30)
            f1.put(m.m31)
            f1.put(m.m32)
        }

        numLeafs = 3
        forEach {

            val v0: Int
            val v1: Int

            if (it is TLASBranch) {
                v0 = it.n1.nodeId - it.nodeId
                v1 = it.axis
            } else {
                it as TLASLeaf
                // offset is like an ID
                v0 = it.blas.nodeId
                v1 = numLeafs++ // = max axis + 1 + transform index
            }

            val b = it.bounds
            f0.put(b.minX)
            f0.put(b.minY)
            f0.put(b.minZ)
            f0.put(Float.fromBits(v0))

            f0.put(b.maxX)
            f0.put(b.maxY)
            f0.put(b.maxZ)
            f0.put(Float.fromBits(v1))

            if (it is TLASLeaf) {
                writeMatrix(it.worldToLocal)
                writeMatrix(it.localToWorld)
            }

        }
        baseData.position(f0.position() * 4)
        transformData.position(f1.position() * 4)
        return Pair(baseBuffer, transformBuffer)
    }

    companion object {

        val tlasAttr0 = listOf(
            Attribute("min", 3),
            Attribute("v0", 1),
            Attribute("max", 3),
            Attribute("v1", 1),
        )

        val tlasAttr1 = listOf(
            Attribute("worldToLocal", 12),
            Attribute("localToWorld", 12),
        )

        val PIXELS_PER_TLAS_NODE = 8
        private val LOGGER = LogManager.getLogger(TLASNode::class)
    }
}