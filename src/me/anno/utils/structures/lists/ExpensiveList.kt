package me.anno.utils.structures.lists

import me.anno.utils.search.BinarySearch

/**
 * create a list, where evaluations are cached, because they are expensive
 *
 * this is used to find the cursor location in text, where characters have different widths;
 * without access to the char-by-char computation
 * */
class ExpensiveList<V>(override val size: Int, val generator: (Int) -> V) : List<V> {

    // supposedly, only a small fraction of items will be generated, because they are expensive
    // -> use a hash map instead of a full array
    private val cache = HashMap<Int, V>()

    override fun isEmpty() = size == 0

    override fun get(index: Int): V {
        return cache.getOrPut(index) {
            generator(index)
        }
    }

    // it's expensive; no time for that; will implement it on request/need
    override fun iterator() = throw NotImplementedError()
    override fun listIterator(index: Int) = throw NotImplementedError()
    override fun indexOf(element: V) = throw NotImplementedError()
    override fun lastIndexOf(element: V) = throw NotImplementedError()
    override fun listIterator() = throw NotImplementedError()
    override fun contains(element: V) = throw NotImplementedError()
    override fun containsAll(elements: Collection<V>) = throw NotImplementedError()
    override fun subList(fromIndex: Int, toIndex: Int) = throw NotImplementedError()

    @Suppress("unused")
    fun findInsertIndex(comparator: (V) -> Int): Int {
        val index = binarySearch(comparator)
        return if (index < 0) -1 - index else index
    }

    fun binarySearch(comparator: (V) -> Int): Int {
        return BinarySearch.binarySearch(size) { comparator(this[it]) }
    }
}