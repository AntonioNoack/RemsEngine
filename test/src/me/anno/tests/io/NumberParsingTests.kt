package me.anno.tests.io

import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Strings.toDouble
import me.anno.utils.types.Strings.toInt
import me.anno.utils.types.Strings.toLong
import org.junit.jupiter.api.Test
import kotlin.math.sin

object NumberParsingTests {

    private fun c(str: String): CharSequence {
        return str
    }

    @Test
    fun testParsingDoubles() {
        assertEquals(123456.789, c("123456.789").toDouble())
        assertEquals(0.1, c("0.1").toDouble())
        assertEquals(sin(0.5), c("${sin(0.5)}").toDouble(), 1e-16)
        assertEquals(1e50, c("1e50").toDouble())
        assertEquals(.1e-7, c(".1e-7").toDouble())
        assertEquals(1e50, c("1" + "0".repeat(50)).toDouble())
    }

    @Test
    fun testParsingInts() {
        assertEquals(0b100101, c("100101").toInt(2))
        assertEquals(0x123, c("00123").toInt(16))
        assertEquals(0xcafebabe.toInt(), c("cafebabe").toInt(16))
        assertEquals(0xcaffebabeBEEF, c("caffebabeBEEF").toLong(16))
    }
}