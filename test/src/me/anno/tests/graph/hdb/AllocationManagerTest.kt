package me.anno.tests.graph.hdb

import me.anno.graph.hdb.allocator.AllocationManager
import me.anno.graph.hdb.allocator.ReplaceType
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.isSorted
import me.anno.utils.types.Ranges.size
import org.junit.jupiter.api.Test

class AllocationManagerTest {

    data class TestData(var position: Int, val length: Int)

    data class CopyOperation(
        val from: Int, val fromData: Int,
        val to: IntRange, val toData: Int,
    )

    class TestManager : AllocationManager<TestData, Int, Int> {

        override val instances: ArrayList<TestData> = ArrayList()
        override val holes: ArrayList<IntRange> = ArrayList()
        override var storage: Int? = null
        override var storageSize: Int = 0

        val copyOperations = ArrayList<CopyOperation>()
        val allocationSizes = ArrayList<Int>()

        override fun setRange(instance: TestData, value: IntRange) {
            if (value.size > 0) {
                assertEquals(value.size, instance.length)
                instance.position = value.first
            }
        }

        override fun getRange(instance: TestData): IntRange {
            return instance.position until instance.position + instance.length
        }

        var allocationIndex = 0
        override fun allocate(newSize: Int): Int {
            allocationSizes.add(newSize)
            return allocationIndex++
        }

        override fun deallocate(data: Int) {
        }

        override fun moveData(from: Int, fromData: Int, to: IntRange, toData: Int) {
            copyOperations.add(CopyOperation(from, fromData, to, toData))
        }

        override fun insertData(from: Int, fromData: Int, to: IntRange, toData: Int) {
            moveData(from, fromData, to, toData)
        }

        override fun roundUpStorage(requiredSize: Int): Int {
            return 2 * requiredSize
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
        val (manager, storage, availableSpace0) = data

        var availableSpace = availableSpace0
        val originalPosition = i * 17 + 11
        val key = TestData(originalPosition, 100)
        val newData = -1 - i
        val oldStorage = manager.storage
        manager.addData(key, newData)
        val newStorage = manager.storage!!

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
            val expectedCopies = if (i > 0) {
                val saveDataCopy = CopyOperation(
                    0, oldStorage!!,
                    0 until oldRequiredSpace,
                    newStorage
                )
                listOf(saveDataCopy)
            } else emptyList()

            // assertEquals(expectedCopies, manager.copyOperations)
            manager.copyOperations.clear()

            availableSpace = manager.allocationSizes.removeLastOrNull() ?: -1
            assertTrue(availableSpace >= newRequiredSpace, "$availableSpace >= $newRequiredSpace")
        }

        assertTrue(manager.allocationSizes.isEmpty())
        assertTrue(manager.copyOperations.isEmpty())

        assertEquals(
            listOf(newRequiredSpace until manager.storageSize).filter { !it.isEmpty() },
            manager.holes, "Hole mismatch"
        )
        return Triple(manager, newStorage, availableSpace)
    }

    fun testRemoval(manager: TestManager, i: Int, n: Int) {
        val toRemove = manager.instances[i]
        assertTrue(manager.removeData(toRemove))
        assertTrue(manager.allocationSizes.isEmpty())
        assertTrue(manager.copyOperations.isEmpty())
        assertTrue(toRemove !in manager.instances) {
            "$toRemove shall not appear in ${manager.instances}, $i/$n"
        }
        /* assertEquals(
             listOf(0 until i * 100, (i + 1) * 100 until n * 100)
                 .filter { !it.isEmpty() }, manager.sortedRanges
         )*/
    }

    @Test
    fun testReplaceFitting() {
        val (manager, storage0, availableSpace0) = testInsertion(5)
        testRemoval(manager, 2, 5)
        // insert replacement value
        val newData = TestData(55, 100)
        val type = manager.addData(newData, -17)?.type
        val storage1 = manager.storage!!
        // check insertion
        val insertCopy = CopyOperation(
            55, -17,
            200 until 300,
            storage1
        )

        assertEquals(listOf(insertCopy), manager.copyOperations)
        manager.copyOperations.clear()

        assertTrue(manager.allocationSizes.isEmpty())
        assertEquals(ReplaceType.InsertInto, type)
        assertEquals(storage0, storage1) { "Expected instance to fit" }
        assertTrue(manager.instances.map { it.position }.isSorted())
        // assertEquals(listOf(0 until 500), manager.sortedRanges)
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
        val type = manager.addData(newData, -17)?.type
        val storage1 = manager.storage!!
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
        assertTrue(manager.instances.map { it.position }.isSorted())
        // assertEquals(listOf(0 until 200, 300 until 550), manager.sortedRanges)
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
        manager.addData(newData, -17)
        val storage1 = manager.storage!!
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
        // assertEquals(ReplaceType.Append, type)
        assertNotEquals(storage0, storage1)
        assertTrue(manager.instances.map { it.position }.isSorted())
        // assertEquals(listOf(0 until 550), manager.sortedRanges)
    }
}