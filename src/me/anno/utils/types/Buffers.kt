package me.anno.utils.types

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
        for (i in position() until limit() - 1 step 2) {
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