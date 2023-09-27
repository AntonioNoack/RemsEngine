package me.anno.tests.utils

import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.utils.StringMap
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NumberParsing {
    @Test
    fun testNumberParsing() {
        registerCustomClass(StringMap())
        for ((str, expected) in mapOf(
            "156231" to 156231,
            "${Int.MAX_VALUE}" to Int.MAX_VALUE,
            "${Int.MIN_VALUE}" to Int.MIN_VALUE,
            "0xff" to 255,
            "-0xff" to -255,
            "077" to "77".toInt(8),
            "#a" to 0xffaaaaaa.toInt(),
            "#abc" to 0xffaabbcc.toInt(),
            "#abcd" to 0xaabbccdd.toInt(),
            "0110" to "110".toInt(8)
        )) {
            val actual = TextReader.readFirst<StringMap>("[{\"class\":\"SMap\",\"i32:value\": $str}]", InvalidRef)
            assertEquals(expected, actual["value"])
        }
        for ((str, expected) in mapOf(
            "156231" to 156231L,
            "${Long.MAX_VALUE}" to Long.MAX_VALUE,
            "${Long.MIN_VALUE}" to Long.MIN_VALUE,
            "0xff" to 255L,
            "-0xff" to -255L,
            "077" to "77".toInt(8).toLong(),
            "#a" to 0xffaaaaaa.toInt().toLong(),
            "#abc" to 0xffaabbcc.toInt().toLong(),
            "#abcd" to 0xaabbccdd.toInt().toLong(),
            "0110" to "110".toInt(8).toLong()
        )) {
            val actual = TextReader.readFirst<StringMap>("[{\"class\":\"SMap\",\"i64:value\": $str}]", InvalidRef)
            assertEquals(expected, actual["value"])
        }
    }
}