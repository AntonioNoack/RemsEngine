package me.anno.tests.utils.algorithms

import me.anno.utils.algorithms.Sorting.sorted2
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SortingTests {
    @Test
    fun testSorting() {
        val random = Random(1324)
        val sorted = (0 until 100).toList()
        val shuffled = sorted.shuffled(random)
        assertNotEquals(sorted, shuffled)
        assertEquals(sorted, shuffled.sorted2())
    }
}