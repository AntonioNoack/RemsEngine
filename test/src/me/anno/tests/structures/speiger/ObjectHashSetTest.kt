package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.ObjectHashSet

class ObjectHashSetTest {

    private fun createInstance() = ObjectHashSet<Long>(4)

    @Test
    fun testRehashing() {
        val set = createInstance()
        for (i in 0 until 100L) assertTrue(set.add(i))
        for (i in 0 until 100L) assertFalse(set.add(i))
        assertEquals(100, set.size)
        for (i in 0 until 100) assertTrue(i.toLong() in set)
        assertFalse(-1L in set)
        assertFalse(100L in set)
    }

    @Test
    fun testRemove() {
        val set = createInstance()
        for (i in 0 until 100) set.add(i.toLong())
        assertEquals(100, set.size)
        assertTrue(set.maxFill >= 100)
        for (i in 10 until 90) assertTrue(set.remove(i.toLong()))
        assertFalse(set.remove(-1))
        assertFalse(set.remove(100))
        assertEquals(20, set.size)
        assertTrue(set.maxFill < 100)
        for (i in 0 until 100) assertEquals(i !in 10 until 90, i.toLong() in set)
        assertFalse(-1L in set)
        assertFalse(100L in set)
    }

    @Test
    fun testRemoveIf() {
        val set = createInstance()
        for (i in 0 until 1000) set.add(i.toLong())
        assertEquals(1000, set.size)
        assertTrue(set.maxFill >= 1000)

        assertEquals(800, set.removeIf { key ->
            key in 100 until 900
        })
        assertEquals(0, set.removeIf { key ->
            key in 100 until 900
        })

        assertEquals(200, set.size)
        assertTrue(set.maxFill < 1000)

        for (i in 0 until 1000L) {
            assertEquals(i !in 100 until 900, set.containsKey(i))
        }
    }

    @Test
    fun testClone() {
        val set = createInstance()
        for (i in 0 until 100L) set.add(i)
        val clone = set.clone()
        assertEquals(100, clone.size)
        for (i in 0 until 100L) assertTrue(i in clone)
        assertFalse(-1 in clone)
        assertFalse(100 in clone)
    }

    @Test
    fun testClear() {
        val set = createInstance()
        for (i in 0 until 100) set.add(i.toLong())
        assertEquals(100, set.size)
        assertFalse(set.isEmpty())

        set.clear()
        assertTrue(set.isEmpty())
        for (i in 0 until 100) assertTrue(i.toLong() !in set)
    }

    @Test
    fun testForEachKey() {
        val map = createInstance()
        for (i in 0 until 1000L) map.add(i)
        val keys = HashSet<Long>()
        map.forEachKey { key ->
            assertTrue(keys.add(key))
        }
        assertFalse(-1L in keys)
        assertFalse(1000L in keys)
        for (i in 0 until 1000L) {
            assertTrue(i in keys)
        }
    }
}