package me.anno.tests.utils.structures.arrays

import me.anno.utils.structures.arrays.PackedIntLists
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class PackedIntListsTest {

    private fun toList(p: PackedIntLists, index: Int): List<Int> {
        val out = ArrayList<Int>(p.getSize(index))
        p.forEach(index) { value -> out.add(value) }
        return out
    }

    @Test
    fun testAddAndGet() {
        val lists = PackedIntLists(size = 3, initialCapacityPerValue = 2, -1)
        lists.add(0, 10)
        lists.add(0, 20)
        lists.add(1, 99)

        assertEquals(10, lists[0, 0])
        assertEquals(20, lists[0, 1])
        assertEquals(99, lists[1, 0])
        assertEquals(2, lists.getSize(0))
        assertEquals(1, lists.getSize(1))
        assertEquals(0, lists.getSize(2))
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

    @Test
    fun testForEach() {
        val lists = PackedIntLists(size = 1, initialCapacityPerValue = 2, -1)
        lists.add(0, 1)
        lists.add(0, 2)
        lists.add(0, 3)

        val collected = mutableListOf<Int>()
        lists.forEach(0) { collected.add(it) }

        assertEquals(listOf(1, 2, 3), collected)
    }

    @Test
    fun testAppendingWithNecessaryRightShift() {
        val lists = PackedIntLists(size = 2, initialCapacityPerValue = 1, -1)
        // Row 0 and 1 will be packed close together
        // println("[[],[]] ${lists.offsets.toList()}, ${lists.values.toList()}")
        lists.add(0, 11)
        // println("[[11],[]] ${lists.offsets.toList()}, ${lists.values.toList()}")
        lists.add(0, 12) // should trigger shift for row 1’s sentinel
        // println("[[11,12],[]] ${lists.offsets.toList()}, ${lists.values.toList()}")
        lists.add(1, 99)
        // println("[[11,12],[99]] ${lists.offsets.toList()}, ${lists.values.toList()}")

        assertEquals(listOf(11, 12), toList(lists, 0))
        assertEquals(listOf(99), toList(lists, 1))
    }

    @Test
    fun testGrowingWhenOutOfCapacity() {
        val lists = PackedIntLists(size = 1, initialCapacityPerValue = 1, -1)
        // Fill far beyond initial size
        for (i in 0 until 50) {
            lists.add(0, i)
        }
        assertEquals(50, lists.getSize(0))
        assertEquals(42, lists[0, 42])
    }

    @Test
    fun testThrowsOutOfBounds() {
        val lists = PackedIntLists(size = 1, initialCapacityPerValue = 1, -1)
        lists.add(0, 7)
        assertThrows<IndexOutOfBoundsException> {
            lists[0, 1]
        }
    }

    @Test
    fun testRandomizedAddsForConsistency() {
        val rowCount = 20
        val ops = 20_000
        val avgRowSize = 10

        val packed = PackedIntLists(rowCount, avgRowSize, -1)
        val reference = Array(rowCount) { ArrayList<Int>() }

        val rng = Random(12345)

        repeat(ops) { step ->
            val index = rng.nextInt(rowCount)
            val value = step // unique per step so we can track

            packed.add(index, value)
            reference[index].add(value)

            // println("[#$step, [$index]+=$value], state: ${packed.offsets.toList()}, ${packed.values.toList()}")
        }

        // Verify row-by-row consistency
        for (index in 0 until rowCount) {
            val expected = reference[index]
            val actual = toList(packed, index).sorted()
            assertEquals(expected, actual, "Mismatch in index=$index")
        }
    }

}