package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.LongToIntHashMap
import speiger.primitivecollections.ObjectToLongHashMap

class LongToIntHashMapTest {

    @Test
    fun testInsert() {
        val map = LongToIntHashMap(-1)
        assertEquals(-1, map[0])
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertFalse(map.containsKey(-1))
        for (i in 0 until 1000) {
            assertTrue(map.containsKey(i.toLong()))
            assertEquals(i * i + 5, map[i.toLong()])
        }
    }

    @Test
    fun testRemove() {
        val map = LongToIntHashMap(-1)
        assertEquals(-1, map[0])
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)

        for (i in 100 until 900) {
            assertEquals(i * i + 5, map.remove(i.toLong()))
        }

        assertEquals(200, map.size)
        assertTrue(map.maxFill < 1000)

        for (i in 0 until 1000) {
            assertEquals(if (i in 100 until 900) -1 else i * i + 5, map[i.toLong()])
        }
    }

    @Test
    fun testClear() {
        val map = LongToIntHashMap(-1)
        assertEquals(-1, map[0])
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)
        map.clear()
        for (i in 0 until 1000) {
            assertEquals(-1, map[i.toLong()])
        }
    }

    @Test
    fun testReplace() {
        val map = LongToIntHashMap(-1)
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
        }
        assertEquals(-1, map.replace(-1, 100))
        assertEquals(1000, map.size)
        for (i in 0 until 500) {
            assertEquals(i * i + 5, map.replace(i.toLong(), i * 2))
        }
        for (i in 500 until 1000) {
            assertFalse(map.replace(i.toLong(), i * i + 6, 0))
            assertTrue(map.replace(i.toLong(), i * i + 5, i * 2))
        }
    }

    @Test
    fun testForEach() {
        val map = LongToIntHashMap(-1)
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
        }
        val done = IntArray(1000)
        map.forEach { key, value ->
            done[key.toInt()]++
            assertEquals(key.toInt() * key.toInt() + 5, value)
        }
        assertTrue(done.all { it == 1 })
    }

    @Test
    fun testKeySet() {
        val map = LongToIntHashMap(-1)
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
        }
        val keys = map.keysToHashSet()
        assertFalse(-1L in keys)
        assertFalse(1000L in keys)
        for (i in 0 until 1000L) {
            assertTrue(i in keys)
        }
    }

    @Test
    fun testForEachKey() {
        val map = LongToIntHashMap(-1)
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
        }
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

    @Test
    fun testGetOrPut() {
        val map = LongToIntHashMap(-1)
        val timesCalculated = IntArray(999)
        fun fib(i: Int): Int {
            return if (i < 2) i
            else map.getOrPut(i.toLong()) {
                timesCalculated[i - 2]++
                fib(i - 1) + fib(i - 2)
            }
        }
        fib(1000)
        assertTrue(timesCalculated.all { it == 1 })
    }
}