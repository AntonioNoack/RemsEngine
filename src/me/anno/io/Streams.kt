package me.anno.io

import me.anno.Engine
import me.anno.graph.hdb.ByteSlice
import me.anno.utils.Sleep.sleepShortly
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.size
import java.io.BufferedReader
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * Helper class to read/write little endian, big endian, strings, or lines from Streams
 * */
@Suppress("unused")
object Streams {

    private val tmpBuffer = ThreadLocal2 { ByteArray(1024) }

    // defined with a 2, if already present (newer Java versions)
    @JvmStatic
    fun InputStream.readNBytes2(n: Int, throwEOF: Boolean): ByteArray =
        readNBytes2(n, ByteArray(n), throwEOF)

    @JvmStatic
    fun InputStream.readNBytes2(n: Int, bytes: ByteArray, throwEOF: Boolean): ByteArray {
        var i = 0
        while (i < n) {
            val numReadChars = read(bytes, i, n - i)
            if (numReadChars < 0) {
                if (throwEOF) {
                    throw EOFException()
                } else {
                    // end :/ -> return sub array
                    return bytes.copyOf(i)
                }
            }
            i += numReadChars
        }
        return bytes
    }

    @JvmStatic
    fun InputStream.readNBytes2(bytes: ByteArray, startIndex: Int, length: Int): ByteArray {
        var i = 0
        while (i < length) {
            val numReadChars = read(bytes, i + startIndex, length - i)
            if (numReadChars < 0) throw EOFException()
            i += numReadChars
        }
        return bytes
    }

    @JvmStatic
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

    @JvmStatic
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
                throw EOFException("Only found $i/$n bytes")
            }
            bytes.put(tmp, 0, numReadChars)
            i += numReadChars
        }
        bytes.flip()
        return bytes
    }

    @JvmStatic
    fun InputStream.skipN(v: Long): InputStream {
        var read = 0L
        while (read < v) {
            val r = skip(v)
            if (r <= 0) return this
            read += r
        }
        return this
    }

    @JvmStatic
    fun InputStream.readLine(reader: Reader, builder: StringBuilder = StringBuilder()): String? {
        var ctr = 0
        while (!Engine.shutdown) {
            if (available() > 0) {
                when (val char = reader.read()) {
                    -1 -> return null
                    '\n'.code -> {
                        val line = builder.toString()
                        builder.clear()
                        return line
                    }
                    '\r'.code -> {}
                    else -> builder.append(char.toChar())
                }
            } else {
                if (ctr++ > 1024) {
                    ctr = 0
                    RuntimeException("Stream is waiting a long time")
                        .printStackTrace()
                }
                sleepShortly(false)
            }
        }
        return null
    }

    @JvmStatic
    fun InputStream.listen(name: String, callback: (String) -> Unit) {
        thread(name = name) {
            // some streams always return 0 for available() :(
            bufferedReader().use { reader: BufferedReader ->
                while (!Engine.shutdown) {
                    callback(reader.readLine() ?: break)
                }
            }
            /*reader().use { reader ->
                val builder = StringBuilder()
                while (!Engine.shutdown) {
                    callback(readLine(reader, builder) ?: break)
                }
            }*/
        }
    }

    @JvmStatic
    fun InputStream.readText(): String {
        return readBytes().decodeToString()
    }

    @JvmStatic
    fun InputStream.copy(other: OutputStream) {
        use { input: InputStream ->
            other.use { output: OutputStream ->
                input.copyTo(output)
            }
        }
    }

    // the following functions are reading and writing functions for little and big endian integers and floats
    // half precision floats might be added in the future (on request maybe)

    @JvmStatic
    fun InputStream.readBE16(): Int {
        val a = read()
        val b = read()
        if (a or b < 0) throw EOFException()
        return a.shl(8) + b
    }

    @JvmStatic
    fun InputStream.readLE16(): Int {
        val a = read()
        val b = read()
        if (a or b < 0) throw EOFException()
        return a + b.shl(8)
    }

    @JvmStatic
    fun InputStream.readLE32(): Int {
        val a = read()
        val b = read()
        val c = read()
        val d = read()
        if (a or b or c or d < 0) throw EOFException()
        return d.shl(24) + c.shl(16) + b.shl(8) + a
    }

    @JvmStatic
    fun InputStream.readLE64(): Long {
        return readLE32().toLong().and(0xffffffff) +
                readLE32().toLong().shl(32)
    }

    @JvmStatic
    fun InputStream.readBE32(): Int {
        val a = read()
        val b = read()
        val c = read()
        val d = read()
        if (a or b or c or d < 0) throw EOFException()
        return a.shl(24) + b.shl(16) + c.shl(8) + d
    }

    @JvmStatic
    fun InputStream.readBE64(): Long {
        return readBE32().toLong().shl(32) +
                readBE32().toLong().and(0xffffffff)
    }

    @JvmStatic
    fun InputStream.readLE32F() = Float.fromBits(readLE32())

    @JvmStatic
    fun InputStream.readLE64F() = Double.fromBits(readLE64())

    @JvmStatic
    fun InputStream.readBE32F() = Float.fromBits(readBE32())

    @JvmStatic
    fun InputStream.readBE64F() = Double.fromBits(readBE64())

    @JvmStatic
    fun OutputStream.writeBE16(a: Int) {
        write(a shr 8)
        write(a)
    }

    @JvmStatic
    fun OutputStream.writeLE16(a: Int) {
        write(a)
        write(a shr 8)
    }

    @JvmStatic
    fun OutputStream.writeBE24(a: Int) {
        write(a shr 16)
        write(a shr 8)
        write(a)
    }

    @JvmStatic
    fun OutputStream.writeBE32(a: Int) {
        write(a shr 24)
        write(a shr 16)
        write(a shr 8)
        write(a)
    }

    @JvmStatic
    fun OutputStream.writeBE64(a: Long) {
        writeBE32((a shr 32).toInt())
        writeBE32(a.toInt())
    }

    @JvmStatic
    fun OutputStream.writeLE32(a: Int) {
        write(a)
        write(a shr 8)
        write(a shr 16)
        write(a shr 24)
    }

    @JvmStatic
    fun OutputStream.writeLE64(a: Long) {
        writeLE32(a.toInt())
        writeLE32((a shr 32).toInt())
    }

    @JvmStatic
    fun OutputStream.writeLE32(a: Float) {
        writeLE32(a.toRawBits())
    }

    @JvmStatic
    fun OutputStream.writeLE64(a: Double) {
        writeLE64(a.toRawBits())
    }

    @JvmStatic
    fun OutputStream.writeBE32F(a: Float) {
        writeBE32(a.toRawBits())
    }

    @JvmStatic
    fun OutputStream.writeBE64F(a: Double) {
        writeBE64(a.toRawBits())
    }

    /**
     * read a zero-terminated string, as they are commonly used in C
     * */
    @JvmStatic
    fun InputStream.read0String(): String {
        val builder = StringBuilder()
        while (true) {
            val n = read()
            if (n == 0) break
            if (n < 0) throw EOFException()
            builder.append(n.toChar())
        }
        return builder.toString()
    }

    /**
     * write a zero-terminated string, as they are commonly used in C
     * */
    @JvmStatic
    fun OutputStream.write0String(str: String) {
        writeString(str)
        write(0)
    }

    @JvmStatic
    fun OutputStream.writeString(str: String) {
        write(str.encodeToByteArray())
    }

    @JvmStatic
    fun InputStream.consumeMagic(magic: String) {
        for (i in magic) {
            if (read() != i.code) {
                throw IOException("Magic incorrect")
            }
        }
    }

    @JvmStatic
    fun InputStream.consumeMagic(magic: ByteArray) {
        for (i in magic.indices) {
            if (read() != magic[i].toInt().and(255)) {
                throw IOException("Magic incorrect")
            }
        }
    }

    @JvmStatic
    fun OutputStream.writeNBytes2(src: ByteBuffer) {
        val tmp = tmpBuffer.get()
        val pos = src.position()
        while (src.remaining() > 0) {
            val length = min(tmp.size, src.remaining())
            src.get(tmp, 0, length)
            write(tmp, 0, length)
        }
        src.position(pos)
    }

    @JvmStatic
    fun OutputStream.writeNBytes2(src: ByteBuffer, offset: Int, length: Int) {
        val pos = src.position()
        val limit = src.limit()
        src.position(pos + offset).limit(pos + offset + length)
        writeNBytes2(src)
        src.limit(limit).position(pos)
    }

    @JvmStatic
    fun OutputStream.write(src: ByteSlice) {
        write(src.bytes, src.range.first(), src.range.size)
    }
}