package me.anno.io.files

import me.anno.maths.Maths.max
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

    fun readLine(): String? {
        return readLineRaw()?.toString()
    }

    fun readLineRaw(): CharSequence? {
        builder.clear()
        if (hasReachedEnd) {
            return null
        }
        while (true) {
            when (val c = reader.read()) {
                -1 -> { // eof
                    hasReachedEnd = true
                    reader.close()
                    return builder
                }
                '\n'.code -> return builder
                '\r'.code -> {}// ignored
                else -> if (builder.length < lineLengthLimit) {
                    builder.append(c.toChar())
                } else break
            }
        }
        // when we reach this point, the line has been too long
        builder.removeRange(0, max(0, lineLengthLimit - suffix.length))
        builder.append(suffix)
        while (true) {
            when (reader.read()) {
                -1 -> { // eof
                    hasReachedEnd = true
                    reader.close()
                    return builder
                }
                '\n'.code -> return builder
                // everything else is ignored
            }
        }
    }

    override fun nextEntry(): String? {
        return try {
            readLine()
        } catch (e: IOException) {
            reader.close()
            null
        }
    }

    fun close() {
        reader.close()
    }
}