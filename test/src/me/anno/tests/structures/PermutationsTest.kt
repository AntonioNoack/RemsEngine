package me.anno.tests.structures

import me.anno.maths.Maths.factorial
import me.anno.maths.Permutations
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

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