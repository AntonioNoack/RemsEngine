package me.anno.maths.chunks.cartesian

import me.anno.cache.Promise

/**
 * a typical implementation for a chunk system would be an array backend
 * */
abstract class ByteArrayChunkSystem(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Byte
) : ChunkSystem<ByteArray, Byte>(bitsX, bitsY, bitsZ) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: ByteArray)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int, result: Promise<ByteArray>) {
        val data = ByteArray(size)
        if (defaultElement != 0.toByte()) data.fill(defaultElement)
        generateChunk(chunkX, chunkY, chunkZ, data)
        result.value = data
    }

    override fun getElement(
        container: ByteArray,
        localX: Int, localY: Int, localZ: Int, index: Int
    ) = container[index]

    override fun setElement(
        container: ByteArray,
        localX: Int, localY: Int, localZ: Int, index: Int,
        element: Byte
    ): Boolean {
        container[index] = element
        return true
    }
}