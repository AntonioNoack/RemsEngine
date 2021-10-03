package me.anno.utils.structures.heap

import org.apache.logging.log4j.LogManager
import kotlin.math.max

/**
 * base implementation from
 * https://www.geeksforgeeks.org/min-heap-in-java/
 *
 * made generic, and removed the useless index 0
 * */
class MinHeap<Value>(
    initialSize: Int,
    private val comparator: Comparator<Value>
) : Iterable<Value>, Iterator<Value> {

    // Member variables of this class
    private var values = Array<Any?>(initialSize) { null }
    private var size = 0

    fun isEmpty() = size <= 0
    fun isNotEmpty() = size > 0

    private fun parent(pos: Int): Int {
        return (pos - 1) / 2
    }

    private fun leftChild(pos: Int): Int {
        return 2 * pos + 1
    }

    private fun rightChild(pos: Int): Int {
        return 2 * pos + 2
    }

    private fun isLeaf(pos: Int): Boolean {
        return pos >= size / 2 && pos < size
    }

    private fun swapNodes(a: Int, b: Int) {
        val tmp = values[a]
        values[a] = values[b]
        values[b] = tmp
    }

    private fun minHeapify(pos: Int) {
        // If the node is a non-leaf node and greater
        // than any of its child
        if (!isLeaf(pos)) {
            val x = values[pos] as Value
            val l = values[leftChild(pos)] as Value
            val r = values[rightChild(pos)] as Value
            if (comparator.compare(x, l) > 0 || comparator.compare(x, r) > 0) {
                // Swap with the left child and heapify
                // the left child
                if (comparator.compare(l, r) < 0) {
                    swapNodes(pos, leftChild(pos))
                    minHeapify(leftChild(pos))
                } else {
                    swapNodes(pos, rightChild(pos))
                    minHeapify(rightChild(pos))
                }
            }
        }
    }

    fun add(element: Value) {
        if (size >= values.size) {
            // resize
            val newValues = arrayOfNulls<Any>(max(values.size * 2, 8))
            System.arraycopy(values, 0, newValues, 0, size)
            values = newValues
        }
        var current = size++
        values[current] = element
        // while less than parent, swap this with parent
        while (comparator.compare(values[current] as Value, values[parent(current)] as Value) < 0) {
            swapNodes(current, parent(current))
            current = parent(current)
        }
    }

    fun print() {
        for (i in 0 until size / 2) {
            // Printing the parent and both children
            LOGGER.info(
                "Node: " + values[i] +
                        ", left: " + values.getOrNull(leftChild(i)) +
                        ", right: " + values.getOrNull(rightChild(i))
            )
        }
    }

    /**
     * remove the minimum value from the heap
     * */
    fun remove(): Value {
        return removeAt(0)
    }

    fun removeAt(i: Int): Value {
        if (i !in 0 until size) throw IndexOutOfBoundsException("Heap is empty")
        val popped = values[i]
        values[i] = values[--size]
        if (i < size) minHeapify(i)
        return popped as Value
    }

    fun peak(): Value? {
        return values[0] as? Value
    }

    fun clear() {
        // clear the memory for GC
        val values = values
        for (i in 0 until size) {
            values[i] = null
        }
        size = 0
    }

    /**
     * unsafe & slow method...
     * */
    fun remove(v: Value) {
        val values = values
        for (i in 0 until size) {
            val vi = values[i]
            if (vi != null && v == vi) {
                removeAt(i)
                return
            }
        }
    }

    override fun hasNext(): Boolean = size > 0
    override fun next(): Value = remove()

    /**
     * empties the heap!
     * */
    override fun iterator(): Iterator<Value> {
        return this
    }

    companion object {

        private val LOGGER = LogManager.getLogger(MinHeap::class)

        @JvmStatic
        fun main(arg: Array<String>) {

            val minHeap = MinHeap<Int>(3, Int::compareTo)

            minHeap.add(5)
            minHeap.add(3)
            minHeap.add(17)
            minHeap.add(10)
            minHeap.add(84)
            minHeap.add(19)
            minHeap.add(6)
            minHeap.add(22)
            minHeap.add(9)

            minHeap.print()

            while (minHeap.isNotEmpty()) {
                LOGGER.info("Extracted " + minHeap.remove())
            }

        }
    }
}
