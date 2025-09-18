package me.anno.utils.types

import me.anno.maths.MinMax.min
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * turns a section of a ByteBuffer into an InputStream
 * */
class BufferInputStream(private val buffer: ByteBuffer, offset: Int, size: Int) : InputStream() {

    private var pos = offset
    private val end = offset + size

    override fun available(): Int = end - pos

    override fun read(): Int {
        return if (pos < end) buffer[pos++].toInt().and(255)
        else -1
    }

    override fun read(dst: ByteArray, start: Int, length: Int): Int {
        if (pos >= end) return -1
        val size = min(length, available())
        synchronized(buffer) {
            val pos0 = buffer.position()
            buffer.position(pos)
            buffer.get(dst, start, size)
            pos += size
            buffer.position(pos0)
        }
        return size
    }
}
