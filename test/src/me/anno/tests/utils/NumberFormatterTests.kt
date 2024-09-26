package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Floats.f3s
import me.anno.utils.types.NumberFormatter.formatFloat
import me.anno.utils.types.NumberFormatter.formatInt
import me.anno.utils.types.NumberFormatter.formatIntTriplets
import org.junit.jupiter.api.Test

object NumberFormatterTests {

    @Test
    fun testFormatFloat() {
        assertEquals("1.120", 1.12f.f3())
        assertEquals(" 1.120", 1.12f.f3s())
        assertEquals("-1.120", (-1.12f).f3())
        assertEquals("-1.120", (-1.12f).f3s())
        assertEquals("0.020", 0.02f.f3())
        val tmp = StringBuilder()
            .append("xxx")
            .formatFloat(1.23, 3, false)
            .append("yyy")
        assertEquals("xxx1.230yyy", tmp.toString())
    }

    @Test
    fun testFormatInt() {
        assertEquals("1", StringBuilder().formatInt(1).toString())
        assertEquals("12", StringBuilder().formatInt(12).toString())
        assertEquals("123", StringBuilder().formatInt(123).toString())
        assertEquals("1234", StringBuilder().formatInt(1234).toString())
        assertEquals("12345", StringBuilder().formatInt(12345).toString())
        assertEquals("123456", StringBuilder().formatInt(123456).toString())
        assertEquals("1234567", StringBuilder().formatInt(1234567).toString())
        assertEquals("12345678", StringBuilder().formatInt(12345678).toString())
        assertEquals("123456789", StringBuilder().formatInt(123456789).toString())
        assertEquals("-1234567", StringBuilder().formatInt(-1234567).toString())
        assertEquals("-1234567", StringBuilder().formatInt(-1234567, true).toString())
        assertEquals(" 1234567", StringBuilder().formatInt(+1234567, true).toString())
    }

    @Test
    fun testFormatTripleInt() {
        assertEquals("1", StringBuilder().formatIntTriplets(1).toString())
        assertEquals("12", StringBuilder().formatIntTriplets(12).toString())
        assertEquals("123", StringBuilder().formatIntTriplets(123).toString())
        assertEquals("1,234", StringBuilder().formatIntTriplets(1234).toString())
        assertEquals("12,345", StringBuilder().formatIntTriplets(12345).toString())
        assertEquals("123,456", StringBuilder().formatIntTriplets(123456).toString())
        assertEquals("1,234,567", StringBuilder().formatIntTriplets(1234567).toString())
        assertEquals("12,345,678", StringBuilder().formatIntTriplets(12345678).toString())
        assertEquals("123,456,789", StringBuilder().formatIntTriplets(123456789).toString())
        assertEquals("-1,234,567", StringBuilder().formatIntTriplets(-1234567).toString())
        assertEquals("-1,234,567", StringBuilder().formatIntTriplets(-1234567, true).toString())
        assertEquals(" 1,234,567", StringBuilder().formatIntTriplets(+1234567, true).toString())
    }
}