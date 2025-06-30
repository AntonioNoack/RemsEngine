package me.anno.maths.chunks.cartesian

import speiger.primitivecollections.IntToObjectHashMap

/**
 * a semi-typical implementation for a chunk system could be a map backend
 * */
abstract class MapChunkSystem<Element>(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Element
) : ChunkSystem<IntToObjectHashMap<Element>, Element>(bitsX, bitsY, bitsZ) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: IntToObjectHashMap<Element>)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): IntToObjectHashMap<Element> {
        val data = IntToObjectHashMap<Element>()
        generateChunk(chunkX, chunkY, chunkZ, data)
        return data
    }

    override fun getElement(
        container: IntToObjectHashMap<Element>,
        localX: Int, localY: Int, localZ: Int, index: Int
    ): Element {
        return container[index] ?: defaultElement
    }

    override fun setElement(
        container: IntToObjectHashMap<Element>,
        localX: Int, localY: Int, localZ: Int,
        index: Int, element: Element
    ): Boolean {
        container[index] = element
        return true
    }
}