package me.anno.utils.structures.heap

import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.swap

/**
 * helper for heaps: heaps are data structures that make inserting and extracting the min/max fast
 * */
object Heap {

    fun parent(pos: Int): Int {
        return (pos - 1) / 2
    }

    fun leftChild(pos: Int): Int {
        return 2 * pos + 1
    }

    fun rightChild(pos: Int): Int {
        return 2 * pos + 2
    }

    // from https://www.geeksforgeeks.org/building-heap-from-array/
    fun <V> maxHeapify(arr: ArrayList<V>, i: Int, comparator: Comparator<V>) {
        return maxHeapify(arr, i, comparator, 1)
    }

    fun <V> maxHeapify(arr: ArrayList<V>, i0: Int, comparator: Comparator<V>, sign: Int) {
        var i = i0
        while (true) {
            var largest = i // Initialize largest as root
            val l = leftChild(i)
            val r = rightChild(i)
            val n = arr.size

            // If left child is larger than root
            if (l < n && comparator.compare(arr[l], arr[largest]) * sign > 0) largest = l

            // If right child is larger than largest so far
            if (r < n && comparator.compare(arr[r], arr[largest]) * sign > 0) largest = r

            // If largest is not root
            if (largest != i) {
                arr.swap(i, largest)
                // Iteratively heapify the affected subtree
                i = largest
            } else break
        }
    }

    fun <V> minHeapify(arr: ArrayList<V>, i: Int, comparator: Comparator<V>) {
        return maxHeapify(arr, i, comparator, -1)
    }

    fun <V> extractMax(arr: ArrayList<V>, comparator: Comparator<V>): V {
        return extractMax(arr, 0, comparator)
    }

    fun <V> extractMax(arr: ArrayList<V>, i: Int, comparator: Comparator<V>): V {
        return extract(arr, i, false, comparator)
    }

    fun <V> extractMin(arr: ArrayList<V>, comparator: Comparator<V>): V {
        return extractMin(arr, 0, comparator)
    }

    fun <V> extractMin(arr: ArrayList<V>, i: Int, comparator: Comparator<V>): V {
        return extract(arr, i, true, comparator)
    }

    fun <V> extract(arr: ArrayList<V>, i: Int, isMinHeap: Boolean, comparator: Comparator<V>): V {
        assertTrue(i in arr.indices, "Heap is empty")
        return if (i + 1 < arr.size) {
            val popped = arr[i]
            arr[i] = arr.removeLast()
            if (isMinHeap) minHeapify(arr, i, comparator)
            else maxHeapify(arr, i, comparator)
            popped
        } else arr.removeLast()
    }

    // Function to build a Max-Heap from the Array
    fun <V> buildMaxHeap(arr: ArrayList<V>, comparator: Comparator<V>) {
        // Index of last non-leaf node
        val startIdx = arr.size.shr(1) - 1
        // Perform reverse level order traversal from last non-leaf node and heapify each node
        for (i in startIdx downTo 0) {
            maxHeapify(arr, i, comparator)
        }
    }

    // Function to build a Min-Heap from the Array
    fun <V> buildMinHeap(arr: ArrayList<V>, comparator: Comparator<V>) {
        // Index of last non-leaf node
        val startIdx = arr.size.shr(1) - 1
        // Perform reverse level order traversal from last non-leaf node and heapify each node
        for (i in startIdx downTo 0) {
            minHeapify(arr, i, comparator)
        }
    }
}