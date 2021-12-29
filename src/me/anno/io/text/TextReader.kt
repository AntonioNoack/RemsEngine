package me.anno.io.text

import me.anno.Engine
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

        private val LOGGER = LogManager.getLogger(TextReader::class)

        fun read(data: CharSequence): List<ISaveable> {
            val reader = TextReader(data)
            reader.readAllInList()
            // sorting is very important
            return reader.sortedContent
        }

        fun read(file: FileReference): List<ISaveable> {
            // buffered is very important and delivers an improvement of 5x
            return file.inputStream().useBuffered().use {
                try {
                    println("reading $file")
                    read(it, false)
                } catch (e: Throwable) {
                    println("$e by $file")
                    Engine.shutdown()
                    throw e
                }
            }
        }

        fun read(data: InputStream, safely: Boolean): List<ISaveable> {
            val reader = TextStreamReader(data)
            if (safely) {
                try {
                    reader.readAllInList()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else reader.readAllInList()
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