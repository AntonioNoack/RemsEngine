package me.anno.ecs.components.chunks.cartesian

/**
 * a typical implementation for a chunk system would be an array backend
 * */
abstract class ByteArrayChunkSystem(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Byte,
    initialCapacity: Int = 256
) : ChunkSystem<ByteArray, Byte>(bitsX, bitsY, bitsZ, initialCapacity) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: ByteArray)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): ByteArray {
        val data = ByteArray(size)
        if (defaultElement != 0.toByte()) data.fill(defaultElement)
        generateChunk(chunkX, chunkY, chunkZ, data)
        return data
    }

    override fun getElement(
        container: ByteArray,
        localX: Int, localY: Int, localZ: Int,
        index: Int
    ) = container[index]

    override fun setElement(
        container: ByteArray,
        localX: Int, localY: Int, localZ: Int,
        index: Int,
        element: Byte
    ): Boolean {
        container[index] = element
        return true
    }

}