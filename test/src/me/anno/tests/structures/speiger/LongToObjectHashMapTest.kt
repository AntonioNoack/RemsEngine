package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.LongToObjectHashMap
import speiger.primitivecollections.ObjectToLongHashMap

class LongToObjectHashMapTest {

    @Test
    fun testInsert() {
        val map = LongToObjectHashMap<Long>()
        assertEquals(null, map[0])
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertFalse(map.containsKey(-1))
        for (i in 0 until 1000L) {
            assertTrue(map.containsKey(i))
            assertEquals(i * i + 5, map[i])
        }
    }

    @Test
    fun testRemove() {
        val map = LongToObjectHashMap<Long>()
        assertEquals(null, map[0])
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
            assertEquals(if (i in 100 until 900) null else i * i + 5, map[i])
        }
    }

    @Test
    fun testClear() {
        val map = LongToObjectHashMap<Long>()
        assertEquals(null, map[0])
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)
        map.clear()
        for (i in 0 until 1000L) {
            assertEquals(null, map[i])
        }
    }
    @Test
    fun testReplace() {
        val map = LongToObjectHashMap<Long>()
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
        }
        assertEquals(null, map.replace(-1, 100))
        assertEquals(1000, map.size)
        for (i in 0 until 500L) {
            assertEquals(i * i + 5, map.replace(i, i * 2))
        }
        for (i in 500 until 1000L) {
            assertFalse(map.replace(i, i * i + 6L, 0L))
            assertTrue(map.replace(i, i * i + 5L, i * 2L))
        }
    }

    @Test
    fun testForEach() {
        val map = LongToObjectHashMap<Long>()
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
        val map = LongToObjectHashMap<Long>()
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
    fun testForEachKey() {
        val map = LongToObjectHashMap<Long>()
        for (i in 0 until 1000L) {
            map[i] = i * i + 5
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
        val map = LongToObjectHashMap<Long>()
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