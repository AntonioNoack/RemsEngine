package me.anno.tests.structures

import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.heap.MinHeap
import org.junit.jupiter.api.Assertions

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

    val list = ExpandingIntArray(16)
    while (minHeap.isNotEmpty()) {
        list.add(minHeap.remove())
    }

    Assertions.assertEquals(list.toString(), "[3,5,6,9,10,17,19,22,84]")
}