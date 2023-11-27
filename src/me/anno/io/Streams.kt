package me.anno.io

import me.anno.Engine
import me.anno.io.base.InvalidFormatException
import me.anno.utils.Sleep.sleepShortly
import java.io.*
import kotlin.concurrent.thread

/**
 * Helper class to read/write little endian, big endian, strings, or lines from Streams
 * */
@Suppress("unused")
object Streams {

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
        val bytes = readBytes()
        return String(bytes)
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
    fun InputStream.readFloatLE() = Float.fromBits(readLE32())

    @JvmStatic
    fun InputStream.readDoubleLE() = Double.fromBits(readLE64())

    @JvmStatic
    fun InputStream.readFloatBE() = Float.fromBits(readBE32())

    @JvmStatic
    fun InputStream.readDoubleBE() = Double.fromBits(readBE64())

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
    fun OutputStream.writeBE32(a: Float) {
        writeBE32(a.toRawBits())
    }

    @JvmStatic
    fun OutputStream.writeBE64(a: Double) {
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
     * read a zero-terminated string, as they are commonly used in C
     * */
    @JvmStatic
    fun OutputStream.write0String(str: String) {
        write(str.toByteArray())
        write(0)
    }

    @JvmStatic
    fun InputStream.consumeMagic(magic: String) {
        for (i in magic) {
            if (read() != i.code)
                throw InvalidFormatException("Magic incorrect")
        }
    }

}