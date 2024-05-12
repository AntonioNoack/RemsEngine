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
        var ctr = 0
        val base = listOf(1, 2, 3, 4, 5)
        Permutations.generatePermutations(base) {
            ctr++
            assertTrue(all.add(it.toList()))
        }
        assertEquals(base.size.factorial().toInt(), ctr)
    }
}