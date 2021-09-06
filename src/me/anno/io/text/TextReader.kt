package me.anno.io.text

import me.anno.io.BufferedIO.useBuffered
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream

/**
 * reads a JSON-similar format from a text file
 * */
class TextReader(val data: CharSequence) : TextReaderBase() {

    private var index = 0

    override fun next(): Char {
        if (tmpChar != -1) {
            val v = tmpChar
            tmpChar = -1
            return v.toChar()
        }
        val data = data
        if (index >= data.length) throw EOFException()
        return data[index++]
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
        val length = data.length
        var index = index
        val data = data
        while (index < length) {
            when (val next = data[index++]) {
                '\r', '\n', '\t', ' ' -> {
                }
                else -> {
                    this.index = index
                    return next
                }
            }
        }
        throw EOFException()
    }

    companion object {

        fun read(data: CharSequence): List<ISaveable> {
            val reader = TextReader(data)
            reader.readAllInList()
            // sorting is very important
            return reader.sortedContent
        }

        fun read(input: FileReference): List<ISaveable> {
            // buffered is very important and delivers an improvement of 5x
            return input.inputStream().useBuffered().use { read(it) }
        }

        fun read(data: InputStream): List<ISaveable> {
            val reader = TextStreamReader(data)
            reader.readAllInList()
            // sorting is very important
            return reader.sortedContent
        }

        fun clone(element: ISaveable): ISaveable? {
            return read(TextWriter.toText(element)).getOrNull(0)
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