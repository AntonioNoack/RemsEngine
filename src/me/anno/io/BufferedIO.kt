package me.anno.io

import java.io.*

object BufferedIO {

    fun InputStream.useBuffered(): InputStream {
        return when(this){
            is BufferedInputStream, is ByteArrayInputStream -> this
            else -> this.buffered()
        }
    }

    fun OutputStream.useBuffered(): OutputStream {
        return when(this){
            is BufferedOutputStream, is ByteArrayOutputStream -> this
            else -> this.buffered()
        }
    }

}