package me.anno.tests.utils.search

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.search.BinarySearch
import me.anno.utils.structures.lists.Lists
import org.junit.jupiter.api.Test

class BinarySearchTest {

    @Test
    fun testExistingElements() {
        val list = Array(64) { it }
        for (i in list.indices) {
            assertEquals(i, BinarySearch.binarySearch(0, list.size) { list[it].compareTo(i) })
        }
    }

    @Test
    fun testMissingElements() {
        val list = arrayListOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f)
        for (v in listOf(-10f, 0.5f, 3.4f, 11f)) {
            val insertIndex = BinarySearch.binarySearch(list.size) { list[it].compareTo(v) }
            assertTrue(insertIndex < 0)
            list.add(-1 - insertIndex, v)
        }
        assertEquals(list.sorted(), list)
    }

    @Test
    fun testDifferentSearchAreas() {
        val list = IntArray(64) { it }
        for (i in list.indices) {
            assertEquals(i, BinarySearch.binarySearch(i / 2, i + 1) { list[it].compareTo(i) })
        }
    }
}