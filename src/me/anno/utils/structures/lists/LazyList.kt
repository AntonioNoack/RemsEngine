package me.anno.utils.structures.lists

import me.anno.utils.search.BinarySearch
import speiger.primitivecollections.IntToObjectHashMap

/**
 * create a list, where evaluations are cached, because they are expensive
 *
 * this is used to find the cursor location in text, where characters have different widths;
 * without access to the char-by-char computation
 * */
class LazyList<V>(override val size: Int, val generator: (Int) -> V) : SimpleList<V>() {

    // supposedly, only a small fraction of items will be generated, because they are expensive
    // -> use a hash map instead of a full array
    private val cache = IntToObjectHashMap<V>()

    override fun get(index: Int): V {
        return cache.getOrPut(index) {
            generator(index)
        }
    }

    // it's expensive; no time for that; will implement it on request/need
    fun binarySearch(comparator: (V) -> Int): Int {
        return BinarySearch.binarySearch(size) { comparator(this[it]) }
    }
}