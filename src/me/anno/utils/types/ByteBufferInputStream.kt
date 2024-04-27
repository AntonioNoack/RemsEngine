package me.anno.utils.types

import me.anno.maths.Maths.min
import java.io.InputStream
import java.nio.ByteBuffer

class ByteBufferInputStream(val bytes: ByteBuffer) : InputStream() {
    override fun read(): Int = if (bytes.remaining() > 0) bytes.get().toInt().and(255) else -1
    override fun read(dst: ByteArray, off: Int, len: Int): Int {
        if (bytes.remaining() == 0) return -1
        val toBeRead = min(len, bytes.remaining())
        bytes.get(dst, off, toBeRead)
        return toBeRead
    }
}