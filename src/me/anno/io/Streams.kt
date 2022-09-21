package me.anno.io

import me.anno.Engine
import me.anno.utils.Sleep.sleepShortly
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import kotlin.concurrent.thread

@Suppress("unused")
object Streams {

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

    fun InputStream.listen(name: String, callback: (String) -> Unit) {
        thread(name = name) {
            // some streams always return 0 for available() :(
            bufferedReader().use { reader ->
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

    fun InputStream.readText() = String(readBytes())

    fun InputStream.copy(other: OutputStream) {
        use { input ->
            other.use { output ->
                input.copyTo(output)
            }
        }
    }

    // the following functions are reading and writing functions for little and big endian integers and floats
    // half precision floats might be added in the future (on request maybe)

    fun InputStream.readBE16(): Int {
        val a = read()
        val b = read()
        if (a or b < 0) throw EOFException()
        return a.shl(8) + b
    }

    fun InputStream.readLE16(): Int {
        val a = read()
        val b = read()
        if (a or b < 0) throw EOFException()
        return a + b.shl(8)
    }

    fun InputStream.readLE32(): Int {
        val a = read()
        val b = read()
        val c = read()
        val d = read()
        if (a or b or c or d < 0) throw EOFException()
        return d.shl(24) + c.shl(16) + b.shl(8) + a
    }

    fun InputStream.readLE64(): Long {
        return readLE32().toLong().and(0xffffffff) +
                readLE32().toLong().shl(32)
    }

    fun InputStream.readBE32(): Int {
        val a = read()
        val b = read()
        val c = read()
        val d = read()
        if (a or b or c or d < 0) throw EOFException()
        return a.shl(24) + b.shl(16) + c.shl(8) + d
    }

    fun InputStream.readBE64(): Long {
        return readBE32().toLong().shl(32) +
                readBE32().toLong().and(0xffffffff)
    }

    fun InputStream.readFloatLE() = Float.fromBits(readLE32())
    fun InputStream.readDoubleLE() = Double.fromBits(readLE64())
    fun InputStream.readFloatBE() = Float.fromBits(readBE32())
    fun InputStream.readDoubleBE() = Double.fromBits(readBE64())

    fun OutputStream.writeBE16(a: Int) {
        write(a shr 8)
        write(a)
    }

    fun OutputStream.writeLE16(a: Int) {
        write(a)
        write(a shr 8)
    }

    fun OutputStream.writeBE32(a: Int) {
        write(a shr 24)
        write(a shr 16)
        write(a shr 8)
        write(a)
    }

    fun OutputStream.writeLE32(a: Int) {
        write(a)
        write(a shr 8)
        write(a shr 16)
        write(a shr 24)
    }

    /**
     * read a zero-terminated string, as they are commonly used in C
     * */
    fun InputStream.read0String(): String {
        val builder = StringBuilder()
        while (true) {
            val n = read()
            if (n == 0) break
            builder.append(n.toChar())
        }
        return builder.toString()
    }

}