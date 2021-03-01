package me.anno.utils.io

import me.anno.utils.Threads.threadWithName
import java.io.InputStream
import kotlin.concurrent.thread

object Streams {

    fun InputStream.listen(name: String, callback: (String) -> Unit) {
        threadWithName(name) {
            val reader = bufferedReader()
            while (true) {
                reader.read()
                val line = reader.readLine() ?: break
                callback(line)
            }
            reader.close()
        }
    }

    fun InputStream.readText() = String(readBytes())

}