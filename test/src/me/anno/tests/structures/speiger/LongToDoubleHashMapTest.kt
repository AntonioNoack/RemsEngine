package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.LongToDoubleHashMap
import kotlin.ranges.contains

class LongToDoubleHashMapTest {

    private fun createInstance() = LongToDoubleHashMap(-1.0)

    @Test
    fun testInsert() {
        val map = createInstance()
        assertEquals(-1.0, map[0])
        for (i in 0 until 1000L) assertEquals(-1.0, map.put(i, i * i + 3.0))
        for (i in 0 until 1000L) assertEquals(i * i + 3.0, map.put(i, i * i + 5.0))
        assertEquals(1000, map.size)
        assertFalse(map.containsKey(-1))
        for (i in 0 until 1000L) {
            assertTrue(map.containsKey(i))
            assertEquals(i * i + 5.0, map[i])
        }
    }

    @Test
    fun testRemove() {
        val map = createInstance()
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
    fun testRemoveIf() {
        val map = createInstance()
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
        }
        assertEquals(1000, map.size)
        assertTrue(map.maxFill >= 1000)

        assertEquals(800, map.removeIf { key, _ ->
            key in 100 until 900
        })
        assertEquals(0, map.removeIf { key, _ ->
            key in 100 until 900
        })

        assertEquals(200, map.size)
        assertTrue(map.maxFill < 1000)

        for (i in 0 until 1000L) {
            assertEquals(if (i in 100 until 900) -1.0 else i * i + 5.0, map[i])
        }
    }

    @Test
    fun testClone() {
        val base = createInstance()
        for (i in 0 until 1000L) {
            base[i] = i * i + 5.0
        }
        val clone = base.clone()
        assertEquals(1000, clone.size)
        assertFalse(clone.containsKey(-1))
        for (i in 0 until 1000L) {
            assertTrue(clone.containsKey(i))
            assertEquals(i * i + 5.0, clone[i])
        }
    }

    @Test
    fun testClear() {
        val map = createInstance()
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
    fun testForEach() {
        val map = createInstance()
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
        val map = createInstance()
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
    fun testForEachKey() {
        val map = createInstance()
        for (i in 0 until 1000L) {
            map[i] = i * i + 5.0
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
        val map = createInstance()
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