package me.anno.utils.structures.lists

import java.util.function.Predicate
import kotlin.math.min

/**
 * list with a special status of full, where it is assumed that all elements are part of the list;
 * */
class LimitedList<V>(limit: Int = 16) : MutableCollection<V> {

    private val data = arrayOfNulls<Any>(limit)

    var isFull = limit == 0
        private set

    override var size = 0
        private set

    override fun isEmpty(): Boolean = size <= 0

    override fun clear() {
        size = 0
        isFull = false
        // for GC
        data.fill(null)
    }

    override fun add(element: V): Boolean {
        if (element in this) return false
        if (size < data.size) data[size++] = element
        else isFull = true
        return true
    }

    // implemented to reduce allocations
    @Suppress("unchecked_cast")
    fun sumOf(run: (V?) -> Int): Int {
        if (isFull) throw IllegalStateException("Cannot calculate sum if is full")
        var accumulator = 0
        for (i in 0 until min(size, data.size)) {
            accumulator += run(data[i] as V)
        }
        return accumulator
    }

    override fun remove(element: V): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove specific element when list is full")
        var size = size
        for (index in 0 until size) {
            if (element == data[index]) {
                size--
                data[index] = data[size]
                data[size] = null
                this.size = size
                return true
            }
        }
        return false
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove finite set from filled LimitedList")
        var writeIndex = 0
        val oldSize = min(size, data.size)
        for (readIndex in 0 until oldSize) {
            val element = data[readIndex]
            if (element !in elements) {
                data[writeIndex++] = element
            }
        }
        size = writeIndex
        return writeIndex != oldSize
    }

    override fun removeIf(p0: Predicate<in V>): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove finite set from filled LimitedList")
        var writeIndex = 0
        val oldSize = min(size, data.size)
        for (readIndex in 0 until oldSize) {
            val element = data[readIndex]
            @Suppress("unchecked_cast")
            if (!p0.test(element as V)) {
                data[writeIndex++] = element
            }
        }
        size = writeIndex
        return writeIndex != oldSize
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        if (isFull) throw IllegalStateException("Cannot remove finite set from filled LimitedList")
        var writeIndex = 0
        val oldSize = min(size, data.size)
        for (readIndex in 0 until oldSize) {
            val element = data[readIndex]
            if (element in elements) {
                data[writeIndex++] = element
            }
        }
        size = writeIndex
        return writeIndex != oldSize
    }

    override fun addAll(elements: Collection<V>): Boolean {
        if (isFull) return true
        var wasChanged = false
        for (e in elements) {
            if (e !in data) {
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
        for (i in 0 until min(size, data.size)) {
            if (data[i] == element) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return if (isFull) true
        else elements.all { it in this }
    }

    override fun iterator(): MutableIterator<V> {
        if (isFull) throw IllegalStateException("Cannot iterate over all elements")
        return object : Iterator<V>, MutableIterator<V> {
            private var i = 0
            private val size1 = min(size, data.size)
            @Suppress("unchecked_cast")
            override fun next() = data[i++] as V
            override fun hasNext() = i < size1
            override fun remove() {
                this@LimitedList.remove(data[i - 1])
            }
        }
    }

    override fun toString(): String {
        if (isFull) return "*"
        return Array(size) { data[it] }.joinToString()
    }
}