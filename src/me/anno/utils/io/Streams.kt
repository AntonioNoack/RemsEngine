package me.anno.utils.io

import java.io.InputStream
import kotlin.concurrent.thread

object Streams {

    fun InputStream.listen(name: String, callback: (String) -> Unit) {
        thread(name = name) {
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