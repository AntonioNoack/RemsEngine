package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.BVHBuilder.createTexture
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import java.nio.FloatBuffer

object TLASTexture {
    private val LOGGER = LogManager.getLogger(TLASTexture::class)
    val PIXELS_PER_TLAS_NODE = 8

    // we can create one wasteful texture, or two efficient ones...
    // since we have a binding limit, I'd go with a single one for now
    fun TLASNode.createTLASTexture(
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

            putBounds(data, node.bounds, v0, v1)

            if (node is TLASLeaf) {
                node.worldToLocal.putInto(data)
                node.localToWorld.putInto(data)
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

    fun putBounds(data: FloatBuffer, b: AABBf, v0: Int, v1: Int) {
        data.put(b.minX)
        data.put(b.minY)
        data.put(b.minZ)
        data.put(Float.fromBits(v0))

        data.put(b.maxX)
        data.put(b.maxY)
        data.put(b.maxZ)
        data.put(Float.fromBits(v1))
    }
}
