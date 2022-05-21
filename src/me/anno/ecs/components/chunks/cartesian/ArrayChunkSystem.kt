package me.anno.ecs.components.chunks.cartesian

/**
 * a typical implementation for a chunk system would be an array backend
 * */
abstract class ArrayChunkSystem<Element>(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Element,
    initialCapacity: Int = 256
) : ChunkSystem<Array<Element>, Element>(bitsX, bitsY, bitsZ, initialCapacity) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: Array<Element>)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): Array<Element> {
        @Suppress("UNCHECKED_CAST")
        val data = arrayOfNulls<Any>(size) as Array<Element>
        data.fill(defaultElement)
        generateChunk(chunkX, chunkY, chunkZ, data)
        return data
    }

    override fun getElement(
        container: Array<Element>,
        localX: Int, localY: Int, localZ: Int,
        index: Int
    ): Element {
        return container[index]
    }

    override fun setElement(
        container: Array<Element>,
        localX: Int, localY: Int, localZ: Int,
        index: Int,
        element: Element
    ): Boolean {
        container[index] = element
        return true
    }

}