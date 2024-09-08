package me.anno.tests.structures

import me.anno.maths.Maths
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNull
import me.anno.utils.structures.lists.WeightedList
import org.junit.jupiter.api.Test

class WeightedListTest {

    // todo test that elements of weight 0.0 aren't chosen

    @Test
    fun testGetOrNullEmpty() {
        val list = WeightedList<Int>()
        assertNull(list.getOrNull(0.0))
        list.add(5, 1.0)
        assertEquals(list.getOrNull(1.0), 5)
    }

    @Test
    fun testGetOrNull() {
        val list = WeightedList<Int>()
        val c = 11
        for (i in 0 until c) {
            list.add(i, 1.0)
        }

        val baseline = (0 until c).toList()

        // test start
        for ((idx, v) in baseline.withIndex()) {
            val t = (idx + 1e-15) / baseline.size
            assertEquals(v, list.getOrNull(t))
        }

        // test middle
        for ((idx, v) in baseline.withIndex()) {
            val t = (idx + 0.5) / baseline.size
            assertEquals(v, list.getOrNull(t))
        }

        // test end
        for ((idx, v) in baseline.withIndex()) {
            val t = (idx + 0.999) / baseline.size
            assertEquals(v, list.getOrNull(t))
        }
    }

    @Test
    fun testGetInterpolated() {
        val list = WeightedList<Double>()
        val c = 11
        for (i in 0 until c) {
            list.add(i.toDouble(), 1.0)
        }

        val baseline = (0 until c).toList()

        // test start
        for (idx in 0 until c) {
            val t = (idx + 1e-15)
            assertEquals(t, list.getInterpolated(t / baseline.size, Maths::mix)!!, 1e-15)
        }

        // test middle
        for (idx in 0 until c) {
            val t = (idx + 0.5)
            assertEquals(t, list.getInterpolated(t / baseline.size, Maths::mix)!!, 1e-15)
        }

        // test end
        for (idx in 0 until c) {
            val t = (idx + 0.999)
            assertEquals(t, list.getInterpolated(t / baseline.size, Maths::mix)!!, 1e-15)
        }
    }
}