package me.anno.tests.maths

import me.anno.maths.Packing.pack16
import me.anno.maths.Packing.pack32
import me.anno.maths.Packing.pack64
import me.anno.maths.Packing.unpackHighFrom16
import me.anno.maths.Packing.unpackHighFrom32
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom16
import me.anno.maths.Packing.unpackLowFrom32
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class PackingTests {
    @Test
    fun testPack16() {
        assertEquals(0xabcd, pack16(0xab, 0xcd))
        assertEquals(0xabcd, pack16(0x1ab, 0x1cd))
        assertEquals(0xcd, unpackLowFrom16(0xabcd,false))
        assertEquals(0xcd.toByte().toInt(), unpackLowFrom16(0xabcd,true))
        assertEquals(0xab, unpackHighFrom16(0xabcd,false))
        assertEquals(0xab.toByte().toInt(), unpackHighFrom16(0xabcd,true))
    }

    @Test
    fun testPack32() {
        assertEquals(0xabcda123.toInt(), pack32(0xabcd, 0xa123))
        assertEquals(0xabcda123.toInt(), pack32(0x1abcd, 0x1a123))
        assertEquals(0xa123, unpackLowFrom32(0xabcda123.toInt(),false))
        assertEquals(0xa123.toShort().toInt(), unpackLowFrom32(0xabcda123.toInt(),true))
        assertEquals(0xabcd, unpackHighFrom32(0xabcda123.toInt(),false))
        assertEquals(0xabcd.toShort().toInt(), unpackHighFrom32(0xabcda123.toInt(),true))
    }

    @Test
    fun testPack64() {
        assertEquals(0x7bcd123478510123, pack64(0x7bcd1234, 0x78510123))
        assertEquals(0x78510123, unpackLowFrom64(0x7bcd123478510123))
        assertEquals(0x7bcd1234, unpackHighFrom64(0x7bcd123478510123))
    }
}