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
        for (n in listOf(0, 1, 2, 5, 10, 100, 500)) {
            val sorted = (0 until n).toList()
            val shuffled = if (n < 5) sorted.reversed() else sorted.shuffled(random)
            if (n > 1) assertNotEquals(sorted, shuffled)
            println("shuffled: $shuffled")
            val sorted2 = shuffled.sorted2()
            println("sorted: $sorted2")
            assertEquals(sorted, sorted2)
        }
    }
}