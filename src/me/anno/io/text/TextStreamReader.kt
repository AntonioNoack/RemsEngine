package me.anno.io.text

import java.io.EOFException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * reads a JSON-similar format from a text file
 * */
class TextStreamReader(data: InputStream) : TextReaderBase() {

    private val reader = InputStreamReader(data)

    override fun next(): Char {
        if (tmpChar != -1) {
            val v = tmpChar
            tmpChar = -1
            return v.toChar()
        }
        val x = reader.read()
        if (x < 0) throw EOFException()
        return x.toChar()
    }

    override fun skipSpace(): Char {
        if (tmpChar != -1) {
            val v = tmpChar.toChar()
            tmpChar = -1
            when (v) {
                '\r', '\n', '\t', ' ' -> {
                }
                else -> return v
            }
        }
        while (true) {
            when (val next = reader.read()) {
                '\r'.code, '\n'.code, '\t'.code, ' '.code -> {
                }
                -1 -> throw EOFException()
                else -> return next.toChar()
            }
        }
    }

}
