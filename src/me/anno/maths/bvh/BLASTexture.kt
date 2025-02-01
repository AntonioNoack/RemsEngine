package me.anno.maths.bvh

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.BLASFiller.Companion.fillBLAS
import me.anno.maths.bvh.BVHBuilder.createTexture
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager

object BLASTexture {

    private val LOGGER = LogManager.getLogger(BLASTexture::class)

    val PIXELS_PER_BLAS_NODE = 2

    fun createBLASTexture(blas: BLASNode, pixelsPerTriangle: Int) =
        createBLASTexture(listOf(blas), pixelsPerTriangle)

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
}