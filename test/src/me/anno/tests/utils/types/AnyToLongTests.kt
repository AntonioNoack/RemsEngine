package me.anno.tests.utils.types

import me.anno.utils.types.AnyToLong
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnyToLongTests {
    @Test
    fun testNumberToLong() {
        assertEquals(1L, AnyToLong.getLong(1f, 0))
        assertEquals(1L, AnyToLong.getLong(1.0, 0))
        assertEquals(17L, AnyToLong.getLong(17, 0))
        assertEquals(17L, AnyToLong.getLong(17.toByte(), 0))
        assertEquals(17L, AnyToLong.getLong(17.toShort(), 0))
    }

    @Test
    fun testStringToLong() {
        assertEquals(7L, AnyToLong.getLong("", 7))
        assertEquals(1L, AnyToLong.getLong("1", 0))
        assertEquals(-17L, AnyToLong.getLong("-17", 0))
        assertEquals(Long.MIN_VALUE, AnyToLong.getLong("${Long.MIN_VALUE}", 0))
        assertEquals(Long.MAX_VALUE, AnyToLong.getLong("${Long.MAX_VALUE}", 0))
        assertEquals(0x123456789L, AnyToLong.getLong("0x123456789", 0))
        assertEquals(0xff117722.toInt().toLong(), AnyToLong.getLong("#172", 0))
    }
}