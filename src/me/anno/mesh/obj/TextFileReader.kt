package me.anno.mesh.obj

import java.io.EOFException
import java.io.InputStream
import kotlin.math.pow

open class TextFileReader(val reader: InputStream) {

    companion object {
        const val MINUS = '-'.code
        private const val ZERO = '0'.code
        private const val NINE = '9'.code
        private const val DOT = '.'.code
        private const val LOWER_E = 'e'.code
        private const val UPPER_E = 'E'.code
        private const val SPACE = ' '.code
        private const val TAB = '\t'.code
        private const val NEW_LINE = '\n'.code
        private const val NEW_LINE2 = '\r'.code
        const val SLASH = '/'.code
    }

    fun skipSpaces() {
        while (true) {
            when (val next = next()) {
                SPACE, TAB, NEW_LINE2 -> {
                }
                else -> {
                    putBack(next)
                    return
                }
            }
        }
    }

    fun skipLine() {
        if (next() == NEW_LINE) {
            // done :)
            return
        }
        val reader = reader
        while (true) {
            val code = reader.read()
            if (code == NEW_LINE || code == -1) {
                return
            }
        }
    }

    var eof = false
    var putBack = -1
    fun next(): Int {
        val char = if (putBack >= 0) putBack else reader.read()
        putBack = -1
        if (char < 0) {
            if (eof) throw EOFException()
            eof = true
            return '\n'.code // finish current line
        }
        return char
    }

    fun nextChar(): Char = next().toChar()

    fun putBack(char: Int) {
        putBack = char
    }

    fun putBack(char: Char) {
        putBack = char.code
    }

    fun readIndex(numVertices: Int): Int {
        val v = readInt()
        return if (v < 0) {
            // indices from the end of the file
            numVertices + v
        } else v - 1 // indices, starting at 1
    }

    fun readInt(ifInvalid: Int = 0): Int {
        var char = next()
        if (char == MINUS) return -readInt(-ifInvalid)
        if (char !in 48 until 58) {
            putBack = char
            return ifInvalid
        }
        var number = char - 48
        val reader = reader
        while (true) {
            char = reader.read()
            val code = (char - 48) and 255
            if (code > 9) {
                putBack = char
                return number
            }
            number = 10 * number + code
        }
    }

    fun readUntilSpace(): String {
        val builder = StringBuilder()
        while (true) {
            when (val char = next()) {
                NEW_LINE2 -> {}
                SPACE, TAB, NEW_LINE -> {
                    putBack(char)
                    return builder.toString()
                }
                else -> builder.append(char.toChar())
            }
        }
    }

    fun readUntilNewline(): String {
        val builder = StringBuilder()
        while (true) {
            when (val char = next()) {
                NEW_LINE2 -> {}
                NEW_LINE -> {
                    putBack(char)
                    return builder.toString()
                }
                else -> {
                    builder.append(char.toChar())
                }
            }
        }
    }

    private fun readFloatExp(sign: Float, number: Float): Float {
        var exponent = 0.1f
        var fraction = 0f
        while (true) {
            when (val char2 = reader.read()) {
                in ZERO..NINE -> {
                    fraction += exponent * (char2 - 48)
                    exponent *= 0.1f
                }
                LOWER_E, UPPER_E -> {
                    val power = readInt()
                    return sign * (number + fraction) * 10f.pow(power)
                }
                else -> {
                    putBack = char2
                    return sign * (number + fraction)
                }
            }
        }
    }

    // not perfect, but maybe faster
    // uses no allocations :)
    fun readFloat(): Float {
        var sign = 1f
        var number = 0L
        val reader = reader
        when (val char = next()) {
            MINUS -> sign = -sign
            in ZERO..NINE -> number = (char - 48).toLong()
            DOT -> return readFloatExp(sign, 0f)
            // else should not happen
        }
        while (true) {
            when (val char = reader.read()) {
                in ZERO..NINE -> {
                    number = number * 10 + char - 48
                }
                DOT -> return readFloatExp(sign, number.toFloat())
                LOWER_E, UPPER_E -> {
                    val power = readInt()
                    return sign * number * 10f.pow(power)
                }
                else -> {
                    putBack = char
                    return sign * number.toFloat()
                }
            }
        }
    }
}