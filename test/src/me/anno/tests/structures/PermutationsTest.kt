package me.anno.tests.structures

import me.anno.maths.Maths.factorial
import me.anno.maths.Permutations
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object PermutationsTest {
    @Test
    fun testPermutations() {
        val all = HashSet<List<Int>>()
        val base = listOf(1, 2, 3, 4, 5)
        Permutations.generatePermutations(base) {
            assertTrue(all.add(it.toList()))
        }
        assertEquals(base.size.factorial().toInt(), all.size)
    }
}