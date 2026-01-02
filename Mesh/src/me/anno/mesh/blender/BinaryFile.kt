package me.anno.mesh.blender

import me.anno.utils.Color.rgba
import me.anno.utils.assertions.assertFalse
import java.nio.ByteBuffer

class BinaryFile(val data: ByteBuffer) {

    var index = 0
    var is64Bit = false

    var headerSize = 0

    /**
     * false if Blender 5.0 or newer
     * */
    var isLegacyFile = false

    fun clone(): BinaryFile {
        val clone = BinaryFile(data)
        clone.index = index
        clone.is64Bit = is64Bit
        return clone
    }

    fun consumeIdentifier(c0: Char, c1: Char, c2: Char, c3: Char) {
        val r0 = char()
        val r1 = char()
        val r2 = char()
        val r3 = char()
        assertFalse(r0 != c0 || r1 != c1 || r2 != c2 || r3 != c3) {
            "Identifier mismatch, expected $c0$c1$c2$c3, but got $r0$r1$r2$r3"
        }
    }

    @Suppress("DuplicatedCode")
    fun consumeIdentifierWithPadding(c0: Char, c1: Char, c2: Char, c3: Char) {
        val r0 = char()
        val r1 = char()
        val r2 = char()
        val r3 = char()
        if (r0 == c0 && r1 == c1 && r2 == c2 && r3 == c3) return // 0 padding
        val r4 = char()
        if (r1 == c0 && r2 == c1 && r3 == c2 && r4 == c3) return // 1 padding
        val r5 = char()
        if (r2 == c0 && r3 == c1 && r4 == c2 && r5 == c3) return // 2 padding
        val r6 = char()
        if (r3 == c0 && r4 == c1 && r5 == c2 && r6 == c3) return // 3 padding
        throw IllegalStateException("Identifier mismatch, expected $c0$c1$c2$c3, but got $r0$r1$r2$r3")
    }

    fun read(): Byte {
        return data.get(index++)
    }

    fun char() = read().toInt().toChar()

    fun readPointer(): Long {
        if (is64Bit) {
            val v = data.getLong(index)
            index += 8
            return v
        } else {
            val v = data.getInt(index).toLong()
            index += 4
            return v
        }
    }

    fun readInt(): Int {
        val v = data.getInt(index)
        index += 4
        return v
    }

    fun readBlockCode() = rgba(read(), read(), read(), read())
    fun readShort(): Short {
        val v = data.getShort(index)
        index += 2
        return v
    }

    fun readString(length: Int): String {
        val bytes = ByteArray(length)
        data.position(index)
        data.get(bytes)
        index += length
        return bytes.decodeToString()
    }

    /**
     * read zero-terminated string
     * */
    fun read0String(): String {
        var length = 0
        val data = data
        for (i in index until data.capacity()) {
            if (data.get(i).toInt() == 0) break
            length++
        }
        val str = readString(length)
        index++ // skip \0
        return str
    }

    fun getOffset(): Int = index
    fun offset(offset: Int) {
        if (offset in 0 until data.capacity()) {
            index = offset
        } else throw UnsupportedOperationException()
    }
}