package me.anno.io

import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.max
import me.anno.utils.structures.Callback
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Base64 encoder/decoder.
 * Don't instantiate this regularly, because creating it is quite expensive, because it creates two lookup tables.
 * */
open class Base64Impl(char62: Char, char63: Char) {

    private val invalidCode: Byte = -2
    private val base64ToCode = ByteArray(128)
    private val codeToBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".encodeToByteArray()

    init {

        base64ToCode.fill(invalidCode)
        for (index in codeToBase64.indices) {
            val letter = codeToBase64[index]
            base64ToCode[letter.toInt()] = index.toByte()
        }

        codeToBase64[62] = char62.code.toByte()
        codeToBase64[63] = char63.code.toByte()
        base64ToCode[char62.code] = 62
        base64ToCode[char63.code] = 63

        // padding
        base64ToCode['='.code] = 0
        base64ToCode[127] = 0
    }

    fun register(char: Char, code: Int) {
        base64ToCode[char.code] = code.toByte()
    }

    fun encodeBase64(bytes: ByteArray, writePadding: Boolean = false): String {
        val output = ByteArrayOutputStream(ceilDiv(bytes.size * 4, 3))
        encodeBase64(ByteArrayInputStream(bytes), output, writePadding)
        return output.toString()
    }

    fun decodeBase64(bytes: String): ByteArray {
        val input = bytes.byteInputStream()
        val output = ByteArrayOutputStream(ceilDiv(bytes.length * 3, 4))
        decodeBase64(input, output, false, null)
        return output.toByteArray()
    }

    fun encodeBase64(input: InputStream, output: OutputStream, writePadding: Boolean) {
        // text -> base 64
        // 3 long -> 4 long
        while (true) {
            val a = input.read()
            if (a < 0) return
            val b = input.read()
            val c = input.read()
            val value = a.shl(16) + max(b, 0).shl(8) + max(c, 0)
            output.write(codeToBase64[value.shr(18).and(63)].toInt())
            output.write(codeToBase64[value.shr(12).and(63)].toInt())
            if (b < 0) {
                if (writePadding) {
                    output.write('='.code)
                    output.write('='.code)
                }
            } else {
                output.write(codeToBase64[value.shr(6).and(63)].toInt())
                if (c < 0) {
                    if (writePadding) {
                        output.write('='.code)
                    }
                } else {
                    output.write(codeToBase64[value.and(63)].toInt())
                }
            }
        }
    }

    fun decodeBase64(
        input: InputStream,
        output: ByteArrayOutputStream,
        throwIfUnknown: Boolean,
        callback: Callback<ByteArray>?
    ) {
        // result is shorter than the input
        val padding = '='.code
        while (true) {
            val ai = input.read()
            if (ai < 0 || ai == padding) {
                callback?.ok(output.toByteArray())
                return
            }
            val bi = input.read()
            val ci = input.read()
            val di = input.read()
            val ac = base64ToCode[ai and 127]
            val bc = base64ToCode[bi and 127]
            val cc = base64ToCode[ci and 127]
            val dc = base64ToCode[di and 127]
            if (throwIfUnknown) {
                val code = when {
                    ac == invalidCode -> ai
                    bi >= 0 && bc == invalidCode -> bi
                    ci >= 0 && cc == invalidCode -> ci
                    di >= 0 && dc == invalidCode -> di
                    else -> 0
                }
                if (code != 0) {
                    callback?.err(IOException(getInvalidCharMessage(code)))
                    return
                }
            }
            val code = ac.toInt().shl(18) + bc.toInt().shl(12) + cc.toInt().shl(6) + dc
            output.write(code.shr(16))
            if (ci != padding && ci != -1) {
                output.write(code.shr(8))
                if (di != padding && di != -1) {
                    output.write(code)
                }
            }
        }
    }

    private fun getInvalidCharMessage(a: Int): String {
        return "Illegal character in Base64: $a '${a.toChar()}'"
    }

    fun encodeBase64(str: String, writePadding: Boolean): String {
        val input = str.encodeToByteArray()
        val result = ByteArrayOutputStream(ceilDiv(input.size * 4, 3))
        encodeBase64(ByteArrayInputStream(input), result, writePadding)
        return result.toString()
    }

    fun decodeBase64(str: String, throwIfUnknown: Boolean, callback: Callback<ByteArray>) {
        val input = str.encodeToByteArray()
        val result = ByteArrayOutputStream(ceilDiv(input.size * 3, 4))
        decodeBase64(ByteArrayInputStream(input), result, throwIfUnknown, callback)
    }
}