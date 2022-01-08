package me.anno.network

import java.io.OutputStream

class ResetByteArrayOutputStream(val buffer: ByteArray) : OutputStream() {

    var size = 0
    override fun write(p0: Int) {
        buffer[size++] = p0.toByte()
    }

}