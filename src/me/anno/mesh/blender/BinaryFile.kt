package me.anno.mesh.blender

import me.anno.utils.Color.rgba
import java.io.IOException
import java.nio.ByteBuffer

class BinaryFile(val data: ByteBuffer) {

    var index = 0
    var is64Bit = false

    fun clone(): BinaryFile {
        val clone = BinaryFile(data)
        clone.index = index
        clone.is64Bit = is64Bit
        return clone
    }

    fun consumeIdentifier(string: String) {
        for (i in string.indices) {
            val char = char()
            if (char != string[i]) throw IOException("Identifier is not matching $string, got $char at $i")
        }
    }

    fun read(): Byte {
        return data.get(index++)
    }

    fun char() = read().toInt().toChar()

    fun readLong(): Long {
        return if (is64Bit) {
            val v = data.getLong(index)
            index += 8
            v
        } else {
            val v = data.getInt(index).toLong()
            index += 4
            v
        }
    }

    fun readInt(): Int {
        val v = data.getInt(index)
        index += 4
        return v
    }

    fun readRGBA() = rgba(read(), read(), read(), read())
    fun readShort(): Short {
        val v = data.getShort(index)
        index += 2
        return v
    }

    fun readString(length: Int): String {
        return String(ByteArray(length) { data.get(index++) })
    }

    fun readByte() = read()
    fun readBoolean() = read() != 0.toByte()
    fun read0String(): String {
        // control codes?
        var length = 0
        // find \0
        val data = data
        for (i in index until data.capacity()) {
            if (data.get(i).toInt() == 0) break
            length++
        }
        val str = readString(length)
        index++ // skip \0
        return str
    }

    fun offset(): Int = index
    fun offset(offset: Int) {
        if (offset in 0 until data.capacity()) {
            index = offset
        } else throw UnsupportedOperationException()
    }

    fun offset(offset: Long) = offset(offset.toInt())

    fun skip(n: Int) {
        index += n
    }

    fun padding(alignment: Int) {
        val pos = index
        val misalignment = pos % alignment
        if (misalignment > 0) index = pos + alignment - misalignment
    }
}