package me.anno.ecs.components.chunks.cartesian

/**
 * a typical implementation for a chunk system would be an array backend
 * */
abstract class IntArrayChunkSystem(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Int,
    initialCapacity: Int = 256
) : ChunkSystem<IntArray, Int>(bitsX, bitsY, bitsZ, initialCapacity) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: IntArray)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): IntArray {
        val data = IntArray(size)
        if (defaultElement != 0) data.fill(defaultElement)
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