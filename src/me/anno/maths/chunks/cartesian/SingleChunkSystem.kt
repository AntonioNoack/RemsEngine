package me.anno.maths.chunks.cartesian

/**
 * single chunk system, where each chunk is an element
 * */
abstract class SingleChunkSystem<Element: Any> :
    ChunkSystem<Element, Element>(0, 0, 0) {

    override fun setElement(
        container: Element,
        localX: Int,
        localY: Int,
        localZ: Int,
        index: Int,
        element: Element
    ): Boolean = throw NotImplementedError()

    override fun getElement(container: Element, localX: Int, localY: Int, localZ: Int, index: Int): Element {
        return container
    }
}