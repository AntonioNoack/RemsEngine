package me.anno.io

import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.max
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object Base64 {

    private const val invalidCode: Byte = -1
    private val base64ToCode = ByteArray(256)
    private val codeToBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".encodeToByteArray()

    init {

        base64ToCode.fill(invalidCode)
        for (index in codeToBase64.indices) {
            val letter = codeToBase64[index]
            base64ToCode[letter.toInt()] = index.toByte()
        }

        // 62
        // base64ToCode['+'.code] = 62
        base64ToCode['-'.code] = 62

        // 63
        // base64ToCode['/'.code] = 63
        base64ToCode[','.code] = 63
        base64ToCode['_'.code] = 63

        // padding
        base64ToCode['='.code] = 0

    }

    fun encodeBase64(v: Long, writePadding: Boolean = false): String {
        val input = ByteArray(8) { v.shr(it shl 3).toInt().toByte() }
        return encodeBase64(input, writePadding)
    }

    fun encodeBase64(bytes: ByteArray, writePadding: Boolean = false): String {
        val output = ByteArrayOutputStream((bytes.size * 4 + 2) / 3)
        encodeBase64(ByteArrayInputStream(bytes), output, writePadding)
        return output.toString()
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

    fun decodeBase64(input: InputStream, output: OutputStream, throwIfUnknown: Boolean) {
        // result is shorter than the input
        val padding = '='.code
        while (true) {
            val a = input.read()
            if (a < 0 || a == padding) return
            val b = input.read()
            val c = input.read()
            val d = input.read()
            val x = base64ToCode[a and 255]
            val y = base64ToCode[b and 255]
            val z = base64ToCode[c and 255]
            val w = base64ToCode[d and 255]
            if (throwIfUnknown) {
                when {
                    x == invalidCode -> throw IOException("Illegal character in Base64: $a '${a.toChar()}'")
                    (b >= 0 && y == invalidCode) -> throw IOException("Illegal character in Base64: $b '${b.toChar()}'")
                    (c >= 0 && z == invalidCode) -> throw IOException("Illegal character in Base64: $c '${c.toChar()}'")
                    (d >= 0 && w == invalidCode) -> throw IOException("Illegal character in Base64: $d '${d.toChar()}'")
                }
            }
            val code = x.toInt().shl(18) + y.toInt().shl(12) + z.toInt().shl(6) + w
            output.write(code.shr(16).and(255))
            if (c != padding) {
                output.write(code.shr(8).and(255))
                if (d != padding) output.write(code.and(255))
            }
        }
    }

    fun encodeBase64(str: String, writePadding: Boolean): String {
        val input = str.encodeToByteArray()
        val result = ByteArrayOutputStream(ceilDiv(input.size * 4, 3))
        encodeBase64(ByteArrayInputStream(input), result, writePadding)
        return result.toString()
    }

    fun decodeBase64(str: String, throwIfUnknown: Boolean): String {
        val input = str.encodeToByteArray()
        val result = ByteArrayOutputStream(ceilDiv(input.size * 3, 4))
        decodeBase64(ByteArrayInputStream(input), result, throwIfUnknown)
        return result.toString()
    }

}