package me.anno.network

import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class ResetByteArrayInputStream(var buffer: ByteArray) : InputStream() {

    constructor(size: Int) : this(ByteArray(size))

    fun ensureCapacity(size: Int) {
        if (buffer.size < size) {
            buffer = ByteArray(size)
        }
    }

    override fun reset() {
        size = 0
    }

    override fun skip(delta: Long): Long {
        size += delta.toInt()
        return delta
    }

    private var size = 0
    override fun read(): Int {
        return if (size < buffer.size) {
            buffer[size++].toInt() and 255
        } else -1
    }

    override fun read(dst: ByteArray): Int {
        return read(dst, 0, dst.size)
    }

    override fun read(dst: ByteArray, startIndex: Int, length: Int): Int {
        val pos = size
        val size = max(min(length, buffer.size - pos), 0)
        return if (length > 0 || size > 0) {
            System.arraycopy(buffer, pos, dst, startIndex, size)
            this.size += size
            size
        } else -1
    }

    override fun available(): Int {
        return buffer.size - size
    }

}