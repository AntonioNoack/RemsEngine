package me.anno.utils

import java.io.InputStream
import kotlin.concurrent.thread

object Streams {

    fun InputStream.listen(callback: (String) -> Unit){
        thread {
            val reader = bufferedReader()
            while(true){
                val line = reader.readLine() ?: break
                callback(line)
            }
            reader.close()
        }
    }

    fun InputStream.readText() = String(readBytes())

}