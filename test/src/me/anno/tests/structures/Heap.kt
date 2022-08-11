package me.anno.tests.structures

import me.anno.utils.structures.heap.Heap
import me.anno.utils.structures.lists.Lists.smallestKElements

fun main() {
    // Binary Tree Representation
    // of input array
    //              1
    //           /     \
    //         3         5
    //      /    \     /  \
    //     4      6   13  10
    //    / \    / \
    //   9   8  15 17
    val arr = arrayListOf(1, 3, 5, 4, 6, 13, 10, 9, 8, 15, 17)
    val comparator = Comparator { a: Int, b: Int -> a.compareTo(b) }
    println(arr.smallestKElements(10, comparator))
    Heap.buildMaxHeap(arr, comparator)
    Heap.printHeap(arr)
    Heap.printSortedMax(arr, comparator)
}