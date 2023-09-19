package me.anno.tests.structures

import me.anno.utils.structures.lists.Lists.sortedByTopology
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

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
        assertEquals(elements.sortedByTopology { dependencies[it] }, listOf(0, 1, 2, 3, 4))
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
        assertFails { elements.sortedByTopology { dependencies[it] } }
    }
}