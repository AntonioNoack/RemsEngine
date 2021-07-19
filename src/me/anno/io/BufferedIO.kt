package me.anno.io

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

object BufferedIO {

    fun InputStream.useBuffered(): InputStream {
        return if(this is BufferedInputStream) this
        else this.buffered()
    }

    fun OutputStream.useBuffered(): OutputStream {
        return if(this is BufferedOutputStream) this
        else this.buffered()
    }

}