package me.anno.tests.maths

import me.anno.io.Base64
import kotlin.test.assertEquals

fun main() {
    val data = "Hello World"
    val base64 = "SGVsbG8gV29ybGQ="
    assertEquals(Base64.encodeBase64(data, true), base64)
    assertEquals(Base64.decodeBase64(base64, true), data)
}