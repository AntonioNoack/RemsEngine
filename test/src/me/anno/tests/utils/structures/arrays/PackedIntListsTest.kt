package me.anno.tests.utils.structures.arrays

import me.anno.utils.structures.arrays.PackedIntLists
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PackedIntListsTest {

    private fun toList(p: PackedIntLists, index: Int): List<Int> {
        val out = ArrayList<Int>(p.getSize(index))
        p.forEach(index) { value -> out.add(value) }
        return out
    }

    @Test
    fun testAddAndGet() {
        val p = PackedIntLists(4, 4, -1)

        p.add(0, 10)
        p.add(0, 20)
        p.add(0, 30)

        assertEquals(3, p.getSize(0))
        assertEquals(10, p[0, 0])
        assertEquals(20, p[0, 1])
        assertEquals(30, p[0, 2])
    }

    @Test
    fun testMultipleIndicesIndependent() {
        val p = PackedIntLists(4, 4, -1)

        p.add(0, 1)
        p.add(1, 2)
        p.add(2, 3)

        assertEquals(listOf(1), toList(p, 0))
        assertEquals(listOf(2), toList(p, 1))
        assertEquals(listOf(3), toList(p, 2))
    }

    @Test
    fun testAddUnique() {
        val p = PackedIntLists(2, 2, -1)

        p.addUnique(0, 5)
        p.addUnique(0, 5)
        p.addUnique(0, 6)

        assertEquals(2, p.getSize(0))
        assertEquals(listOf(5, 6), toList(p, 0))
    }

    @Test
    fun testAddUniqueRejectsNegative() {
        val p = PackedIntLists(2, 2, -1)

        p.addUnique(0, -1)
        p.addUnique(0, -5)

        assertEquals(0, p.getSize(0))
    }

    @Test
    fun testContains() {
        val p = PackedIntLists(2, 4, -1)

        p.add(0, 10)
        p.add(0, 20)

        assertTrue(p.contains(0, 10))
        assertTrue(p.contains(0, 20))
        assertFalse(p.contains(0, 30))
    }

    @Test
    fun testForEachIteration() {
        val p = PackedIntLists(2, 4, -1)

        p.add(0, 1)
        p.add(0, 2)
        p.add(0, 3)

        val result = mutableListOf<Int>()
        val count = p.forEach(0) { result.add(it) }

        assertEquals(3, count)
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun testClearIndex() {
        val p = PackedIntLists(3, 4, -1)

        p.add(1, 10)
        p.add(1, 20)

        p.clear(1)

        assertEquals(0, p.getSize(1))
        assertFalse(p.contains(1, 10))
    }

    @Test
    fun testClearAll() {
        val p = PackedIntLists(3, 4, -1)

        p.add(0, 1)
        p.add(1, 2)
        p.add(2, 3)

        p.clear()

        assertEquals(0, p.getSize(0))
        assertEquals(0, p.getSize(1))
        assertEquals(0, p.getSize(2))
    }

    @Test
    fun testSortAscending() {
        val p = PackedIntLists(2, 4, -1)

        p.add(0, 5)
        p.add(0, 1)
        p.add(0, 3)

        p.sortBy(0, Int::compareTo)

        assertEquals(listOf(1, 3, 5), toList(p, 0))
    }

    @Test
    fun testSortDescending() {
        val p = PackedIntLists(2, 4, -1)

        p.add(0, 2)
        p.add(0, 9)
        p.add(0, 4)

        p.sortBy(0) { a, b -> b - a }

        assertEquals(listOf(9, 4, 2), toList(p, 0))
    }

    @Test
    fun testResizeGrow() {
        val p = PackedIntLists(2, 4, -1)

        p.add(0, 1)
        p.add(1, 2)

        p.resizeTo(4)

        assertEquals(listOf(1), toList(p, 0))
        assertEquals(listOf(2), toList(p, 1))

        p.add(3, 9)
        assertEquals(listOf(9), toList(p, 3))
    }

    @Test
    fun testResizeShrink() {
        val p = PackedIntLists(4, 4, -1)

        p.add(0, 1)
        p.add(1, 2)
        p.add(2, 3)
        p.add(3, 4)

        p.resizeTo(2)

        assertEquals(listOf(1), toList(p, 0))
        assertEquals(listOf(2), toList(p, 1))
    }

    @Test
    fun testGetSizeAfterAdds() {
        val p = PackedIntLists(2, 2, -1)

        repeat(10) { value ->
            p.add(0, value)
        }

        assertEquals(10, p.getSize(0))
    }

    @Test
    fun testDuplicatesAllowedWithAdd() {
        val p = PackedIntLists(2, 2, -1)

        p.add(0, 7)
        p.add(0, 7)

        assertEquals(2, p.getSize(0))
        assertEquals(listOf(7, 7), toList(p, 0))
    }

    @Test
    fun testForEachOnEmpty() {
        val p = PackedIntLists(2, 2, -1)

        val result = mutableListOf<Int>()
        val count = p.forEach(0) { result.add(it) }

        assertEquals(0, count)
        assertTrue(result.isEmpty())
    }

}