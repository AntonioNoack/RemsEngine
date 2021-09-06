package me.anno.io.text

import java.io.EOFException
import java.io.InputStream

/**
 * reads a JSON-similar format from a text file
 * */
class TextStreamReader(val data: InputStream) : TextReaderBase() {

    override fun next(): Char {
        if (tmpChar != -1) {
            val v = tmpChar
            tmpChar = -1
            return v.toChar()
        }
        val x = data.read()
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
            when (val next = data.read()) {
                '\r'.code, '\n'.code, '\t'.code, ' '.code -> {
                }
                -1 -> throw EOFException()
                else -> return next.toChar()
            }
        }
    }

}

/*fun main() { // a test, because I had a bug
    val readTest = OS.desktop.getChild("fbx.yaml")
    val fakeString = TextReader.InputStreamCharSequence(readTest.inputStream(), readTest.length().toInt())
    var i = 0
    while (i < fakeString.length) {
        val char = fakeString[i++]
        print(char)
    }
    logger.info()
    logger.info("characters: $i")
}*/