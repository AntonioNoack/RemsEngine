package me.anno.maths.chunks.cartesian

import me.anno.cache.Promise

/**
 * a typical implementation for a chunk system would be an array backend
 * */
open class IntArrayChunkSystem(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Int
) : ChunkSystem<IntArray, Int>(bitsX, bitsY, bitsZ) {

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int, result: Promise<IntArray>) {
        result.value = IntArray(size).apply {
            if (defaultElement != 0) fill(defaultElement)
        }
    }

    override fun getElement(
        container: IntArray,
        localX: Int, localY: Int, localZ: Int, index: Int
    ) = container[index]

    override fun setElement(
        container: IntArray,
        localX: Int, localY: Int, localZ: Int, index: Int,
        element: Int
    ): Boolean {
        container[index] = element
        return true
    }
}