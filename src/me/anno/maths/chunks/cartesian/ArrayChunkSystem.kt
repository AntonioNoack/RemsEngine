package me.anno.maths.chunks.cartesian

import me.anno.cache.Promise
import me.anno.utils.structures.lists.Lists.createArrayList

/**
 * a typical implementation for a chunk system would be an array backend
 * */
@Suppress("unused")
abstract class ArrayChunkSystem<Element>(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Element
) : ChunkSystem<ArrayList<Element>, Element>(bitsX, bitsY, bitsZ) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: ArrayList<Element>)

    override fun createChunk(
        chunkX: Int, chunkY: Int, chunkZ: Int, size: Int,
        result: Promise<ArrayList<Element>>
    ) {
        val data = createArrayList(size, defaultElement)
        data.fill(defaultElement) // fill should be way faster than using a lambda
        generateChunk(chunkX, chunkY, chunkZ, data)
        result.value = data
    }

    override fun getElement(
        container: ArrayList<Element>,
        localX: Int, localY: Int, localZ: Int, index: Int
    ): Element = container[index]

    override fun setElement(
        container: ArrayList<Element>,
        localX: Int, localY: Int, localZ: Int, index: Int,
        element: Element
    ): Boolean {
        container[index] = element
        return true
    }
}