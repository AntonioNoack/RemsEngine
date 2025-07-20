package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.ObjectToIntHashMap

class ObjectToIntHashMapTest {

    @Test
    fun testInsert() {
        val map = ObjectToIntHashMap<Int>(-1)
        assertEquals(-1, map[0])
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        for (i in 0 until 1000) {
            assertEquals(i * i + 5, map[i])
        }
    }

    @Test
    fun testRemove() {
        val map = ObjectToIntHashMap<Int>(-1)
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)

        for (i in 100 until 900) {
            assertEquals(i * i + 5, map.remove(i))
        }

        assertEquals(200, map.size)
        assertTrue(map.maxFill < 1000)

        for (i in 0 until 1000) {
            assertEquals(if (i in 100 until 900) -1 else i * i + 5, map[i])
        }
    }

    @Test
    fun testRemoveWithValue() {
        val map = ObjectToIntHashMap<Int>(-1)
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        assertFalse(map.remove(-1, -1))
        for (i in 0 until 1000) {
            assertFalse(map.remove(i, i * i + 6))
            assertTrue(map.remove(i, i * i + 5))
        }
    }

    @Test
    fun testClear() {
        val map = ObjectToIntHashMap<Int>(-1)
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)
        map.clear()
        for (i in 0 until 1000) {
            assertEquals(-1, map[i])
        }
    }

    @Test
    fun testReplace() {
        val map = ObjectToIntHashMap<Int>(-1)
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        assertEquals(-1, map.replace(-1, 100))
        assertEquals(1000, map.size)
        for (i in 0 until 500) {
            assertEquals(i * i + 5, map.replace(i, i * 2))
        }
        for (i in 500 until 1000) {
            assertFalse(map.replace(i, i * i + 6, 0))
            assertTrue(map.replace(i, i * i + 5, i * 2))
        }
    }

    @Test
    fun testForEach() {
        val map = ObjectToIntHashMap<Int>(-1)
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        val done = IntArray(1000)
        map.forEach { key, value ->
            done[key]++
            assertEquals(key * key + 5, value)
        }
        assertTrue(done.all { it == 1 })
    }

    @Test
    fun testKeySet() {
        val map = ObjectToIntHashMap<Int>(-1)
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        val keys = map.keysToHashSet()
        assertFalse(-1 in keys)
        assertFalse(1000 in keys)
        for (i in 0 until 1000) {
            assertTrue(i in keys)
        }
    }

    @Test
    fun testForEachKey() {
        val map = ObjectToIntHashMap<Int>(-1)
        for (i in 0 until 1000) {
            map[i] = i * i + 5
        }
        val keys = HashSet<Int>()
        map.forEachKey { key ->
            assertTrue(keys.add(key))
        }
        assertFalse(-1 in keys)
        assertFalse(1000 in keys)
        for (i in 0 until 1000) {
            assertTrue(i in keys)
        }
    }

    @Test
    fun testGetOrPut() {
        val map = ObjectToIntHashMap<Int>(-1)
        val timesCalculated = IntArray(999)
        fun fib(i: Int): Int {
            return if (i < 2) i
            else map.getOrPut(i) {
                timesCalculated[i - 2]++
                fib(i - 1) + fib(i - 2)
            }
        }
        fib(1000)
        assertTrue(timesCalculated.all { it == 1 })
    }
}