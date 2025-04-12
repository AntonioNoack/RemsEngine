package me.anno.utils.types

import me.anno.utils.algorithms.ForLoop.forLoop
import java.io.InputStream
import java.nio.Buffer
import java.nio.ByteBuffer

object Buffers {
    @JvmStatic
    fun Buffer.skip(n: Int) {
        position(position() + n)
    }

    @JvmStatic
    fun ByteBuffer.flip16() {
        forLoop(position(), limit() - 1, 2) { i ->
            val t = this[i]
            put(i, this[i + 1])
            put(i + 1, t)
        }
    }

    @JvmStatic
    fun ByteBuffer.inputStream(): InputStream {
        return ByteBufferInputStream(this)
    }
}