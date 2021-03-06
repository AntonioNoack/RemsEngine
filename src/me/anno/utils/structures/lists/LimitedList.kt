package me.anno.utils.structures.lists

import me.anno.io.zip.NextEntryIterator
import java.util.function.Predicate
import kotlin.math.min

class LimitedList<V>(limit: Int = 16) : MutableCollection<V> {

    val data = arrayOfNulls<Any>(limit)

    fun isFull() = size >= data.size

    override var size = 0

    override fun isEmpty(): Boolean = size <= 0

    override fun clear() {
        size = 0
        data.fill(null)
    }

    override fun add(element: V): Boolean {
        if (element in this) return false
        if (size < data.size) data[size] = element
        size++
        return size <= data.size
    }

    fun indexOf(element: V): Int {
        return data.indexOf(element)
    }

    // implemented to reduce allocations
    @Suppress("unchecked_cast")
    inline fun sumOf(run: (V?) -> Int): Int {
        var accumulator = 0
        val data = data
        for (i in 0 until size) {
            accumulator += run(data[i] as V)
        }
        return accumulator
    }

    override fun remove(element: V): Boolean {
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
        var writeIndex = 0
        val oldSize = min(size, data.size)
        for (readIndex in 0 until oldSize) {
            val element = data[readIndex]
            if (element !in elements) {
                data[writeIndex++] = element
            }
        }
        // if (!isFull()) {
        size = writeIndex
        // }
        return writeIndex != oldSize || isFull()
    }

    override fun removeIf(p0: Predicate<in V>): Boolean {
        var writeIndex = 0
        val oldSize = min(size, data.size)
        for (readIndex in 0 until oldSize) {
            val element = data[readIndex]
            @Suppress("unchecked_cast")
            if (!p0.test(element as V)) {
                data[writeIndex++] = element
            }
        }
        // if (!isFull()) {
        size = writeIndex
        // }
        return writeIndex != oldSize || isFull()
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        var writeIndex = 0
        val oldSize = min(size, data.size)
        for (readIndex in 0 until oldSize) {
            val element = data[readIndex]
            if (element in elements) {
                data[writeIndex++] = element
            }
        }
        // if (!isFull()) {
        size = writeIndex
        // }
        return writeIndex != oldSize || isFull()
    }

    override fun addAll(elements: Collection<V>): Boolean {
        val targetSize = data.size + elements.size
        var wasChanged = false
        for (e in elements) {
            if (!add(e)) {
                size = targetSize
                return true
            } else {
                wasChanged = true
            }
        }
        return wasChanged
    }

    override operator fun contains(element: V): Boolean {
        if (size > data.size) return true
        for (i in 0 until min(size, data.size)) {
            if (data[i] == element) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return if (size > data.size) true
        else elements.all { it in this }
    }

    override fun iterator(): MutableIterator<V> {
        return object : NextEntryIterator<V>(), MutableIterator<V> {
            private var i = 0
            override fun nextEntry(): V? {
                @Suppress("unchecked_cast")
                return data.getOrNull(i++) as? V
            }

            override fun remove() {
                remove(data[i - 1])
            }
        }
    }

    override fun toString(): String {
        return Array(size) { data[it] }.joinToString()
    }

}