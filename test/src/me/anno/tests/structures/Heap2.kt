package me.anno.tests

import me.anno.utils.structures.heap.MinHeap

fun main() {

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

    while (minHeap.isNotEmpty()) {
        println("Extracted " + minHeap.remove())
    }

}