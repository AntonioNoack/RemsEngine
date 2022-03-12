package me.anno.network

import java.io.OutputStream

class ResetByteArrayOutputStream(val buffer: ByteArray) : OutputStream() {

    constructor(size: Int) : this(ByteArray(size))

    var size = 0
        private set

    override fun write(p0: Int) {
        if (size < buffer.size) {
            buffer[size++] = p0.toByte()
        }
    }

    fun reset() {
        size = 0
    }

}