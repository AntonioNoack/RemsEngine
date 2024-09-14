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
import java.nio.FloatBuffer

abstract class BLASNode(bounds: AABBf) : BVHNode(bounds) {

    abstract fun findGeometryData(): GeometryData

    abstract fun forEach(run: (BLASNode) -> Unit)

    var triangleStartIndex = 0

    companion object {

        private val LOGGER = LogManager.getLogger(BLASNode::class)

        // positionF32, pad0U32, normalF32, colorU32
        val PIXELS_PER_VERTEX = 2
        val PIXELS_PER_TRIANGLE = 3 * PIXELS_PER_VERTEX

        val PIXELS_PER_BLAS_NODE = 2

        fun createTriangleTexture(blas: BLASNode, pixelsPerVertex: Int) =
            createTriangleTexture(listOf(blas), pixelsPerVertex)

        fun createBLASTexture(blas: BLASNode, pixelsPerTriangle: Int) =
            createBLASTexture(listOf(blas), pixelsPerTriangle)

        fun createTriangleTexture(blasList: List<BLASNode>, pixelsPerVertex: Int): Texture2D {
            // to do if there are too many triangles, use a texture array?
            // 8k x 8k = 64M pixels = 64M vertices = 21M triangles
            // but that'd also need 64M * 16byte/vertex = 1GB of VRAM
            // to do most meshes don't need such high precision, maybe use u8 or u16 or fp16
            GFX.checkIsGFXThread()
            val buffers = blasList.map { it.findGeometryData() } // positions without index
            // RGB is not supported by compute shaders (why ever...), so use RGBA
            val numTriangles = buffers.sumOf { it.indices.size / 3 }
            val texture = createTexture("triangles", numTriangles, PIXELS_PER_TRIANGLE)
            val bytesPerPixel = 16 // 4 channels * 4 bytes / float
            val buffer = Pools.byteBufferPool[texture.width * texture.height * bytesPerPixel, false, false]
            val data = buffer.asFloatBuffer()
            fillTris(blasList, buffers, data, pixelsPerVertex)
            LOGGER.info("Filled triangles ${(data.position().toFloat() / data.capacity()).formatPercent()}%, $texture")
            texture.createRGBA(data, buffer, false)
            Pools.byteBufferPool.returnBuffer(buffer)
            return texture
        }

        fun fillTris(
            roots: List<BLASNode>, buffers: List<GeometryData>,
            data: FloatBuffer, pixelsPerVertex: Int
        ): Int {
            return fillTris(roots, buffers) { geometry, vertexIndex ->
                val positions = geometry.positions
                val k = vertexIndex * 3
                data.put(positions, k, 3)
                data.put(0f) // unused padding
                if (pixelsPerVertex > 1) {
                    val normals = geometry.normals
                    val colors = geometry.vertexColors
                    val color = if (colors != null) colors[vertexIndex] else -1
                    data.put(normals, k, 3)
                    data.put(Float.fromBits(color))
                }
            }
        }

        fun fillTris(roots: List<BLASNode>, buffers: List<GeometryData>, callback: TrisFiller): Int {
            // write triangle into memory
            var triangleIndex = 0
            for (index in roots.indices) {
                val blasRoot = roots[index]
                blasRoot.forEach {
                    it.triangleStartIndex = triangleIndex
                }
                val geometry = buffers[index]
                val indices = geometry.indices
                for (i in indices.indices step 3) {
                    callback.fill(geometry, indices[i])
                    callback.fill(geometry, indices[i + 1])
                    callback.fill(geometry, indices[i + 2])
                }
                triangleIndex += indices.size / 3
            }
            return triangleIndex
        }

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

        fun createBLASTexture(roots: List<BLASNode>, pixelsPerTriangle: Int): Texture2D {

            GFX.checkIsGFXThread()

            val pixelsPerNode = PIXELS_PER_BLAS_NODE
            val numNodes = roots.sumOf { it.countNodes() }
            val texture = createTexture("blas", numNodes, pixelsPerNode)
            val buffer = Pools.byteBufferPool[texture.width * texture.height * 16, false, false]
            val data = buffer.asFloatBuffer()

            fillBLAS(roots, pixelsPerTriangle, data)

            val usedFloats = numNodes * 8
            LOGGER.info("Filled BLAS ${(usedFloats.toFloat() / data.capacity()).formatPercent()}%, $texture")
            texture.createRGBA(data, buffer, false)
            Pools.byteBufferPool.returnBuffer(buffer)
            return texture
        }

        fun createBLASBuffer(nodes: List<BLASNode>): ComputeBuffer {
            val numNodes = nodes.sumOf { it.countNodes() }
            val data = ComputeBuffer("BLAS", blasAttr, numNodes)
            val nioBuffer = data.nioBuffer!!.asFloatBuffer()
            fillBLAS(nodes, 3, nioBuffer)
            data.isUpToDate = false
            return data
        }

        val blasAttr = listOf(
            Attribute("min", 3),
            Attribute("v0", 1),
            Attribute("max", 3),
            Attribute("v1", 1)
        )

        private fun fillBLASNode(data: FloatBuffer, v0: Int, v1: Int, bounds: AABBf) {

            // root node
            // aabb = 6x fp32
            // child0 can directly follow
            // child1 needs offset; 1x int32
            // leaf node
            // aabb = 6x fp32
            // start, length = 2x int32
            // for both types just use 8x4 = 32 bytes
            // we will find a place for markers about the type :)

            data.put(bounds.minX)
            data.put(bounds.minY)
            data.put(bounds.minZ)
            data.put(Float.fromBits(v0))

            data.put(bounds.maxX)
            data.put(bounds.maxY)
            data.put(bounds.maxZ)
            data.put(Float.fromBits(v1))
        }

        fun interface BLASFiller {
            fun fill(v0: Int, v1: Int, bounds: AABBf)
        }

        fun interface TrisFiller {
            fun fill(geometry: GeometryData, vertexIndex: Int)
        }

        fun fillBLAS(
            roots: List<BLASNode>,
            multiplier: Int,
            data: FloatBuffer
        ) = fillBLAS(roots, multiplier) { v0, v1, bounds ->
            fillBLASNode(data, v0, v1, bounds)
        }

        fun fillBLAS(
            roots: List<BLASNode>,
            multiplier: Int,
            callback: BLASFiller
        ) {

            if (multiplier < 3) {
                throw IllegalArgumentException("Cannot represent x,y,z this way.")
            }

            var nextId = 0
            for (index in roots.indices) {
                val bvh = roots[index]
                bvh.forEach {
                    it.nodeId = nextId++
                }
            }

            // assign indices to all nodes
            for (index in roots.indices) {
                val bvh = roots[index]
                bvh.forEach {

                    val v0: Int
                    val v1: Int

                    if (it is BLASBranch) {
                        v0 = it.n1.nodeId - it.nodeId // next node
                        v1 = it.axis // not a leaf, 0-2
                    } else {
                        it as BLASLeaf
                        v0 = (it.start + it.triangleStartIndex) * multiplier
                        // >= 3, < 3 would mean not a single triangle, and that's invalid
                        v1 = it.length * multiplier
                    }

                    val bounds = it.bounds
                    callback.fill(v0, v1, bounds)

                }
            }
        }
    }
}