package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.BVHBuilder.createTexture
import me.anno.maths.bvh.TrisFiller.Companion.fillTris
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager

object TriangleTexture {

    private val LOGGER = LogManager.getLogger(TriangleTexture::class)

    // positionF32, pad0U32, normalF32, colorU32
    val PIXELS_PER_VERTEX = 2
    val PIXELS_PER_TRIANGLE = 3 * PIXELS_PER_VERTEX

    fun createTriangleTexture(blas: BLASNode, pixelsPerVertex: Int) =
        createTriangleTexture(listOf(blas), pixelsPerVertex)

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
}