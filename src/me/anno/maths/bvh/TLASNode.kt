package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4x3f

abstract class TLASNode(bounds: AABBf) : BVHBuilder(bounds) {
    abstract fun countTLASLeaves(): Int
    abstract fun forEach(run: (TLASNode) -> Unit)


    // we can create one wasteful texture, or two efficient ones...
    // since we have a binding limit, I'd go with a single one for now
    fun createTLASTexture(): Texture2D {

        GFX.checkIsGFXThread()
        var nodeId = 0
        forEach {
            it.nodeId = nodeId++
        }
        val numNodes = nodeId
        // branch:
        //   aabb + next -> 7 floats
        // leaf:
        //   aabb + 2x 4x3 matrix + child id -> 32 floats
        val pixelsPerNode = PIXELS_PER_TLAS_NODE
        val texture = createTexture("tlas", numNodes, pixelsPerNode)
        val buffer = Texture2D.bufferPool[texture.w * texture.h * 16, false, false]
        val data = buffer.asFloatBuffer()
        var i = 0

        forEach {

            val v0: Int
            val v1: Int

            if (it is TLASBranch) {
                v0 = it.n1.nodeId - it.nodeId
                v1 = 0
            } else {
                it as TLASLeaf
                // offset is like an id
                v0 = it.mesh.nodeId
                v1 = 1
            }

            val b = it.bounds
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
                val pos = data.position()
                m.get(data)
                data.position(pos + 12)
            }

            if (it is TLASLeaf) {
                writeMatrix(it.worldToLocal)
                writeMatrix(it.localToWorld)
            } else {
                // skip 24 elements
                data.position(data.position() + 24)
            }

            i += 32

        }

        LOGGER.info("Filled TLAS ${(i.toFloat() / data.capacity()).formatPercent()}%")
        texture.createRGBA(data, buffer, false)
        Texture2D.bufferPool.returnBuffer(buffer)
        return texture

    }

    companion object {
        val PIXELS_PER_TLAS_NODE = 8
        private val LOGGER = LogManager.getLogger(TLASNode::class)
    }

}