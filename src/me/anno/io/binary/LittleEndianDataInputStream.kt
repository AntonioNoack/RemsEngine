package me.anno.io.binary

import me.anno.utils.input.readNBytes2
import java.io.EOFException
import java.io.InputStream

open class LittleEndianDataInputStream(val input: InputStream) : InputStream() {

    var position = 0L

    var putBack = -1

    fun putBack(char: Char) {
        if (putBack >= 0) throw IllegalStateException()
        putBack = char.code
    }

    override fun read(): Int {
        if (putBack >= 0) {
            val r = putBack
            putBack = -1
            return r
        }
        val r = input.read()
        if (r < 0) throw EOFException()
        position++
        return r
    }

    fun readChar(): Char {
        // skip comments
        val char = read().toChar()
        if (char == ';') {
            while (true) {
                val char2 = read()
                if (char2 == '\n'.code) {
                    return readChar()
                }
            }
        }
        return char
    }

    fun readCharSkipSpaces(): Char {
        while (true) {
            when (val char = readChar()) {
                ' ', '\t', '\n' -> Unit
                else -> return char
            }
        }
    }

    fun readCharSkipSpacesNoNL(): Char {
        while (true) {
            when (val char = readChar()) {
                ' ', '\t' -> Unit
                else -> return char
            }
        }
    }

    fun readNumber(first: Char): String {
        val str = StringBuilder(16)
        str.append(first)
        while (true) {
            when (val char = read().toChar()) {
                in '0'..'9', 'e', '+', '-', '.' -> str.append(char)
                else -> {
                    putBack(char)
                    return str.toString()
                }
            }
        }
    }

    fun readString(): String {
        val str = StringBuilder(16)
        while (true) {
            when (val char = read().toChar()) {
                '\\' -> {
                    when (val second = read().toChar()) {
                        '\\' -> str.append('\\')
                        '"' -> str.append('"')
                        else -> throw RuntimeException("Todo \\$second")
                    }
                }
                '"' -> return str.toString()
                else -> str.append(char)
            }
        }
    }

    fun readRawName(): String {
        val first = readCharSkipSpaces()
        val str = StringBuilder(16)
        str.append(first)
        while (true) {
            when (val char = read().toChar()) {
                in 'A'..'Z', in 'a'..'z', in '0'..'9', '_' -> str.append(char)
                else -> {
                    putBack(char)
                    return str.toString()
                }
            }
        }
    }

    fun readInt(): Int {
        return read() or read().shl(8) or
                read().shl(16) or read().shl(24)
    }

    fun readUInt() = readInt().toUInt().toLong()
    fun readLong(): Long {
        val a = readInt().toLong() and 0xffffffff
        val b = readInt().toLong() and 0xffffffff
        return a + b.shl(32)
    }

    fun readNBytes2(n: Int): ByteArray {
        val v = input.readNBytes2(n, true)
        position += n
        return v
    }

    fun readLength8String(): String {
        val length = input.read()
        val bytes = input.readNBytes2(length, true)
        position += length + 1
        return String(bytes)
    }

    fun read0String(): String {
        val buffer = StringBuffer(10)
        while (true) {
            val char = input.read()
            position++
            if (char < 1) {
                // end reached
                // 0 = end, -1 = eof
                return buffer.toString()
            } else {
                buffer.append(char.toChar())
            }
        }
    }

    fun assert(c0: Char, c1: Char) {
        if (c0 != c1) throw RuntimeException("Expected '$c1', but got '$c0'")
    }

    fun assert(b: Boolean, m: String? = null) {
        if (!b) throw RuntimeException(m ?: "")
    }

}