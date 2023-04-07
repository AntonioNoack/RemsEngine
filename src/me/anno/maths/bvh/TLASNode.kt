package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.BVHBuilder.createTexture
import me.anno.utils.types.Buffers.skip
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4x3f

abstract class TLASNode(bounds: AABBf) : BVHNode(bounds) {

    abstract fun countTLASLeaves(): Int
    abstract fun forEach(run: (TLASNode) -> Unit)
    abstract fun collectMeshes(result: MutableCollection<BLASNode>)

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
                v1 = it.axis
            } else {
                it as TLASLeaf
                // offset is like an id
                v0 = it.blas.nodeId
                v1 = 3 // = max axis + 1
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
                m.putInto(data)
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

    fun createTLASBuffer(): ComputeBuffer {

        GFX.checkIsGFXThread()
        var nodeId = 0
        forEach {
            it.nodeId = nodeId++
        }

        val numNodes = nodeId
        val buffer = ComputeBuffer(tlasAttr, numNodes)
        val data = buffer.nioBuffer!!
        val floats = data.asFloatBuffer()

        fun writeMatrix(m: Matrix4x3f) {
            // send data column major
            // as that's the way for the constructor it seems

            floats.put(m.m00)
            floats.put(m.m01)
            floats.put(m.m02)

            floats.put(m.m10)
            floats.put(m.m11)
            floats.put(m.m12)

            floats.put(m.m20)
            floats.put(m.m21)
            floats.put(m.m22)

            floats.put(m.m30)
            floats.put(m.m31)
            floats.put(m.m32)

        }

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
                v1 = 3 // = max axis + 1
            }

            val b = it.bounds
            floats.put(b.minX)
            floats.put(b.minY)
            floats.put(b.minZ)
            floats.put(Float.fromBits(v0))

            floats.put(b.maxX)
            floats.put(b.maxY)
            floats.put(b.maxZ)
            floats.put(Float.fromBits(v1))

            if (it is TLASLeaf) {
                writeMatrix(it.worldToLocal)
                writeMatrix(it.localToWorld)
            } else {
                floats.skip(2 * 12)
            }

        }
        data.position(floats.position() * 4)
        return buffer
    }

    companion object {

        val tlasAttr = listOf(
            Attribute("min", 3),
            Attribute("v0", 1),
            Attribute("max", 3),
            Attribute("v1", 1),
            Attribute("worldToLocal", 12),
            Attribute("localToWorld", 12),
        )

        val PIXELS_PER_TLAS_NODE = 8
        private val LOGGER = LogManager.getLogger(TLASNode::class)

    }

}