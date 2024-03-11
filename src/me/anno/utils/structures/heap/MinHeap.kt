package me.anno.utils.structures.heap

import me.anno.utils.structures.lists.Lists.swap

/**
 * base implementation from
 * https://www.geeksforgeeks.org/min-heap-in-java/
 *
 * made generic, and removed the useless index 0
 * */
open class MinHeap<Value>(
    initialSize: Int,
    private val comparator: Comparator<Value>
) : Collection<Value> {

    // Member variables of this class
    private val values = ArrayList<Value>(initialSize)
    override val size get() = values.size
    override fun isEmpty() = values.isEmpty()

    fun add(element: Value) {
        var index = size
        values.add(element)
        // while less than parent, swap this with parent
        var parentIndex = Heap.parent(index)
        while (comparator.compare(values[index], values[parentIndex]) < 0) {
            values.swap(index, parentIndex)
            index = parentIndex
            parentIndex = Heap.parent(index)
        }
    }

    /**
     * remove the minimum value from the heap
     * */
    fun extract(): Value = removeAt(0)

    fun removeAt(i: Int): Value {
        if (i !in indices) {
            throw IndexOutOfBoundsException("Heap is empty")
        }
        return if (i + 1 < size) {
            val popped = values[i]
            values[i] = values.removeLast()
            Heap.minHeapify(values, i, comparator)
            popped
        } else values.removeLast()
    }

    fun peak(): Value? {
        return values[0]
    }

    fun clear() {
        // clear the memory for GC
        values.clear()
    }

    fun remove(element: Value): Boolean {
        val i = indexOf(element)
        if (i >= 0) {
            removeAt(i)
        }
        return i >= 0
    }

    override fun contains(element: Value): Boolean {
        return indexOf(element) >= 0
    }

    fun indexOf(element: Value): Int {
        // cannot really be optimized
        return values.indexOf(element)
    }

    override fun containsAll(elements: Collection<Value>): Boolean {
        return elements.all { contains(it) }
    }

    override fun iterator(): Iterator<Value> {
        return values.iterator()
    }

    operator fun get(i: Int): Value {
        return values[i]
    }
}
