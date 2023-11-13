package me.anno.io.files

import me.anno.maths.Maths.max
import me.anno.utils.structures.NextEntryIterator
import java.io.BufferedReader
import java.io.IOException

class ReadLineIterator(val reader: BufferedReader, val lineLengthLimit: Int, val suffix: String = "...") :
    NextEntryIterator<String>() {

    private val builder = StringBuilder()
    fun readLine(): String? {
        builder.clear()
        while (true) {
            when (val c = reader.read()) {
                -1 -> { // eof
                    reader.close()
                    return null
                }
                '\n'.code -> return builder.toString()
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
                    reader.close()
                    return null
                }
                '\n'.code -> return builder.toString()
                // everything else is ignored
            }
        }
    }

    override fun nextEntry(): String? {
        return try {
            val line = readLine()
            if (line == null) reader.close()
            line
        } catch (e: IOException) {
            reader.close()
            null
        }
    }

    fun close() {
        reader.close()
    }
}