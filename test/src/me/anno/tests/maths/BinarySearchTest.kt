package me.anno.tests.maths

import me.anno.utils.search.BinarySearch.binarySearch
import me.anno.utils.structures.lists.Lists.createArrayList
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BinarySearchTest {

    @Test
    fun testExistingElements() {
        val list = createArrayList(64) { it }
        for (i in list.indices) {
            assertEquals(i, binarySearch(0, list.size) { list[it].compareTo(i) })
        }
    }

    @Test
    fun testMissingElements() {
        val list = arrayListOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f)
        for (v in listOf(-10f, 0.5f, 3.4f, 11f)) {
            val insertIndex = binarySearch(list.size) { list[it].compareTo(v) }
            assertTrue(insertIndex < 0)
            list.add(-1 - insertIndex, v)
        }
        assertEquals(list.sorted(), list)
    }

    @Test
    fun testDifferentSearchAreas() {
        val list = createArrayList(64) { it }
        for (i in list.indices) {
            assertEquals(i, binarySearch(i / 2, i + 1) { list[it].compareTo(i) })
        }
    }
}