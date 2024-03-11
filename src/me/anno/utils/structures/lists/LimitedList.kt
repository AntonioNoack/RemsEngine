package me.anno.utils.structures.lists

import java.util.function.Predicate
import kotlin.math.min

/**
 * list with a special status of full, where it is assumed that all elements are part of the list;
 * */
class LimitedList<V>(limit: Int = 16) : MutableCollection<V> {

    private val values = ArrayList<V>(limit)

    var isFull = limit == 0
        private set

    override val size get() = values.size
    override fun isEmpty(): Boolean = size <= 0

    override fun clear() {
        isFull = false
        values.clear()
    }

    override fun add(element: V): Boolean {
        if (element in this) return false
        if (size < values.size) values.add(element)
        else isFull = true
        return true
    }

    fun sumOf(getPartialSum: (V?) -> Int): Int {
        if (isFull) throw IllegalStateException("Cannot calculate sum if is full")
        return values.sumOf(getPartialSum)
    }

    override fun remove(element: V): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove specific element when list is full")
        return values.remove(element)
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove finite set from filled LimitedList")
        return values.removeAll(elements.toSet())
    }

    override fun removeIf(predicate: Predicate<in V>): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove finite set from filled LimitedList")
        return values.removeIf(predicate)
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove finite set from filled LimitedList")
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
        if (isFull) return true
        for (i in 0 until min(size, values.size)) {
            if (values[i] == element) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return if (isFull) true
        else elements.all { it in this }
    }

    override fun iterator(): MutableIterator<V> {
        if (isFull) throw IllegalStateException("Cannot iterate over all elements")
        return values.iterator()
    }

    override fun toString(): String {
        if (isFull) return "*"
        return values.toString()
    }
}