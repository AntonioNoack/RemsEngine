package me.anno.tests.maths

import me.anno.io.base64.Base64
import me.anno.io.base64.Base64Impl
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Base64Test {

    // messages and encodings with zero, one and two padding chars at the end
    private val msg0 = "Hello World!"
    private val enc0p = "SGVsbG8gV29ybGQh"
    private val enc0t = enc0p.trimEnd('=')

    private val msg1 = "Hello World"
    private val enc1p = "SGVsbG8gV29ybGQ="
    private val enc1t = enc1p.trimEnd('=')

    private val msg2 = "Hello World 🌍"
    private val enc2p = "SGVsbG8gV29ybGQg8J+MjQ=="
    private val enc2t = enc2p.trimEnd('=')

    @Test
    fun testEncodeWithPadding() {
        assertEquals(enc1p, Base64.encodeBase64(msg1, true))
        assertEquals(enc2p, Base64.encodeBase64(msg2, true))
        assertEquals(enc0p, Base64.encodeBase64(msg0, true))
    }

    @Test
    fun testEncodeWithoutPadding() {
        assertEquals(enc1t, Base64.encodeBase64(msg1, false))
        assertEquals(enc2t, Base64.encodeBase64(msg2, false))
        assertEquals(enc0t, Base64.encodeBase64(msg0, false))
    }

    @Test
    fun testDecodeWithPadding() {
        testDecode(enc1p, msg1)
        testDecode(enc2p, msg2)
        testDecode(enc0p, msg0)
    }

    @Test
    fun testDecodeWithoutPadding() {
        testDecode(enc1t, msg1)
        testDecode(enc2t, msg2)
        testDecode(enc0t, msg0)
    }

    @Test
    fun testDifferentEncodingChars() {
        val msg0 = "🌍!?"
        val enc0 = "8J+MjSE/"
        assertEquals(enc0, Base64.encodeBase64(msg0, true))
        testDecode(enc0, msg0)
        val inst1 = Base64Impl('(',')')
        val msg1 = "🌍!?"
        val enc1 = "8J(MjSE)"
        assertEquals(enc1, inst1.encodeBase64(msg1, true))
        testDecode(enc1, msg1, inst1)
    }

    private fun testDecode(encoded: String, msg: String, impl: Base64Impl = Base64) {
        impl.decodeBase64(encoded, true) { decoded, err ->
            assertNull(err)
            assertEquals(msg, decoded?.decodeToString())
        }
    }
}