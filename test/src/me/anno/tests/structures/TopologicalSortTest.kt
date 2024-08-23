package me.anno.tests.structures

import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.sortedByTopology
import org.junit.jupiter.api.Test

class TopologicalSortTest {

    @Test
    fun okTest() {
        val elements = listOf(2, 1, 4, 3, 0)
        val dependencies = hashMapOf(// full sort
            1 to listOf(0),
            2 to listOf(1),
            3 to listOf(2),
            4 to listOf(3)
        )
        assertEquals(listOf(0, 1, 2, 3, 4), elements.sortedByTopology { dependencies[it] })
    }

    @Test
    fun cyclicTest() {
        val elements = listOf(0, 1, 2, 3, 4).shuffled()
        val dependencies = hashMapOf(// full sort
            0 to listOf(4),
            1 to listOf(0),
            2 to listOf(1),
            3 to listOf(2),
            4 to listOf(3)
        )
        assertEquals(null, elements.sortedByTopology { dependencies[it] })
    }
}