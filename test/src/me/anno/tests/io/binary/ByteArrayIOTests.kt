package me.anno.tests.io.binary

import me.anno.io.binary.ByteArrayIO.read
import me.anno.io.binary.ByteArrayIO.readBE16
import me.anno.io.binary.ByteArrayIO.readBE32
import me.anno.io.binary.ByteArrayIO.readBE32F
import me.anno.io.binary.ByteArrayIO.readBE64
import me.anno.io.binary.ByteArrayIO.readBE64F
import me.anno.io.binary.ByteArrayIO.readLE16
import me.anno.io.binary.ByteArrayIO.readLE32
import me.anno.io.binary.ByteArrayIO.readLE32F
import me.anno.io.binary.ByteArrayIO.readLE64
import me.anno.io.binary.ByteArrayIO.readLE64F
import me.anno.io.binary.ByteArrayIO.writeBE16
import me.anno.io.binary.ByteArrayIO.writeBE32
import me.anno.io.binary.ByteArrayIO.writeBE64
import me.anno.io.binary.ByteArrayIO.writeLE16
import me.anno.io.binary.ByteArrayIO.writeLE32
import me.anno.io.binary.ByteArrayIO.writeLE64
import me.anno.maths.Maths.PIf
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.ByteArrayList
import me.anno.utils.types.Floats.toHalf
import org.junit.jupiter.api.Test
import kotlin.math.PI

class ByteArrayIOTests {
    @Test
    fun testReadWriteBE64() {
        val bytes = ByteArray(16)
        val index = 7
        val valueF = PI
        val value = valueF.toRawBits()
        bytes.writeBE64(index, value)
        assertEquals(value.shr(32).toInt(), bytes.readBE32(index, 0))
        assertEquals(value.toInt(), bytes.readBE32(index + 4, 0))
        assertEquals(value, bytes.readBE64(index))

        for (i in 0 until 16) {
            if (i < index || i == 15) assertEquals(0.toByte(), bytes[i])
        }

        val bytes2 = ByteArray(16)
        bytes2.writeBE64(index, valueF)
        assertContentEquals(bytes, bytes2)
        assertEquals(valueF, bytes2.readBE64F(index))
    }

    @Test
    fun testReadWriteLE64() {
        val bytes = ByteArray(16)
        val index = 7
        val valueF = PI
        val value = valueF.toRawBits()
        bytes.writeLE64(index, value)
        assertEquals(value.toInt(), bytes.readLE32(index, 0))
        assertEquals(value.shr(32).toInt(), bytes.readLE32(index + 4, 0))
        assertEquals(value, bytes.readLE64(index))

        for (i in 0 until 16) {
            if (i < index || i == 15) assertEquals(0.toByte(), bytes[i])
        }

        val bytes2 = ByteArray(16)
        bytes2.writeLE64(index, valueF)
        assertContentEquals(bytes, bytes2)
        assertEquals(valueF, bytes2.readLE64F(index))
    }

    @Test
    fun testReadWriteBE32() {
        val bytes = ByteArray(16)
        val index = 7
        val valueF = PIf
        val value = valueF.toRawBits()
        bytes.writeBE32(index, value)
        assertEquals(value.shr(16), bytes.readBE16(index, 0))
        assertEquals(value.and(0xffff), bytes.readBE16(index + 2, 0))
        assertEquals(value, bytes.readBE32(index))

        for (i in 0 until 16) {
            if (i < index || i >= index + 4) assertEquals(0.toByte(), bytes[i])
        }

        val bytes2 = ByteArray(16)
        bytes2.writeBE32(index, valueF)
        assertContentEquals(bytes, bytes2)
        assertEquals(valueF, bytes2.readBE32F(index))
    }

    @Test
    fun testReadWriteLE32() {
        val bytes = ByteArray(16)
        val index = 7
        val valueF = PIf
        val value = valueF.toRawBits()
        bytes.writeLE32(index, value)
        assertEquals(value.shr(16), bytes.readLE16(index + 2, 0))
        assertEquals(value.and(0xffff), bytes.readLE16(index, 0))
        assertEquals(value, bytes.readLE32(index))

        for (i in 0 until 16) {
            if (i < index || i >= index + 4) assertEquals(0.toByte(), bytes[i])
        }

        val bytes2 = ByteArray(16)
        bytes2.writeLE32(index, valueF)
        assertContentEquals(bytes, bytes2)
        assertEquals(valueF, bytes2.readLE32F(index))
    }

    @Test
    fun testReadWriteBE16() {
        val bytes = ByteArray(16)
        val index = 7
        val valueF = PIf.toHalf()
        bytes.writeBE16(index, valueF)
        assertEquals(valueF.shr(8), bytes.read(index, 0))
        assertEquals(valueF.and(0xff), bytes.read(index + 1, 0))
        assertEquals(valueF, bytes.readBE16(index))

        for (i in 0 until 16) {
            if (i < index || i >= index + 2) assertEquals(0.toByte(), bytes[i])
        }

        val bytes2 = ByteArray(16)
        bytes2.writeBE16(index, valueF)
        assertContentEquals(bytes, bytes2)
        assertEquals(valueF, bytes2.readBE16(index))
    }

    @Test
    fun testReadWriteLE16() {
        val bytes = ByteArray(16)
        val index = 7
        val valueF = PIf.toHalf()
        bytes.writeLE16(index, valueF)
        assertEquals(valueF.shr(8), bytes.read(index + 1, 0))
        assertEquals(valueF.and(0xff), bytes.read(index, 0))
        assertEquals(valueF, bytes.readLE16(index))

        for (i in 0 until 16) {
            if (i < index || i >= index + 2) assertEquals(0.toByte(), bytes[i])
        }

        val bytes2 = ByteArray(16)
        bytes2.writeLE16(index, valueF)
        assertContentEquals(bytes, bytes2)
        assertEquals(valueF, bytes2.readLE16(index))
    }

    @Test
    fun testReadWriteByteArrayListBE(){
        val list = ByteArrayList(16)
        list.writeBE64(PI)
        list.writeBE32(PIf)
        list.writeBE16(PIf.toHalf())
        assertEquals(14, list.size)
        assertEquals(PI, list.readBE64F(0))
        assertEquals(PI.toRawBits(), list.readBE64(0))
        assertEquals(PIf, list.readBE32F(8))
        assertEquals(PIf.toRawBits(), list.readBE32(8))
        assertEquals(PIf.toHalf(), list.readBE16(12))
    }

    @Test
    fun testReadWriteByteArrayListLE(){
        val list = ByteArrayList(16)
        list.writeLE64(PI)
        list.writeLE32(PIf)
        list.writeLE16(PIf.toHalf())
        assertEquals(14, list.size)
        assertEquals(PI, list.readLE64F(0))
        assertEquals(PI.toRawBits(), list.readLE64(0))
        assertEquals(PIf, list.readLE32F(8))
        assertEquals(PIf.toRawBits(), list.readLE32(8))
        assertEquals(PIf.toHalf(), list.readLE16(12))
    }
}