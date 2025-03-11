package me.anno.tests.structures

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.sortedByTopology
import me.anno.utils.structures.lists.TopologicalSort
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

    @Test
    fun findCycleTest() {
        val elements = listOf(0, 1, 2, 3, 4).shuffled()
        val dependencies = hashMapOf(// full sort
            0 to listOf(1, 3),
            1 to listOf(2, 4),
            2 to listOf(0, 3),
            3 to listOf(),
            4 to listOf()
        )
        val list = ArrayList(elements)
        val sort = object : TopologicalSort<Int, MutableList<Int>>(list) {
            override fun visitDependencies(node: Int): Boolean {
                val dependenciesI = dependencies[node] ?: emptyList()
                return dependenciesI.any { visit(it) }
            }
        }
        assertEquals(emptyList<Int>(), sort.findCycle()) // before sorting, findCycle() returns empty list
        assertNull(sort.finish(false))
        val cycle = sort.findCycle()
        assertTrue(
            cycle == listOf(0, 1, 2) ||
                    cycle == listOf(1, 2, 0) ||
                    cycle == listOf(2, 0, 1)
        )
        // algorithm shall be repeatable
        assertEquals(cycle, sort.findCycle())
    }
}