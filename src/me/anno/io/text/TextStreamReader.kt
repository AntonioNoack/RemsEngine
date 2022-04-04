package me.anno.io.text

import me.anno.io.files.FileReference
import java.io.EOFException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * reads a JSON-similar format from a text file
 * */
class TextStreamReader(data: InputStream, workspace: FileReference) : TextReaderBase(workspace) {

    private val reader = InputStreamReader(data)

    override fun next(): Char {
        if (tmpChar != -1) {
            val v = tmpChar
            tmpChar = -1
            return v.toChar()
        }
        val char = reader.read()
        if (char < 0) throw EOFException()
        readNext(char)
        return char.toChar()
    }

    override fun skipSpace(): Char {
        if (tmpChar != -1) {
            val v = tmpChar.toChar()
            tmpChar = -1
            when (v) {
                '\n', '\r', '\t', ' ' -> {
                }
                else -> return v
            }
        }
        while (true) {
            val next = reader.read()
            readNext(next)
            when (next) {
                '\n'.code, '\r'.code, '\t'.code, ' '.code -> {
                }
                -1 -> throw EOFException()
                else -> return next.toChar()
            }
        }
    }

}
