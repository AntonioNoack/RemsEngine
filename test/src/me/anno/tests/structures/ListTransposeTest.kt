package me.anno.tests.structures

import me.anno.utils.structures.lists.Lists.transpose
import me.anno.utils.structures.lists.Lists.transposed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListTransposeTest {

    val sample = listOf(listOf(1, 2, 3), listOf(9, 8, 7))
    val transposedSample = listOf(listOf(1, 9), listOf(2, 8), listOf(3, 7))

    @Test
    fun testTransposed() {
        assertEquals(sample.transposed(), transposedSample)
        assertEquals(sample.transposed().transposed(), sample)
    }

    @Test
    fun testTranspose() {
        val mutableSample = arrayListOf(arrayListOf(1, 2, 3), arrayListOf(9, 8, 7))
        assertEquals(mutableSample.transpose(), transposedSample)
        assertEquals(mutableSample.transpose(), sample)
    }
}