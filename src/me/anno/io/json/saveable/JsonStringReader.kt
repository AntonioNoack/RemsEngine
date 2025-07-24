package me.anno.io.json.saveable

import me.anno.io.saveable.Saveable
import me.anno.io.files.FileReference
import me.anno.io.saveable.ReaderImpl
import me.anno.io.saveable.StreamReader
import me.anno.io.saveable.StringReader
import java.io.EOFException
import java.io.InputStream

/**
 * reads a JSON-similar format from a text file
 * */
open class JsonStringReader(val data: CharSequence, workspace: FileReference) : JsonReaderBase(workspace) {

    private var index = 0

    override fun next(): Char {
        if (tmpChar != -1) {
            val v = tmpChar
            tmpChar = -1
            return v.toChar()
        }
        val data = data
        if (index >= data.length) throw EOFException()
        val char = data[index++]
        readNext(char)
        return char
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
        val length = data.length
        var index = index
        val data = data
        while (index < length) {
            val next = data[index++]
            readNext(next)
            when (next) {
                '\n', '\r', '\t', ' ' -> {
                }
                else -> {
                    this.index = index
                    return next
                }
            }
        }
        throw EOFException()
    }

    companion object : StringReader, StreamReader {

        override fun createReader(data: CharSequence, workspace: FileReference, sourceName: String): ReaderImpl {
            val impl = JsonStringReader(data, workspace)
            impl.sourceName = sourceName
            return impl
        }

        override fun createReader(data: InputStream, workspace: FileReference, sourceName: String): ReaderImpl {
            val impl = JsonStreamReader(data, workspace)
            impl.sourceName = sourceName
            return impl
        }

        override fun toText(element: Saveable, workspace: FileReference): String {
            return JsonStringWriter.toText(element, workspace)
        }
    }
}