package me.anno.tests.graph.hdb

import me.anno.graph.hdb.allocator.AllocationManager
import me.anno.graph.hdb.allocator.ReplaceType
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.isSorted
import me.anno.utils.types.size
import org.junit.jupiter.api.Test

class AllocationManagerTest {

    data class TestData(var position: Int, val length: Int)

    data class CopyOperation(
        val from: Int, val fromData: Int,
        val to: IntRange, val toData: Int,
    )

    class TestManager : AllocationManager<TestData, Int> {

        val sortedElements = ArrayList<TestData>()
        val sortedRanges = ArrayList<IntRange>()

        val copyOperations = ArrayList<CopyOperation>()
        val allocationSizes = ArrayList<Int>()
        override fun getRange(key: TestData): IntRange {
            return key.position until key.position + key.length
        }

        var allocationIndex = 0
        override fun allocate(newSize: Int): Int {
            allocationSizes.add(newSize)
            return allocationIndex++
        }

        override fun deallocate(data: Int) {
        }

        override fun copy(from: Int, fromData: Int, to: IntRange, toData: Int) {
            copyOperations.add(CopyOperation(from, fromData, to, toData))
        }

        override fun copy(key: TestData, from: Int, fromData: Int, to: IntRange, toData: Int) {
            copy(from, fromData, to, toData)
            key.position = to.first
            assertEquals(key.length, to.size)
        }

        override fun roundUpStorage(requiredSize: Int): Int {
            return 2 * requiredSize
        }

        override fun allocationKeepsOldData(): Boolean {
            return true
        }
    }

    @Test
    fun testInsertionOnly() {
        testInsertion(5)
    }

    fun testInsertion(n: Int): Triple<TestManager, Int, Int> {
        val manager = TestManager()
        val availableSpace = 32
        val storage = manager.allocate(32)
        assertEquals(availableSpace, manager.allocationSizes.removeLast())
        var data = Triple(manager, storage, availableSpace)
        for (i in 0 until n) {
            data = testInsertion(data, i)
        }
        return data
    }

    fun testInsertion(data: Triple<TestManager, Int, Int>, i: Int): Triple<TestManager, Int, Int> {
        val (manager, storage0, availableSpace0) = data

        var availableSpace = availableSpace0
        var storage = storage0
        val originalPosition = i * 17 + 11
        val key = TestData(originalPosition, 100)
        val newData = -1 - i
        val oldStorage = storage
        val (_, newStorage) = manager.insert(
            manager.sortedElements,
            manager.sortedRanges,
            key, newData,
            manager.getRange(key), availableSpace,
            storage, false
        )
        storage = newStorage

        // we need two copy operations:
        //  - saving the old data
        //  - storing the new data
        val oldRequiredSpace = i * 100
        val newRequiredSpace = (i + 1) * 100

        // check storing the new data
        assertTrue(manager.copyOperations.isNotEmpty())
        val newDataCopy = CopyOperation(
            originalPosition, newData,
            oldRequiredSpace until newRequiredSpace,
            newStorage
        )
        assertEquals(newDataCopy, manager.copyOperations.removeLast())

        if (availableSpace < newRequiredSpace) {
            // check saving the old data
            if (i > 0) {
                val saveDataCopy = CopyOperation(
                    0, oldStorage,
                    0 until oldRequiredSpace,
                    newStorage
                )
                assertEquals(saveDataCopy, manager.copyOperations.removeLast())
            }
            availableSpace = manager.allocationSizes.removeLast()
            assertTrue(availableSpace >= newRequiredSpace, "$availableSpace >= $newRequiredSpace")
        }

        assertTrue(manager.allocationSizes.isEmpty())
        assertTrue(manager.copyOperations.isEmpty())
        assertEquals(listOf(0 until newRequiredSpace), manager.sortedRanges)

        return Triple(manager, storage, availableSpace)
    }

    fun testRemoval(manager: TestManager, i: Int, n: Int) {
        val toRemove = manager.sortedElements[i]
        assertTrue(manager.remove(toRemove, manager.sortedElements, manager.sortedRanges))
        assertTrue(manager.allocationSizes.isEmpty())
        assertTrue(manager.copyOperations.isEmpty())
        assertTrue(toRemove !in manager.sortedElements) {
            "$toRemove shall not appear in ${manager.sortedElements}, $i/$n"
        }
        assertEquals(
            listOf(0 until i * 100, (i + 1) * 100 until n * 100)
                .filter { !it.isEmpty() }, manager.sortedRanges
        )
    }

    @Test
    fun testReplaceFitting() {
        val (manager, storage0, availableSpace0) = testInsertion(5)
        testRemoval(manager, 2, 5)
        // insert replacement value
        val newData = TestData(55, 100)
        val (type, storage1) = manager.insert(
            manager.sortedElements, manager.sortedRanges,
            newData, -17, manager.getRange(newData), availableSpace0,
            storage0, false
        )
        // check insertion
        val insertCopy = CopyOperation(
            55, -17,
            200 until 300,
            storage0
        )
        assertEquals(insertCopy, manager.copyOperations.removeLast())
        assertTrue(manager.copyOperations.isEmpty())
        assertTrue(manager.allocationSizes.isEmpty())
        assertEquals(ReplaceType.InsertInto, type)
        assertEquals(storage0, storage1)
        assertTrue(manager.sortedElements.map { it.position }.isSorted())
        assertEquals(listOf(0 until 500), manager.sortedRanges)
        // check that we can continue inserting normally
        var data = Triple(manager, storage1, availableSpace0)
        for (i in 5 until 10) {
            data = testInsertion(data, i)
        }
    }

    @Test
    fun testRemoval() {
        for (n in 1 until 10) {
            for (i in 0 until n) {
                val (manager, _, _) = testInsertion(n)
                testRemoval(manager, i, n)
            }
        }
    }

    @Test
    fun testReplaceNotFitting() {
        val (manager, storage0, availableSpace0) = testInsertion(4)
        assertTrue(availableSpace0 >= 550, "$availableSpace0 >= 550") // we want it to fit at the end
        testRemoval(manager, 2, 4)
        // insert replacement value
        val newData = TestData(55, 150)
        val (type, storage1) = manager.insert(
            manager.sortedElements, manager.sortedRanges,
            newData, -17, manager.getRange(newData), availableSpace0,
            storage0, false
        )
        // check insertion
        val insertCopy = CopyOperation(
            55, -17,
            400 until 550,
            storage1
        )
        assertEquals(insertCopy, manager.copyOperations.removeLast())
        assertTrue(manager.copyOperations.isEmpty())
        assertTrue(manager.allocationSizes.isEmpty())
        assertEquals(ReplaceType.InsertInto, type)
        assertEquals(storage0, storage1)
        assertTrue(manager.sortedElements.map { it.position }.isSorted())
        assertEquals(listOf(0 until 200, 300 until 550), manager.sortedRanges)
    }

    @Test
    fun testReplaceNotFittingWithCompacting() {
        val (manager, storage0, availableSpace0) = testInsertion(5)
        assertTrue(
            availableSpace0 < 650,
            "$availableSpace0 < 650"
        ) // we want there to be too few space, so we run compacting
        testRemoval(manager, 2, 5)
        // insert replacement value
        val newData = TestData(55, 150)
        val (type, storage1) = manager.insert(
            manager.sortedElements, manager.sortedRanges,
            newData, -17, manager.getRange(newData), availableSpace0,
            storage0, false
        )
        // check insertion
        val insertCopy = CopyOperation(
            55, -17,
            400 until 550,
            storage1
        )
        assertEquals(insertCopy, manager.copyOperations.removeLast())
        val saveDataCopy4 = CopyOperation(
            400, storage0,
            300 until 400, storage1,
        )
        assertEquals(saveDataCopy4, manager.copyOperations.removeLast())
        val saveDataCopy3 = CopyOperation(
            300, storage0,
            200 until 300, storage1,
        )
        assertEquals(saveDataCopy3, manager.copyOperations.removeLast())
        val saveDataCopy1 = CopyOperation(
            100, storage0,
            100 until 200, storage1,
        )
        assertEquals(saveDataCopy1, manager.copyOperations.removeLast())
        val saveDataCopy0 = CopyOperation(
            0, storage0,
            0 until 100, storage1,
        )
        assertEquals(saveDataCopy0, manager.copyOperations.removeLast())
        assertTrue(manager.copyOperations.isEmpty())
        // assertEquals(550 > manager.allocationSizes.removeLast())
        assertEquals(ReplaceType.Append, type)
        assertNotEquals(storage0, storage1)
        assertTrue(manager.sortedElements.map { it.position }.isSorted())
        assertEquals(listOf(0 until 550), manager.sortedRanges)
    }
}