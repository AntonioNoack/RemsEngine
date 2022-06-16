package me.anno.utils.types

import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.ByteBufferPool
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.min

object InputStreams {

    private val tmpBuffer = ThreadLocal2 { ByteArray(1024) }

    // defined with a 2, if already present (newer Java versions)
    fun InputStream.readNBytes2(n: Int, throwEOF: Boolean) =
        readNBytes2(n, ByteArray(n), throwEOF)

    @Throws(EOFException::class)
    fun InputStream.readNBytes2(n: Int, bytes: ByteArray, throwEOF: Boolean): ByteArray {
        var i = 0
        while (i < n) {
            val numReadChars = read(bytes, i, n - i)
            if (numReadChars < 0) {
                if (throwEOF) {
                    throw EOFException()
                } else {
                    // end :/ -> return sub array
                    val sub = ByteArray(i)
                    // src, dst
                    System.arraycopy(bytes, 0, sub, 0, i)
                    return sub
                }
            }
            i += numReadChars
        }
        return bytes
    }

    @Throws(EOFException::class)
    fun InputStream.readNBytes2(bytes: ByteArray, startIndex: Int, length: Int): ByteArray {
        var i = 0
        while (i < length) {
            val numReadChars = read(bytes, i + startIndex, length - i)
            if (numReadChars < 0) throw EOFException()
            i += numReadChars
        }
        return bytes
    }

    @Throws(EOFException::class)
    fun InputStream.readNBytes2(n: Int, bytes: ByteBuffer, throwEOF: Boolean): ByteBuffer {
        bytes.position(0)
        bytes.limit(n)
        val tmp = tmpBuffer.get()
        // we could allocate a little, temporary buffer...
        var i = 0
        while (i < n) {
            val numReadChars = read(tmp, 0, min(n - i, tmp.size))
            if (numReadChars < 0) {
                if (throwEOF) throw EOFException()
                else break
            }
            bytes.put(tmp, 0, numReadChars)
            i += numReadChars
        }
        bytes.flip()
        return bytes
    }

    @Throws(EOFException::class)
    fun InputStream.readNBytes2(n: Int, pool: ByteBufferPool): ByteBuffer {

        val tmp = tmpBuffer.get()

        // don't request a buffer from the pool, if we won't need one anyway
        val numReadChars0 = read(tmp, 0, min(n, tmp.size))
        if (numReadChars0 < 0) {
            throw EOFException()
        }

        val bytes = pool[n, false, false]
        bytes.position(0)
        bytes.limit(n)
        bytes.put(tmp, 0, numReadChars0)

        var i = numReadChars0
        while (i < n) {
            val numReadChars = read(tmp, 0, min(n - i, tmp.size))
            if (numReadChars < 0) {
                pool.returnBuffer(bytes)
                throw EOFException()
            }
            bytes.put(tmp, 0, numReadChars)
            i += numReadChars
        }
        bytes.flip()
        return bytes
    }

}