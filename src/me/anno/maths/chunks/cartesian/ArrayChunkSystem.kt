package me.anno.maths.chunks.cartesian

/**
 * a typical implementation for a chunk system would be an array backend
 * */
@Suppress("unused")
abstract class ArrayChunkSystem<Element>(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Element
) : ChunkSystem<Array<Any?>, Element>(bitsX, bitsY, bitsZ) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: Array<Any?>)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): Array<Any?> {
        val data = arrayOfNulls<Any>(size)
        data.fill(defaultElement) // fill should be way faster than using a lambda
        generateChunk(chunkX, chunkY, chunkZ, data)
        return data
    }

    override fun getElement(
        container: Array<Any?>,
        localX: Int, localY: Int, localZ: Int,
        index: Int
    ): Element {
        @Suppress("UNCHECKED_CAST")
        return container[index] as Element
    }

    override fun setElement(
        container: Array<Any?>,
        localX: Int, localY: Int, localZ: Int,
        index: Int,
        element: Element
    ): Boolean {
        container[index] = element
        return true
    }

}