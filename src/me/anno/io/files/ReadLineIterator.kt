package me.anno.io.files

import me.anno.io.zip.NextEntryIterator
import java.io.BufferedReader
import java.io.IOException

class ReadLineIterator(val reader: BufferedReader) : NextEntryIterator<String>() {
    override fun nextEntry(): String? {
        return try {
            val line = reader.readLine()
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