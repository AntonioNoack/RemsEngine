package me.anno.tests.utils

import me.anno.utils.Color.black
import me.anno.utils.Color.rgba
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class ColorParsingTest {

    @Test
    fun testHex3() {
        assertEquals(0xff112233.toInt(), parseColor("#123"))
        assertEquals(0xff112233.toInt(), parseColor("0x123"))
    }

    @Test
    fun testHex4() {
        assertEquals(0x44112233, parseColor("#1234"))
        assertEquals(0x44112233, parseColor("0x1234"))
    }

    @Test
    fun testHex6() {
        assertEquals(0xff345678.toInt(), parseColor("#345678"))
        assertEquals(0xff345678.toInt(), parseColor("0x345678"))
    }

    @Test
    fun testHex8() {
        assertEquals(0x78123456, parseColor("#12345678"))
        assertEquals(0x78123456, parseColor("0x12345678"))
    }

    @Test
    fun testParsingFailure() {
        assertNull(parseColor("none"))
    }

    @Test
    fun testNamed() {
        assertEquals(0xffff0000.toInt(), parseColor("red"))
        assertEquals(0xffff0000.toInt(), parseColor("ReD"))
        assertEquals(-1, parseColor("white"))
        assertEquals(black, parseColor("black"))
        assertEquals(0xff0000ff.toInt(), parseColor("blue"))
        assertEquals(0xffff00ff.toInt(), parseColor("magenta"))
        assertEquals(0xff00ff00.toInt(), parseColor("lime"))
    }

    @Test
    fun testRGB() {
        assertEquals(0xff112233.toInt(), parseColor("rgb(17,34,51)"))
        assertEquals(rgba(0.1f, 0.2f, 0.3f, 1f), parseColor("rgb(0.1,0.2,0.3)"))
        assertEquals(rgba(0.11f, 0.21f, 0.31f, 1f), parseColor("rgb(11%,21%,31%)"))
    }

    @Test
    fun testRGBA() {
        assertEquals(0xff112233.toInt(), parseColor("rgba(17,34,51,255)"))
        assertEquals(0x44112233, parseColor("rgba(17,34,51,68)"))
        assertEquals(rgba(0.1f, 0.2f, 0.3f, 0.4f), parseColor("rgba(0.1,0.2,0.3,0.4)"))
        assertEquals(rgba(0.11f, 0.21f, 0.31f, 0.41f), parseColor("rgba(11%,21%,31%,41%)"))
        assertEquals(0xff112233.toInt(), parseColor("rgba(17,34,51,1.0)"))
    }
}