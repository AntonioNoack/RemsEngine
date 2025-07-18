package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.LongHashSet

class LongHashSetTest {
    @Test
    fun testRehashing() {
        val set = LongHashSet(4)
        for (i in 0 until 100) set.add(i.toLong())
        assertEquals(100, set.size)
        for (i in 0 until 100) assertTrue(i.toLong() in set)
        assertFalse(-1L in set)
        assertFalse(100L in set)
    }

    @Test
    fun testRemoving() {
        val set = LongHashSet(4)
        for (i in 0 until 100) set.add(i.toLong())
        assertEquals(100, set.size)
        assertTrue(set.maxFill >= 100)
        for (i in 10 until 90) assertTrue(set.remove(i.toLong()))
        assertFalse(set.remove(-1))
        assertFalse(set.remove(100))
        assertEquals(20, set.size)
        assertTrue(set.maxFill < 100)
        for (i in 0 until 100) assertEquals(i < 10 || i >= 90, i.toLong() in set)
        assertFalse(-1L in set)
        assertFalse(100L in set)
    }

    @Test
    fun testClear() {
        val set = LongHashSet(4)
        for (i in 0 until 100) set.add(i.toLong())
        assertEquals(100, set.size)
        assertFalse(set.isEmpty())

        set.clear()
        assertTrue(set.isEmpty())
        for (i in 0 until 100) assertTrue(i.toLong() !in set)
    }
}