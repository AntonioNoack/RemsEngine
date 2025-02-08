package me.anno.tests.structures

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.sets.FastIteratorSet
import org.junit.jupiter.api.Test
import kotlin.random.Random

class FastIteratorSetTest {
    @Test
    fun testAddAndRemove() {
        val set = FastIteratorSet<Int>()
        val random = Random(1234)
        assertTrue(set.isEmpty())
        for (i in (0 until 10).shuffled(random)) {
            assertTrue(set.add(i))
        }
        assertFalse(set.isEmpty())
        for (i in (0 until 10).shuffled(random)) {
            assertFalse(set.add(i))
        }
        for (i in (0 until 10).shuffled(random)) {
            assertTrue(set.remove(i))
        }
        for (i in (0 until 10).shuffled(random)) {
            assertFalse(set.remove(i))
        }
        assertTrue(set.isEmpty())
    }

    @Test
    fun testToggleContains() {
        val set = FastIteratorSet<Int>()
        set.add(1)
        set.add(2)
        set.add(3)
        assertEquals(listOf(1, 2, 3), set.asList().sorted())
        set.toggleContains(2)
        assertEquals(listOf(1, 3), set.asList().sorted())
        set.toggleContains(1)
        assertEquals(listOf(3), set.asList().sorted())
        set.toggleContains(7)
        assertEquals(listOf(3, 7), set.asList().sorted())
    }

    @Test
    fun testSetContains() {
        val set = FastIteratorSet<Int>()
        set.add(1)
        set.add(2)
        set.add(3)
        assertEquals(listOf(1, 2, 3), set.asList().sorted())
        for (i in 0 until 3) {
            set.setContains(2, false)
            assertEquals(listOf(1, 3), set.asList().sorted())
        }
        for (i in 0 until 3) {
            set.setContains(7, true)
            assertEquals(listOf(1, 3, 7), set.asList().sorted())
        }
    }
}