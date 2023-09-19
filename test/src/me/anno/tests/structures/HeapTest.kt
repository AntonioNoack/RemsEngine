package me.anno.tests.structures

import me.anno.utils.structures.heap.MinHeap
import me.anno.utils.structures.lists.Lists.smallestKElements
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeapTest {

    @Test
    fun testSmallestKElements() {
        val arr = arrayListOf(1, 3, 5, 4, 6, 13, 10, 9, 8, 15, 17)
        assertEquals(arr.smallestKElements(5, Int::compareTo), listOf(1, 3, 4, 5, 6))
    }

    @Test
    fun testHeapSorting1() {

        val minHeap = MinHeap<Int>(3, Int::compareTo)
        for (v in listOf(5, 3, 17, 10, 84, 19, 6, 22, 9)) {
            minHeap.add(v)
        }

        val sorted = Array(minHeap.size) {
            assertTrue(minHeap.isNotEmpty())
            minHeap.remove()
        }
        assertTrue(minHeap.isEmpty())

        Assertions.assertEquals(sorted.toList(), listOf(3, 5, 6, 9, 10, 17, 19, 22, 84))
    }
}