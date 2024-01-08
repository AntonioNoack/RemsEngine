package me.anno.io

import java.io.*

@Suppress("unused")
object BufferedIO {

    fun InputStream.useBuffered(): InputStream {
        return when(this){
            is BufferedInputStream, is ByteArrayInputStream -> this
            else -> BufferedInputStream(this)
        }
    }

    fun OutputStream.useBuffered(): OutputStream {
        return when(this){
            is BufferedOutputStream, is ByteArrayOutputStream -> this
            else -> BufferedOutputStream(this)
        }
    }

}