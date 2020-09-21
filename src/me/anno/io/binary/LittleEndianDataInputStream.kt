package me.anno.io.binary

import me.anno.utils.readNBytes2
import java.io.EOFException
import java.io.InputStream
import java.lang.RuntimeException

open class LittleEndianDataInputStream(val input: InputStream): InputStream(){

    var position = 0L

    override fun read(): Int {
        val r = input.read()
        if(r < 0) throw EOFException()
        position++
        return r
    }

    fun readInt(): Int {
        return read() or read().shl(8) or
                read().shl(16) or read().shl(24)
    }

    fun readUInt() = readInt().toUInt().toLong()
    fun readLong(): Long {
        val a = readInt().toLong() and 0xffffffff
        val b = readInt().toLong() and 0xffffffff
        return a + b.shl(32)
    }

    fun readNBytes2(n: Int): ByteArray {
        val v = input.readNBytes2(n)
        position += n
        return v
    }

    fun readLength8String(): String {
        val length = input.read()
        val bytes = input.readNBytes2(length)
        position += length + 1
        return String(bytes)
    }

    fun read0String(): String {
        val buffer = StringBuffer(10)
        while(true){
            val char = input.read()
            position++
            if(char < 1){
                // end reached
                // 0 = end, -1 = eof
                return buffer.toString()
            } else {
                buffer.append(char.toChar())
            }
        }
    }

    fun assert(b: Boolean, m: String? = null){
        if(!b) throw RuntimeException(m ?: "")
    }

}