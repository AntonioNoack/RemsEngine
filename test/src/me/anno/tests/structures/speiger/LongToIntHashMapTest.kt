package me.anno.tests.structures.speiger

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import speiger.primitivecollections.LongToIntHashMap
import kotlin.ranges.contains

class LongToIntHashMapTest {

    private fun createInstance() = LongToIntHashMap(-1)

    @Test
    fun testInsert() {
        val map = createInstance()
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
        val map = createInstance()
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
    fun testRemoveIf() {
        val map = createInstance()
        for (i in 0 until 1000) {
            map[i.toLong()] = i * i + 5
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

        for (i in 0 until 1000) {
            assertEquals(if (i in 100 until 900) -1 else i * i + 5, map[i.toLong()])
        }
    }

    @Test
    fun testClone() {
        val base = createInstance()
        for (i in 0 until 1000) {
            base[i.toLong()] = i * i + 5
        }
        val clone = base.clone()
        assertEquals(1000, clone.size)
        assertFalse(clone.containsKey(-1))
        for (i in 0 until 1000) {
            assertTrue(clone.containsKey(i.toLong()))
            assertEquals(i * i + 5, clone[i.toLong()])
        }
    }

    @Test
    fun testClear() {
        val map = createInstance()
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
    fun testForEach() {
        val map = createInstance()
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
        val map = createInstance()
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
        val map = createInstance()
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
        val map = createInstance()
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