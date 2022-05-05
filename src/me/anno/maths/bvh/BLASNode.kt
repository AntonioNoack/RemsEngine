package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager
import org.joml.AABBf

abstract class BLASNode(bounds: AABBf) : BVHBuilder(bounds) {

    abstract fun findCompactPositions(): FloatArray
    abstract fun forEach(run: (BLASNode) -> Unit)

    var triangleStartIndex = 0

    fun createBLASTexture(): Texture2D {
        GFX.checkIsGFXThread()
        // root node
        // aabb = 6x fp32
        // child0 can directly follow;
        // child1 needs offset; 1x int32
        // leaf node
        // aabb = 6x fp32
        // start, length = 2x int32
        // for both types just use 8x4 = 32 bytes
        // we will find a place for markers about the type :)
        val pixelsPerNode = PIXELS_PER_BLAS_NODE
        val numNodes = countNodes()
        val texture = createTexture("blas", numNodes, pixelsPerNode)
        val buffer = Texture2D.bufferPool[texture.w * texture.h * 16, false, false]
        val data = buffer.asFloatBuffer()
        var i = 0
        // assign indices to all nodes
        forEach {
            it.nodeId = i++
        }
        i = 0
        forEach {

            val v0: Int
            val v1: Int
            when (it) {
                is BLASBranch -> {
                    v0 = it.n1.nodeId - it.nodeId // next node
                    v1 = 0  // not a leaf
                }
                is BLASLeaf -> {
                    v0 = it.start
                    v1 = it.length
                }
                else -> throw RuntimeException()
            }

            val bounds = it.bounds
            data.put(bounds.minX)
            data.put(bounds.minY)
            data.put(bounds.minZ)
            data.put(Float.fromBits(v0))

            data.put(bounds.maxX)
            data.put(bounds.maxY)
            data.put(bounds.maxZ)
            data.put(Float.fromBits(v1))

            i += 8

        }

        LOGGER.info("Filled BLAS ${(i.toFloat() / data.capacity()).formatPercent()}%")
        texture.createRGBA(data, buffer, false)
        Texture2D.bufferPool.returnBuffer(buffer)
        return texture
    }

    fun createTriangleTexture(): Texture2D {
        GFX.checkIsGFXThread()
        val positions = findCompactPositions() // positions without index
        val pixelsPerTriangle = 3 // 9 floats -> 3 pixels with RGB or RGBA are needed
        // RGB is not supported by compute shaders (why ever...), so use RGBA
        val numTriangles = positions.size / 9
        val texture = createTexture("triangles", numTriangles, pixelsPerTriangle)
        val buffer = Texture2D.bufferPool[texture.w * texture.h * 16, false, false]
        val data = buffer.asFloatBuffer()
        // write triangle into memory
        var k = 0
        for (i in 0 until numTriangles * 3) {
            data.put(positions[k++])
            data.put(positions[k++])
            data.put(positions[k++])
            data.put(0f) // padding
        }
        texture.createRGBA(data, buffer, false)
        Texture2D.bufferPool.returnBuffer(buffer)
        return texture
    }

    companion object {

        private val LOGGER = LogManager.getLogger(BLASNode::class)

        val PIXELS_PER_VERTEX = 1
        val PIXELS_PER_TRIANGLE = 3 * PIXELS_PER_VERTEX

        val PIXELS_PER_BLAS_NODE = 2

        fun createTriangleTexture(blasRoots: List<BLASNode>): Texture2D {
            // to do if there are too many triangles, use a texture array?
            // 8k x 8k = 64M pixels = 64M vertices = 21M triangles
            // but that'd also need 64M * 16byte/vertex = 1GB of VRAM
            // todo most meshes don't need such high precision, maybe use u8 or u16 or fp16
            GFX.checkIsGFXThread()
            val buffers = blasRoots.map { it.findCompactPositions() } // positions without index
            val pixelsPerTriangle = 3 // 9 floats -> 3 pixels with RGB or RGBA are needed
            // RGB is not supported by compute shaders (why ever...), so use RGBA
            val numTriangles = buffers.sumOf { it.size / 9 }
            val texture = createTexture("triangles", numTriangles, pixelsPerTriangle)
            val buffer = Texture2D.bufferPool[texture.w * texture.h * 16, false, false]
            val data = buffer.asFloatBuffer()
            // write triangle into memory
            var pixelIndex = 0
            var triangleIndex = 0
            for (index in blasRoots.indices) {
                val blasRoot = blasRoots[index]
                blasRoot.forEach {
                    it.triangleStartIndex = triangleIndex
                }
                val positions = buffers[index]
                val numLocalTriangles = positions.size / 9
                val numLocalVertices = numLocalTriangles * 3
                var k = 0
                for (i in 0 until numLocalVertices) {
                    data.put(positions[k++])
                    data.put(positions[k++])
                    data.put(positions[k++])
                    data.put(0f) // padding
                }
                pixelIndex += numLocalVertices
                triangleIndex += numLocalTriangles
            }
            LOGGER.info("Filled triangles ${(pixelIndex * 4f / data.capacity()).formatPercent()}%")
            texture.createRGBA(data, buffer, false)
            Texture2D.bufferPool.returnBuffer(buffer)
            return texture
        }

        fun createBLASTexture(BLASs: List<BLASNode>): Texture2D {

            GFX.checkIsGFXThread()
            // root node
            // aabb = 6x fp32
            // child0 can directly follow
            // child1 needs offset; 1x int32
            // leaf node
            // aabb = 6x fp32
            // start, length = 2x int32
            // for both types just use 8x4 = 32 bytes
            // we will find a place for markers about the type :)
            val pixelsPerNode = PIXELS_PER_BLAS_NODE
            val numNodes = BLASs.sumOf { it.countNodes() }
            val texture = createTexture("blas", numNodes, pixelsPerNode)
            val buffer = Texture2D.bufferPool[texture.w * texture.h * 16, false, false]
            val data = buffer.asFloatBuffer()

            var i = 0
            var nextId = 0
            for (index in BLASs.indices) {
                val bvh = BLASs[index]
                bvh.forEach {
                    it.nodeId = nextId++
                }
            }
            // assign indices to all nodes
            for (index in BLASs.indices) {
                val bvh = BLASs[index]
                bvh.forEach {

                    val v0: Int
                    val v1: Int

                    if (it is BLASBranch) {
                        v0 = it.n1.nodeId - it.nodeId // next node
                        v1 = 0  // not a leaf
                    } else {
                        it as BLASLeaf
                        v0 = it.start + it.triangleStartIndex
                        v1 = it.length
                    }

                    val bounds = it.bounds
                    data.put(bounds.minX)
                    data.put(bounds.minY)
                    data.put(bounds.minZ)
                    data.put(Float.fromBits(v0))

                    data.put(bounds.maxX)
                    data.put(bounds.maxY)
                    data.put(bounds.maxZ)
                    data.put(Float.fromBits(v1))

                    i += 8

                }
            }

            LOGGER.info("Filled BLAS ${(i.toFloat() / data.capacity()).formatPercent()}%")
            texture.createRGBA(data, buffer, false)
            Texture2D.bufferPool.returnBuffer(buffer)
            return texture

        }


    }

}