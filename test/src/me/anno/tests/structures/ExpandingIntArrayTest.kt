package me.anno.tests.structures

import me.anno.utils.structures.arrays.ExpandingIntArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExpandingIntArrayTest {
    @Test
    fun test() {
        val list = ExpandingIntArray(16)
        list.add(1)
        list.add(2)
        list.add(3)
        assertEquals(list.toList(), listOf(1, 2, 3))
        list.removeAt(1)
        assertEquals(list.toList(), listOf(1, 3))
        list.add(0, 5)
        assertEquals(list.toList(), listOf(5, 1, 3))
        list.removeBetween(0, 1)
        assertEquals(list.toList(), listOf(1, 3))
    }
}