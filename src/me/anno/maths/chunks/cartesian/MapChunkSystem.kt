package me.anno.maths.chunks.cartesian

/**
 * a semi-typical implementation for a chunk system could be a map backend
 * */
abstract class MapChunkSystem<Element>(
    bitsX: Int, bitsY: Int, bitsZ: Int,
    val defaultElement: Element
) : ChunkSystem<HashMap<Int, Element>, Element>(bitsX, bitsY, bitsZ) {

    abstract fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: HashMap<Int, Element>)

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): HashMap<Int, Element> {
        val data = HashMap<Int, Element>()
        generateChunk(chunkX, chunkY, chunkZ, data)
        return data
    }

    override fun getElement(
        container: HashMap<Int, Element>,
        localX: Int,
        localY: Int,
        localZ: Int,
        index: Int
    ): Element {
        return container[index] ?: defaultElement
    }

    override fun setElement(
        container: HashMap<Int, Element>,
        localX: Int,
        localY: Int,
        localZ: Int,
        index: Int,
        element: Element
    ): Boolean {
        container[index] = element
        return true
    }

}