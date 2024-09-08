package me.anno.utils.structures.lists

import me.anno.utils.assertions.assertFalse
import java.util.function.Predicate

/**
 * list with a special status of full, where it is assumed that all elements are part of the list;
 * */
class LimitedList<V>(val limit: Int = 16) : MutableCollection<V> {

    private val values = ArrayList<V>(limit + 1)

    val isFull get() = values.size >= limit

    override val size get() = values.size
    override fun isEmpty(): Boolean = !isFull && values.isEmpty()

    override fun clear() {
        values.clear()
    }

    override fun add(element: V): Boolean {
        if (element in this) return false
        return forceAdd(element)
    }

    fun forceAdd(element: V): Boolean {
        values.add(element)
        return true
    }

    operator fun get(i: Int): V {
        return values[i]
    }

    operator fun set(i: Int, v: V) {
        values[i] = v
    }

    fun sumOf(getPartialSum: (V?) -> Int): Int {
        assertFalse(isFull, "Cannot calculate sum if is full")
        return values.sumOf(getPartialSum)
    }

    override fun remove(element: V): Boolean {
        assertFalse(isFull, "Cannot remove specific element when list is full")
        return values.remove(element)
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        assertFalse(isFull, "Cannot remove finite set from filled LimitedList")
        return values.removeAll(elements.toSet())
    }

    override fun removeIf(predicate: Predicate<in V>): Boolean {
        assertFalse(isFull, "Cannot remove finite set from filled LimitedList")
        return values.removeIf(predicate)
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        assertFalse(isFull, "Cannot remove finite set from filled LimitedList")
        return values.retainAll(elements.toSet())
    }

    override fun addAll(elements: Collection<V>): Boolean {
        if (isFull) return true
        var wasChanged = false
        for (e in elements) {
            if (e !in values) {
                if (!add(e)) { // we're full, so we're done
                    return true
                } else {
                    wasChanged = true
                }
            }
        }
        return wasChanged
    }

    override operator fun contains(element: V): Boolean {
        return isFull || values.contains(element)
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return isFull || elements.all { it in values }
    }

    override fun iterator(): MutableIterator<V> {
        assertFalse(isFull, "Cannot iterate over all elements")
        return values.iterator()
    }

    override fun toString(): String {
        return if (isFull) "*" else values.toString()
    }
}