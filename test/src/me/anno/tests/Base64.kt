package me.anno.tests

import me.anno.io.Base64

fun main() {
    val data = "Hello World"
    val base64 = "SGVsbG8gV29ybGQ="
    if (Base64.encodeBase64(data, true) != base64) throw RuntimeException()
    if (Base64.decodeBase64(base64, true) != data) throw RuntimeException()
}