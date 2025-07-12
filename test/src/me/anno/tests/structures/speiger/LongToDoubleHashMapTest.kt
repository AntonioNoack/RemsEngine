package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.LongToDoubleHashMap

class LongToDoubleHashMapTest {

    @Test
    fun testInsert() {
        val map = LongToDoubleHashMap(-1.0)
        assertEquals(-1.0, map[0])
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
        }
        assertEquals(1000, map.size)
        assertFalse(map.containsKey(-1))
        for (i in 0 until 1000L) {
            assertTrue(map.containsKey(i))
            assertEquals(i * i + 5.0, map[i])
        }
    }

    @Test
    fun testRemove() {
        val map = LongToDoubleHashMap(-1.0)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)

        for (i in 100 until 900L) {
            assertEquals(i * i + 5.0, map.remove(i))
        }

        assertEquals(200, map.size)
        assertTrue(map.maxFill < 1000)

        for (i in 0 until 1000L) {
            assertEquals(if (i in 100 until 900) -1.0 else i * i + 5.0, map[i])
        }
    }

    @Test
    fun testClear() {
        val map = LongToDoubleHashMap(-1.0)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)
        map.clear()
        for (i in 0 until 1000L) {
            assertEquals(-1.0, map[i])
        }
    }
    @Test
    fun testReplace() {
        val map = LongToDoubleHashMap(-1.0)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
        }
        assertEquals(-1.0, map.replace(-1, 100.0))
        assertEquals(1000, map.size)
        for (i in 0 until 500L) {
            assertEquals(i * i + 5.0, map.replace(i, i * 2.0))
        }
        for (i in 500 until 1000L) {
            assertFalse(map.replace(i, i * i + 6.0, 0.0))
            assertTrue(map.replace(i, i * i + 5.0, i * 2.0))
        }
    }

    @Test
    fun testForEach() {
        val map = LongToDoubleHashMap(-1.0)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
        }
        val done = IntArray(1000)
        map.forEach { key, value ->
            done[key.toInt()]++
            assertEquals(key * key + 5.0, value)
        }
        assertTrue(done.all { it == 1 })
    }

    @Test
    fun testKeySet() {
        val map = LongToDoubleHashMap(-1.0)
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
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
        val map = LongToDoubleHashMap(-1.0)
        val timesCalculated = IntArray(999)
        fun fib(i: Int): Double {
            return if (i < 2) i.toDouble()
            else map.getOrPut(i.toLong()) {
                timesCalculated[i - 2]++
                fib(i - 1) + fib(i - 2)
            }
        }
        fib(1000)
        assertTrue(timesCalculated.all { it == 1 })
    }
}