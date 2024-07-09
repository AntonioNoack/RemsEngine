package me.anno.tests.structures

import me.anno.utils.structures.heap.MinHeap
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.Lists.smallestKElements
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeapTest {

    @Test
    fun testSmallestKElements() {
        val arr = arrayListOf(1, 3, 5, 4, 6, 13, 10, 9, 8, 15, 17)
        assertEquals(arr.smallestKElements(5, Int::compareTo), listOf(1, 3, 4, 5, 6))
    }

    @Test
    fun testHeapSorting() {

        val minHeap = MinHeap<Int>(3, Int::compareTo)
        for (v in listOf(5, 3, 17, 10, 84, 19, 6, 22, 9)) {
            minHeap.add(v)
        }

        val sorted = createArrayList(minHeap.size) {
            assertFalse(minHeap.isEmpty())
            minHeap.extract()
        }
        assertTrue(minHeap.isEmpty())
        assertEquals(sorted.toList(), listOf(3, 5, 6, 9, 10, 17, 19, 22, 84))
    }

    @Test
    fun testHeapIndexOf() {

        val heap = MinHeap<Int>(3, Int::compareTo)
        for (v in listOf(5, 3, 17, 10, 84, 19, 6, 22, 9)) {
            heap.add(v)
        }

        println(heap.indices.map { heap[it] })
        for (i in heap.indices) {
            assertEquals(i, heap.indexOf(heap[i]))
        }
    }
}