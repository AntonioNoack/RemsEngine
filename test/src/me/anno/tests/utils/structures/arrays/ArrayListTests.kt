package me.anno.tests.utils.structures.arrays

import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.structures.arrays.ByteArrayList
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.DoubleArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.arrays.ShortArrayList
import org.junit.jupiter.api.Test

class ArrayListTests {
    @Test
    fun testByteArrayList() {
        val sample = ByteArrayList(16)
        sample.add(1)
        sample.add(2)
        sample.add(3)
        assertContentEquals(byteArrayOf(1, 2, 3), sample.toByteArray())
        sample.add(byteArrayOf(4, 5, 6), 1, 1)
        assertContentEquals(byteArrayOf(1, 2, 3, 5), sample.toByteArray())
        sample[0] = -17
        assertContentEquals(byteArrayOf(-17, 2, 3, 5), sample.toByteArray())
        sample.skip(3)
        assertContentEquals(byteArrayOf(-17, 2, 3, 5, 0, 0, 0), sample.toByteArray())
    }

    @Test
    fun testShortArrayList() {
        val sample = ShortArrayList(16)
        sample.add(1)
        sample.add(2)
        sample.add(3)
        assertContentEquals(shortArrayOf(1, 2, 3), sample.toShortArray())
        sample.add(shortArrayOf(4, 5, 6), 1, 1)
        assertContentEquals(shortArrayOf(1, 2, 3, 5), sample.toShortArray())
        sample[0] = -17
        assertContentEquals(shortArrayOf(-17, 2, 3, 5), sample.toShortArray())
        sample.skip(3)
        assertContentEquals(shortArrayOf(-17, 2, 3, 5, 0, 0, 0), sample.toShortArray())
    }

    @Test
    fun testIntArrayList() {
        val sample = IntArrayList(16)
        sample.add(1)
        sample.add(2)
        sample.add(3)
        assertContentEquals(intArrayOf(1, 2, 3), sample.toIntArray())
        sample.add(intArrayOf(4, 5, 6), 1, 1)
        assertContentEquals(intArrayOf(1, 2, 3, 5), sample.toIntArray())
        sample[0] = -17
        assertContentEquals(intArrayOf(-17, 2, 3, 5), sample.toIntArray())
        sample.skip(3)
        assertContentEquals(intArrayOf(-17, 2, 3, 5, 0, 0, 0), sample.toIntArray())
    }

    @Test
    fun testFloatArrayList() {
        val sample = FloatArrayList(16)
        sample.add(1f)
        sample.add(2f)
        sample.add(3f)
        assertContentEquals(floatArrayOf(1f, 2f, 3f), sample.toFloatArray())
        sample.add(floatArrayOf(4f, 5f, 6f), 1, 1)
        assertContentEquals(floatArrayOf(1f, 2f, 3f, 5f), sample.toFloatArray())
        sample[0] = -17f
        assertContentEquals(floatArrayOf(-17f, 2f, 3f, 5f), sample.toFloatArray())
        sample.skip(3)
        assertContentEquals(floatArrayOf(-17f, 2f, 3f, 5f, 0f, 0f, 0f), sample.toFloatArray())
    }

    @Test
    fun testDoubleArrayList() {
        val sample = DoubleArrayList(16)
        sample.add(1.0)
        sample.add(2.0)
        sample.add(3.0)
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0), sample.toDoubleArray())
        sample.add(doubleArrayOf(4.0, 5.0, 6.0), 1, 1)
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0, 5.0), sample.toDoubleArray())
        sample[0] = -17.0
        assertContentEquals(doubleArrayOf(-17.0, 2.0, 3.0, 5.0), sample.toDoubleArray())
        sample.skip(3)
        assertContentEquals(doubleArrayOf(-17.0, 2.0, 3.0, 5.0, 0.0, 0.0, 0.0), sample.toDoubleArray())
    }
}