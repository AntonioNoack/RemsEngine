package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream

/**
 * reads a JSON-similar format from a text file
 * */
class TextReader(val data: CharSequence, workspace: FileReference) : TextReaderBase(workspace) {

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

    companion object {

        private val LOGGER = LogManager.getLogger(TextReader::class)

        /**
         * parses a Json* formatted string
         * @param safely return current results on failure, else throws Exception
         * */
        fun read(data: CharSequence, workspace: FileReference, safely: Boolean): List<ISaveable> {
            return read(data, workspace, "", safely)
        }

        /**
         * parses a Json* formatted string
         * @param safely return current results on failure, else throws Exception
         * */
        fun read(data: CharSequence, workspace: FileReference, sourceName: String, safely: Boolean): List<ISaveable> {
            val reader = TextReader(data, workspace)
            reader.sourceName = sourceName
            if (safely) {
                try {
                    reader.readAllInList()
                } catch (e: Exception) {
                    LOGGER.warn("Error in $sourceName", e)
                }
            } else {
                reader.readAllInList()
            }
            reader.finish()
            // sorting is very important
            return reader.sortedContent
        }

        fun read(file: FileReference, workspace: FileReference, safely: Boolean): List<ISaveable> {
            // buffered is very important and delivers an improvement of 5x
            return file.inputStreamSync().use { input: InputStream ->
                read(input, workspace, file.absolutePath, safely)
            }
        }

        fun read(data: InputStream, workspace: FileReference, safely: Boolean): List<ISaveable> {
            return read(data, workspace, "", safely)
        }

        fun read(data: InputStream, workspace: FileReference, sourceName: String, safely: Boolean): List<ISaveable> {
            val reader = TextStreamReader(data, workspace)
            reader.sourceName = sourceName
            if (safely) {
                try {
                    reader.readAllInList()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else reader.readAllInList()
            reader.finish()
            // sorting is very important
            return reader.sortedContent
        }

        inline fun <reified Type> readFirstOrNull(
            data: FileReference,
            workspace: FileReference,
            safely: Boolean = true
        ): Type? {
            return read(data, workspace, safely).firstInstanceOrNull<Type>()
        }

        inline fun <reified Type> readFirstOrNull(
            data: String,
            workspace: FileReference,
            safely: Boolean = true
        ): Type? {
            return read(data, workspace, safely).firstInstanceOrNull<Type>()
        }

        inline fun <reified Type> readFirst(data: String, workspace: FileReference, safely: Boolean = true): Type {
            return read(data, workspace, safely).firstInstanceOrNull<Type>()!!
        }

        fun <V : ISaveable> clone(element: V): V? {
            val clone = read(TextWriter.toText(element, InvalidRef), InvalidRef, true).getOrNull(0)
            @Suppress("unchecked_cast")
            return clone as? V
        }

    }
}
