package me.anno.io

import me.anno.Engine
import me.anno.utils.Sleep.sleepShortly
import me.anno.utils.hpc.Threads.threadWithName
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader

object Streams {

    fun InputStream.readLine(reader: Reader, builder: StringBuilder = StringBuilder()): String? {
        var ctr = 0
        while (!Engine.shutdown) {
            if (available() > 0) {
                when (val char = reader.read()) {
                    -1 -> return null
                    '\n'.code -> {
                        val line = builder.toString()
                        builder.clear()
                        return line
                    }
                    '\r'.code -> {}
                    else -> builder.append(char.toChar())
                }
            } else {
                if (ctr++ > 1024) {
                    ctr = 0
                    RuntimeException("Stream is waiting a long time")
                        .printStackTrace()
                }
                sleepShortly(false)
            }
        }
        return null
    }

    fun InputStream.listen(name: String, callback: (String) -> Unit) {
        threadWithName(name) {
            // some streams always return 0 for available() :(
            bufferedReader().use { reader ->
                while (!Engine.shutdown) {
                    callback(reader.readLine() ?: break)
                }
            }
            /*reader().use { reader ->
                val builder = StringBuilder()
                while (!Engine.shutdown) {
                    callback(readLine(reader, builder) ?: break)
                }
            }*/
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