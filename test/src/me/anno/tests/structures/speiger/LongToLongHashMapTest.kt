package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.LongToLongHashMap

class LongToLongHashMapTest {

    @Test
    fun testInsert() {
        val map = LongToLongHashMap(-1L)
        assertEquals(-1L, map[0])
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        for (i in 0 until 1000L) {
            assertEquals(i * i + 5, map[i])
        }
    }

    @Test
    fun testRemove() {
        val map = LongToLongHashMap(-1L)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)

        for (i in 100 until 900L) {
            assertEquals(i * i + 5, map.remove(i))
        }

        assertEquals(200, map.size)
        assertTrue(map.maxFill < 1000)

        for (i in 0 until 1000L) {
            assertEquals(if (i in 100 until 900) -1L else i * i + 5, map[i])
        }
    }

    @Test
    fun testRemoveWithValue() {
        val map = LongToLongHashMap(-1L)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertFalse(map.remove(-1L, -1L))
        for (i in 0 until 1000L) {
            assertFalse(map.remove(i, i * i + 6))
            assertTrue(map.remove(i, i * i + 5))
        }
    }

    @Test
    fun testClear() {
        val map = LongToLongHashMap(-1L)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)
        map.clear()
        for (i in 0 until 1000L) {
            assertEquals(-1L, map[i])
        }
    }

    @Test
    fun testReplace() {
        val map = LongToLongHashMap(-1L)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertEquals(-1L, map.replace(-1, 100))
        assertEquals(1000, map.size)
        for (i in 0 until 500L) {
            assertEquals(i * i + 5, map.replace(i, i * 2))
        }
        for (i in 500 until 1000L) {
            assertFalse(map.replace(i, i * i + 6, 0))
            assertTrue(map.replace(i, i * i + 5, i * 2))
        }
    }

    @Test
    fun testForEach() {
        val map = LongToLongHashMap(-1L)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        val done = IntArray(1000)
        map.forEach { key, value ->
            done[key.toInt()]++
            assertEquals(key * key + 5, value)
        }
        assertTrue(done.all { it == 1 })
    }

    @Test
    fun testKeySet() {
        val map = LongToLongHashMap(-1L)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        val keys = map.keysToHashSet()
        assertFalse(-1L in keys)
        assertFalse(1000L in keys)
        for (i in 0 until 1000L) {
            assertTrue(i in keys)
        }
    }

    @Test
    fun testGetOrPut() {
        val map = LongToLongHashMap(-1L)
        val timesCalculated = IntArray(999)
        fun fib(i: Int): Long {
            return if (i < 2) i.toLong()
            else map.getOrPut(i.toLong()) {
                timesCalculated[i - 2]++
                fib(i - 1) + fib(i - 2)
            }
        }
        fib(1000)
        assertTrue(timesCalculated.all { it == 1 })
    }
}