package me.anno.mesh.usd

import me.anno.io.binary.ByteArrayIO.readLE16
import me.anno.io.binary.ByteArrayIO.readLE32

class IntCodingReader(private val data: ByteArray) {

    var pos = 0
    private fun readInt32(): Int {
        val v = data.readLE32(pos)
        pos += 4
        return v
    }

    private fun readInt16(): Int {
        val v = data.readLE16(pos)
        pos += 2
        return v.shl(16).shr(16)
    }

    private fun readInt8(): Int = data[pos++].toInt()

    /**
     * Decode integer list
     */
    fun decode(count: Int): IntArray {
        println("decoding $count ints")
        val result = IntArray(count)

        // 1. Read most common value
        val common = readInt32()
        println("common: $common")

        // 2. Read 2-bit codes
        val codeCount = count
        val codeBytes = (codeCount * 2 + 7) / 8

        val pos0 = pos
        pos = pos0 + codeBytes

        // Helper to extract 2-bit code
        fun getCode(index: Int): Int {
            val bitPos = index * 2
            val byteIndex = bitPos / 8
            val shift = bitPos % 8
            return (data[pos0 + byteIndex].toInt() shr shift) and 0b11
        }

        // 4. Reconstruct original values (prefix sum)
        var prev = 0
        for (i in 0 until count) {
            if (i.and(3) == 0) {
                println("next codeByte: ${data[pos0 + i.shr(2)].toInt().and(0xff)}")
            }
            val diff = when (getCode(i)) {
                0b00 -> common
                0b01 -> readInt8()
                0b10 -> readInt16()
                else -> readInt32()
            }
            val value = if (i == 0) diff else prev + diff
            result[i] = value
            println("value[$i]: $value")
            prev = value
        }

        return result
    }
}