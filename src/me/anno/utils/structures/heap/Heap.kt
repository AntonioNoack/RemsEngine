package me.anno.utils.structures.heap

import org.apache.logging.log4j.LogManager

object Heap {

    private val LOGGER = LogManager.getLogger(Heap::class)

    // https://www.geeksforgeeks.org/building-heap-from-array/
    // To heapify a subtree rooted with node i which is
    // an index in arr[].Nn is size of heap
    fun <V> maxHeapify(arr: ArrayList<V>, i: Int, comparator: Comparator<V>) {
        var largest = i // Initialize largest as root
        val l = 2 * i + 1 // left = 2*i + 1
        val r = 2 * i + 2 // right = 2*i + 2
        val n = arr.size
        // If left child is larger than root
        if (l < n && comparator.compare(arr[l], arr[largest]) > 0) largest = l

        // If right child is larger than largest so far
        if (r < n && comparator.compare(arr[r], arr[largest]) > 0) largest = r

        // If largest is not root
        if (largest != i) {
            val swap = arr[i]
            arr[i] = arr[largest]
            arr[largest] = swap

            // Recursively heapify the affected sub-tree
            maxHeapify(arr, largest, comparator)
        }
    }

    // To heapify a subtree rooted with node i which is
    // an index in arr[].Nn is size of heap
    fun <V> minHeapify(arr: ArrayList<V>, i: Int, comparator: Comparator<V>) {
        var largest = i // Initialize largest as root
        val l = 2 * i + 1 // left = 2*i + 1
        val r = 2 * i + 2 // right = 2*i + 2
        val n = arr.size
        // If left child is larger than root
        if (l < n && comparator.compare(arr[l], arr[largest]) < 0) largest = l

        // If right child is larger than largest so far
        if (r < n && comparator.compare(arr[r], arr[largest]) < 0) largest = r

        // If largest is not root
        if (largest != i) {
            val swap = arr[i]
            arr[i] = arr[largest]
            arr[largest] = swap

            // Recursively heapify the affected sub-tree
            minHeapify(arr, largest, comparator)
        }
    }

    fun <V> extractMax(arr: ArrayList<V>, comparator: Comparator<V>): V {
        val value = arr[0]
        if (arr.size > 1) {
            arr[0] = arr.removeAt(arr.size - 1)
            maxHeapify(arr, 0, comparator)
        } else {
            arr.clear()
        }
        return value
    }

    fun <V> extractMin(arr: ArrayList<V>, comparator: Comparator<V>): V {
        val value = arr[0]
        if (arr.size > 1) {
            arr[0] = arr.removeAt(arr.size - 1)
            minHeapify(arr, 0, comparator)
        } else {
            arr.clear()
        }
        return value
    }

    // Function to build a Max-Heap from the Array
    fun <V> buildMaxHeap(arr: ArrayList<V>, comparator: Comparator<V>) {
        // Index of last non-leaf node
        val startIdx = arr.size / 2 - 1

        // Perform reverse level order traversal
        // from last non-leaf node and heapify
        // each node
        for (i in startIdx downTo 0) {
            maxHeapify(arr, i, comparator)
        }
    }

    // Function to build a Min-Heap from the Array
    fun <V> buildMinHeap(arr: ArrayList<V>, comparator: Comparator<V>) {
        // Index of last non-leaf node
        val startIdx = arr.size / 2 - 1

        // Perform reverse level order traversal
        // from last non-leaf node and heapify
        // each node
        for (i in startIdx downTo 0) {
            minHeapify(arr, i, comparator)
        }
    }

    // A utility function to print the array
    // representation of Heap
    fun <V> printHeap(arr: List<V>?) {
        LOGGER.info("Array representation of Heap is:")
        LOGGER.info(arr)
    }

    fun <V> printSortedMax(arr: ArrayList<V>, comparator: Comparator<V>) {
        LOGGER.info("Sorted elements are:")
        var i = 0
        val l = arr.size
        while (i < l) {
            val value = extractMax(arr, comparator)
            print(value.toString() + " ")
            i++
        }
        LOGGER.info()
    }

    fun <V> printSortedMin(arr: ArrayList<V>, comparator: Comparator<V>) {
        LOGGER.info("Sorted elements are:")
        var i = 0
        val l = arr.size
        while (i < l) {
            val value = extractMin(arr, comparator)
            print(value.toString() + " ")
            i++
        }
        LOGGER.info()
    }

}