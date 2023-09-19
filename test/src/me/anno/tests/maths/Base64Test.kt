package me.anno.tests.maths

import me.anno.io.Base64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Base64Test {

    private val data = "Hello World"
    private val base64 = "SGVsbG8gV29ybGQ="

    @Test
    fun testEncode() {
        assertEquals(Base64.encodeBase64(data, true), base64)
    }

    @Test
    fun testDecode() {
        assertEquals(Base64.decodeBase64(base64, true), data)
    }
}