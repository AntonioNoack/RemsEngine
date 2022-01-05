package me.anno.network

import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class ResetByteArrayInputStream(val buffer: ByteArray) : InputStream() {

    private var pos = 0
    override fun read(): Int {
        return if (pos < buffer.size) {
            buffer[pos].toInt() and 255
        } else -1
    }

    override fun read(dst: ByteArray): Int {
        return read(dst, 0, dst.size)
    }

    override fun read(dst: ByteArray, startIndex: Int, length: Int): Int {
        val pos = pos
        val size = max(min(length, buffer.size - pos), 0)
        return if (length > 0 || size > 0) {
            System.arraycopy(buffer, pos, dst, startIndex, size)
            this.pos += size
            size
        } else -1
    }

    override fun available(): Int {
        return buffer.size - pos
    }

}