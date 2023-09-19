package me.anno.tests.maths

import me.anno.utils.search.BinarySearch.binarySearch
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BinarySearchTest {
    @Test
    fun testBinarySearch() {
        val list = arrayListOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f)
        val found = ArrayList<Float>()
        for (v in listOf(-10f, 0.5f, 4f, 7f, 11f)) {
            val insertIndex = binarySearch(list.size) { list[it].compareTo(v) }
            if (insertIndex >= 0) {
                found.add(v)
            } else {
                list.add(-1 - insertIndex, v)
            }
        }
        Assertions.assertEquals(found, listOf(4f, 7f))
        Assertions.assertEquals(list.sorted(), list)
    }
}