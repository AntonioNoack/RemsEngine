package me.anno.tests.utils.search

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertTrue
import me.anno.utils.search.Partition
import me.anno.utils.structures.lists.Lists.swap
import org.junit.jupiter.api.Test

class PartitionTests {

    @Test
    fun testPartition() {
        val input = listOf(5, 3, 8, 1, 9, 2)
        val pivots = listOf(-1, 100) + input

        for (pivot in pivots) {
            val workList = ArrayList(input)
            val mid = Partition.partition(0, input.size, {
                workList[it] <= pivot
            }, { a, b ->
                workList.swap(a, b)
            })

            assertTrue(workList.subList(0, mid).all { it <= pivot })
            assertTrue(workList.subList(mid, workList.size).all { it > pivot })
        }
    }

    @Test
    fun testPartitionEmpty() {
        val mid = Partition.partition(99, 99, {
            assertFail()
        }, { a, b ->
            assertFail()
        })
        assertEquals(mid, 99)
    }
}