package me.anno.utils.io

import me.anno.Engine
import me.anno.utils.hpc.Threads.threadWithName
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader

object Streams {

    fun InputStream.readLine2(reader: Reader): String? {
        val builder = StringBuilder()
        while (!Engine.shutdown) {
            if (available() > 0) {
                when (val char = reader.read()) {
                    -1 -> break
                    '\n'.code -> {
                        val line = builder.toString()
                        builder.clear()
                        return line
                    }
                    '\r'.code -> {}
                    else -> builder.append(char.toChar())
                }
            }
        }
        return null
    }

    fun InputStream.listen(name: String, callback: (String) -> Unit) {
        threadWithName(name) {
            reader().use { reader ->
                val builder = StringBuilder()
                while (!Engine.shutdown) {
                    if (available() > 0) {
                        when (val char = reader.read()) {
                            -1 -> break
                            '\n'.code -> {
                                callback(builder.toString())
                                builder.clear()
                            }
                            '\r'.code -> {}
                            else -> builder.append(char.toChar())
                        }
                    }
                }
                callback(builder.toString())
            }
        }
    }

    fun InputStream.readText() = String(readBytes())

    fun InputStream.copy(other: OutputStream) {
        use { input ->
            other.use { output ->
                input.copyTo(output)
            }
        }
    }

}