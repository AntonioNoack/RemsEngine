package me.anno.maths.chunks.cartesian

/**
 * a typical implementation for a chunk system would be an array backend
 * */
open class IntArrayChunkSystem(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Int
) : ChunkSystem<IntArray, Int>(bitsX, bitsY, bitsZ) {

    open fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: IntArray) {
        if (defaultElement != 0) chunk.fill(defaultElement)
    }

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): IntArray {
        val data = IntArray(size)
        generateChunk(chunkX, chunkY, chunkZ, data)
        return data
    }

    override fun getElement(
        container: IntArray,
        localX: Int, localY: Int, localZ: Int,
        index: Int
    ) = container[index]

    override fun setElement(
        container: IntArray,
        localX: Int, localY: Int, localZ: Int,
        index: Int,
        element: Int
    ): Boolean {
        container[index] = element
        return true
    }
}