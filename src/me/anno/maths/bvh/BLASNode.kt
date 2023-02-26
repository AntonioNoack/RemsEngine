package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.texture.Texture2D
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager
import org.joml.AABBf

abstract class BLASNode(bounds: AABBf) : BVHBuilder(bounds) {

    abstract fun findGeometryData(): GeometryData

    abstract fun forEach(run: (BLASNode) -> Unit)

    var triangleStartIndex = 0

    companion object {

        private val LOGGER = LogManager.getLogger(BLASNode::class)

        val PIXELS_PER_VERTEX = 2
        val PIXELS_PER_TRIANGLE = 3 * PIXELS_PER_VERTEX

        val PIXELS_PER_BLAS_NODE = 2

        fun createTriangleTexture(BLAS: BLASNode) = createTriangleTexture(listOf(BLAS))
        fun createBLASTexture(BLAS: BLASNode) = createBLASTexture(listOf(BLAS))

        fun createTriangleTexture(BLASs: List<BLASNode>): Texture2D {
            // to do if there are too many triangles, use a texture array?
            // 8k x 8k = 64M pixels = 64M vertices = 21M triangles
            // but that'd also need 64M * 16byte/vertex = 1GB of VRAM
            // todo most meshes don't need such high precision, maybe use u8 or u16 or fp16
            GFX.checkIsGFXThread()
            val buffers = BLASs.map { it.findGeometryData() } // positions without index
            // RGB is not supported by compute shaders (why ever...), so use RGBA
            val numTriangles = buffers.sumOf { it.positions.size / 9 }
            val texture = createTexture("triangles", numTriangles, PIXELS_PER_TRIANGLE)
            val bytesPerPixel = 16
            val buffer = Texture2D.bufferPool[texture.w * texture.h * bytesPerPixel, false, false]
            val data = buffer.asFloatBuffer()
            // write triangle into memory
            var pixelIndex = 0
            var triangleIndex = 0
            for (index in BLASs.indices) {
                val blasRoot = BLASs[index]
                blasRoot.forEach {
                    it.triangleStartIndex = triangleIndex
                }
                val data2 = buffers[index]
                val positions = data2.positions
                val normals = data2.normals
                val colors = data2.vertexColors
                val numLocalTriangles = positions.size / 9
                val numLocalVertices = numLocalTriangles * 3
                var k = 0
                for (i in 0 until numLocalVertices) {
                    data.position((i * PIXELS_PER_VERTEX + pixelIndex) * 4)
                    data.put(positions[k])
                    data.put(positions[k + 1])
                    data.put(positions[k + 2])
                    data.put(0f) // padding
                    if (PIXELS_PER_VERTEX > 1) {
                        data.put(normals[k])
                        data.put(normals[k + 1])
                        data.put(normals[k + 2])
                        data.put(Float.fromBits(if (colors != null) colors[i] else -1))
                    }
                    k += 3
                }
                pixelIndex += numLocalVertices * PIXELS_PER_VERTEX
                triangleIndex += numLocalTriangles
            }
            LOGGER.info("Filled triangles ${(pixelIndex * 4f / data.capacity()).formatPercent()}%")
            texture.createRGBA(data, buffer, false)
            Texture2D.bufferPool.returnBuffer(buffer)
            return texture
        }

        val triangleAttr = listOf(
            Attribute("pos", 3),
            Attribute("pad0", 1),
            Attribute("nor", 3),
            Attribute("color", 1)
        )

        fun createTriangleBuffer(BLASs: List<BLASNode>): ComputeBuffer {
            // to do if there are too many triangles, use a texture array?
            // 8k x 8k = 64M pixels = 64M vertices = 21M triangles
            // but that'd also need 64M * 16byte/vertex = 1GB of VRAM
            // todo most meshes don't need such high precision, maybe use u8 or u16 or fp16
            val buffers = BLASs.map { it.findGeometryData() } // positions without index
            // RGB is not supported by compute shaders (why ever...), so use RGBA
            val numTriangles = buffers.sumOf { it.positions.size / 9 }
            val buffer = ComputeBuffer(triangleAttr, numTriangles * 3)
            // write triangle into memory
            var pixelIndex = 0
            var triangleIndex = 0
            for (index in BLASs.indices) {
                val blasRoot = BLASs[index]
                blasRoot.forEach {
                    it.triangleStartIndex = triangleIndex
                }
                val data2 = buffers[index]
                val positions = data2.positions
                val normals = data2.normals
                val colors = data2.vertexColors
                val numLocalTriangles = positions.size / 9
                val numLocalVertices = numLocalTriangles * 3
                var k = 0
                for (i in 0 until numLocalVertices) {
                    buffer.nioBuffer!!.position((i * PIXELS_PER_VERTEX + pixelIndex) * 4)
                    buffer.put(positions[k])
                    buffer.put(positions[k + 1])
                    buffer.put(positions[k + 2])
                    buffer.put(0f) // padding
                    if (PIXELS_PER_VERTEX > 1) {
                        buffer.put(normals[k])
                        buffer.put(normals[k + 1])
                        buffer.put(normals[k + 2])
                        buffer.put(Float.fromBits(if (colors != null) colors[i] else -1))
                    }
                    k += 3
                }
                pixelIndex += numLocalVertices * PIXELS_PER_VERTEX
                triangleIndex += numLocalTriangles
            }
            return buffer
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
                        v1 = it.axis // not a leaf, 0-2
                    } else {
                        it as BLASLeaf
                        v0 = (it.start + it.triangleStartIndex) * PIXELS_PER_TRIANGLE
                        // >= 3, < 3 would mean not a single triangle, and that's invalid
                        v1 = it.length * PIXELS_PER_TRIANGLE
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

        val blasAttr = listOf(
            Attribute("min", 3),
            Attribute("v0", 1),
            Attribute("max", 3),
            Attribute("v1", 1)
        )

        fun createBLASBuffer(BLASs: List<BLASNode>): ComputeBuffer {

            // root node
            // aabb = 6x fp32
            // child0 can directly follow
            // child1 needs offset; 1x int32
            // leaf node
            // aabb = 6x fp32
            // start, length = 2x int32
            // for both types just use 8x4 = 32 bytes
            // we will find a place for markers about the type :)
            val numNodes = BLASs.sumOf { it.countNodes() }
            val texture = ComputeBuffer(blasAttr, numNodes)

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
                        v1 = it.axis // not a leaf, 0-2
                    } else {
                        it as BLASLeaf
                        v0 = (it.start + it.triangleStartIndex) * 3
                        // >= 3, < 3 would mean not a single triangle, and that's invalid
                        v1 = it.length * 3
                    }

                    val bounds = it.bounds
                    texture.put(bounds.minX)
                    texture.put(bounds.minY)
                    texture.put(bounds.minZ)
                    texture.put(Float.fromBits(v0))

                    texture.put(bounds.maxX)
                    texture.put(bounds.maxY)
                    texture.put(bounds.maxZ)
                    texture.put(Float.fromBits(v1))

                    i += 8

                }
            }

            return texture

        }

    }

}