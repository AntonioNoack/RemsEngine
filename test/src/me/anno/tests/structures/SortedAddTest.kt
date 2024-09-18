package me.anno.tests.structures

import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.sortedAdd
import org.junit.jupiter.api.Test

class SortedAddTest {

    @Test
    fun testSortedAdd() {
        val numbers = listOf(0, 17, 4, 2, 5, 7, 1)
        val mutable = ArrayList<Int>()
        // insert unique numbers
        for (i in numbers.indices) {
            mutable.sortedAdd(numbers[i], false)
            assertEquals(numbers.subList(0, i + 1).sorted(), mutable)
        }
        // try to add duplicates
        for (i in numbers.indices) {
            mutable.sortedAdd(numbers[i], false)
            assertEquals(numbers.sorted(), mutable)
        }
        // add duplicates successfully
        for (i in numbers.indices) {
            mutable.sortedAdd(numbers[i], true)
            assertEquals((numbers.subList(0, i + 1) + numbers).sorted(), mutable)
        }
    }
}