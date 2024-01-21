package me.anno.io

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

@Suppress("unused")
object BufferedIO {

    fun InputStream.useBuffered(): InputStream {
        return when (this) {
            is BufferedInputStream, is ByteArrayInputStream -> this
            else -> BufferedInputStream(this)
        }
    }

    fun OutputStream.useBuffered(): OutputStream {
        return when (this) {
            is BufferedOutputStream, is ByteArrayOutputStream -> this
            else -> BufferedOutputStream(this)
        }
    }
}