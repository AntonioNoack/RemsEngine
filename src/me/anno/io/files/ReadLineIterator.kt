package me.anno.io.files

import me.anno.maths.MinMax.max
import me.anno.utils.structures.NextEntryIterator
import java.io.BufferedReader
import java.io.IOException

/**
 * reads lines as strings until reader is empty;
 * limits line length, and if a line is longer, the suffix is appended
 * */
class ReadLineIterator(val reader: BufferedReader, val lineLengthLimit: Int, val suffix: String = "...") :
    NextEntryIterator<String>() {

    private val builder = StringBuilder()
    private var hasReachedEnd = false
    private var nextChar: Int = -1

    fun readLine(): String? {
        return readLineRaw()?.toString()
    }

    fun readLineRaw(): CharSequence? {

        builder.clear()
        if (hasReachedEnd) return null
        if (nextChar >= 0) {
            val isLineFinished = appendChar(nextChar)
            nextChar = -1
            if (isLineFinished) return builder
        }

        while (true) {
            when (val c = reader.read()) {
                -1 -> { // eof
                    hasReachedEnd = true
                    tryClose()
                    return builder
                }
                '\n'.code -> {
                    return builder
                }
                '\r'.code -> {
                    // \r -> one line break, \r\n -> one line break
                    nextChar = reader.read()
                    if (nextChar == '\n'.code) {
                        nextChar = -1
                    }
                    return builder
                }
                else -> {
                    val isLineFinished = appendChar(c)
                    if (isLineFinished) return builder
                }
            }
        }
    }

    private fun appendChar(c: Int): Boolean {
        if (builder.length < lineLengthLimit) {
            builder.append(c.toChar())
            return false
        } else {

            // when we reach this point, the line has been too long
            builder.setLength(max(0, lineLengthLimit - suffix.length))
            builder.append(suffix)

            while (true) {
                when (reader.read()) {
                    -1 -> { // eof
                        hasReachedEnd = true
                        tryClose()
                        return true
                    }
                    '\n'.code -> return true
                    // everything else is ignored
                }
            }
        }
    }

    override fun nextEntry(): String? {
        return try {
            readLine()
        } catch (_: IOException) {
            tryClose()
            null
        }
    }

    fun close() {
        tryClose()
    }

    private fun tryClose() {
        try {
            reader.close()
        } catch (_: IOException) {
        }
    }
}