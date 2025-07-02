package me.anno.tests.utils.search

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertTrue
import me.anno.utils.search.BinarySearch.binarySearch
import org.junit.jupiter.api.Test

class BinarySearchTests {

    @Test
    fun testBinarySearchExactMatch() {
        val array = listOf(1, 3, 5, 7, 9)
        val index = binarySearch(0, array.lastIndex) { i -> array[i].compareTo(5) }
        assertEquals(2, index)
    }

    @Test
    fun testBinarySearchFirstElement() {
        val array = listOf(2, 4, 6, 8)
        val index = binarySearch(0, array.lastIndex) { i -> array[i].compareTo(2) }
        assertEquals(0, index)
    }

    @Test
    fun testBinarySearchLastElement() {
        val array = listOf(1, 2, 3, 4, 5)
        val index = binarySearch(0, array.lastIndex) { i -> array[i].compareTo(5) }
        assertEquals(4, index)
    }

    @Test
    fun testBinarySearchNotFoundBefore() {
        val array = listOf(10, 20, 30)
        val index = binarySearch(0, array.lastIndex) { i -> array[i].compareTo(5) }
        assertEquals(-1, index) // insertion point 0 → -1
    }

    @Test
    fun testBinarySearchNotFoundAfter() {
        val array = listOf(10, 20, 30)
        val index = binarySearch(0, array.lastIndex) { i -> array[i].compareTo(40) }
        assertEquals(-4, index) // insertion point 3 → -1 - 3 = -4
    }

    @Test
    fun testBinarySearchNotFoundMiddle() {
        val array = listOf(10, 20, 30)
        val index = binarySearch(0, array.lastIndex) { i -> array[i].compareTo(25) }
        assertEquals(-3, index) // insert between 20 and 30 → insert at index 2
    }

    @Test
    fun testBinarySearchEmptyArray() {
        val index = binarySearch(0, -1) { _ -> 0 }
        assertEquals(-1, index) // insert index = 0 → -1
    }

    @Test
    fun testBinarySearchSingleElementFound() {
        val array = listOf(42)
        val index = binarySearch(0, 1) { i -> array[i].compareTo(42) }
        assertEquals(0, index)
    }

    @Test
    fun testBinarySearchSingleElementNotFound() {
        val array = listOf(42)
        val index = binarySearch(0, array.lastIndex) { i -> array[i].compareTo(41) }
        assertEquals(-1, index) // insert at 0
    }

    @Test
    fun testBinarySearchDescending() {
        val array = listOf(9, 7, 5, 3, 1)
        val index = binarySearch(0, array.lastIndex) { i -> -array[i].compareTo(5) } // reverse compare
        assertEquals(2, index)
    }

    private val subListTestArray = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50)

    @Test
    fun testSublistMatchInMiddle() {
        val index = binarySearch(3, 6) { i -> subListTestArray[i].compareTo(30) }
        assertEquals(5, index)
    }

    @Test
    fun testSublistMatchAtStartOfSublist() {
        val index = binarySearch(2, 5) { i -> subListTestArray[i].compareTo(15) }
        assertEquals(2, index)
    }

    @Test
    fun testSublistMatchAtEndOfSublist() {
        val index = binarySearch(4, 7) { i -> subListTestArray[i].compareTo(40) }
        assertEquals(7, index)
    }

    @Test
    fun testSublistNoMatchBeforeSublist() {
        val index = binarySearch(4, 7) { i -> subListTestArray[i].compareTo(10) }
        // 10 would be inserted at index 4
        assertEquals(-5, index)
    }

    @Test
    fun testSublistNoMatchAfterSublist() {
        val index = binarySearch(1, 4) { i ->
            assertTrue(i in 1..4)
            subListTestArray[i].compareTo(35)
        }
        // 35 would be inserted at index 5, which is out of bounds
        assertEquals(-1 - 5, index)
    }

    @Test
    fun testSublistEmptyRange() {
        val index = binarySearch(5, 4) { _ -> assertFail() } // invalid (start > end)
        assertEquals(-6, index) // insertion point is 5
    }

    @Test
    fun testSublistSingleElementFound() {
        val index = binarySearch(6, 6) { i ->
            assertEquals(6, i)
            subListTestArray[i].compareTo(35)
        }
        assertEquals(6, index)
    }

    @Test
    fun testSublistSingleElementNotFound() {
        val index = binarySearch(6, 6) { i ->
            assertEquals(6, i)
            subListTestArray[i].compareTo(36)
        }
        assertEquals(-1 - 7, index) // would be inserted at 7
    }

    @Test
    fun testSublistOutOfBoundsShouldNotCrash() {
        val index = binarySearch(8, 9) { i ->
            assertTrue(i == 8 || i == 9)
            subListTestArray[i].compareTo(100)
        }
        assertEquals(-1 - 10, index) // would be inserted at 10
    }
}