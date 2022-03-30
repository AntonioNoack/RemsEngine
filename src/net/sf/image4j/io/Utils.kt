package net.sf.image4j.io

import me.anno.io.xml.XMLReader.skipN
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * Provides utility methods for endian conversions [big-endian to little-endian; little-endian to big-endian].
 *
 * @author Ian McDonagh
 */
object Utils {

    @Throws(IOException::class)
    fun skip(input: InputStream, count: Int, strict: Boolean): Int {
        var skipped = 0
        while (skipped < count) {
            val b = input.read()
            if (b == -1) {
                break
            }
            skipped++
        }
        if (skipped < count && strict) {
            throw EOFException("Failed to skip $count bytes in input")
        }
        return skipped
    }

    /**
     * Reverses the byte order of the source <tt>int</tt> value
     *
     * @param value
     * the source value
     * @return the converted value
     */
    private fun swapInteger(value: Int): Int {
        return (value and -0x1000000 shr 24 or (value and 0x00FF0000 shr 8)
                or (value and 0x0000FF00 shl 8) or (value and 0x000000FF shl 24))
    }

    private fun toHexString(i0: Int, littleEndian: Boolean): String {
        var i = i0
        if (littleEndian) {
            i = swapInteger(i)
        }
        val sb = StringBuilder()
        sb.append(Integer.toHexString(i))
        if (sb.length % 2 != 0) {
            sb.insert(0, '0')
        }
        while (sb.length < 8) {
            sb.insert(0, "00")
        }
        return sb.toString()
    }

    private fun toCharString(sb: StringBuilder, i: Int) {
        var shift = 24
        for (j in 0 until 4) {
            val b = i shr shift and 0xFF
            val c = if (b < 32) '.' else b.toChar()
            sb.append(c)
            shift -= 8
        }
    }

    fun toInfoString(info: Int): String {
        val sb = StringBuilder()
        sb.append("Decimal: ").append(info)
        sb.append(", Hex BE: ").append(toHexString(info, false))
        sb.append(", Hex LE: ").append(toHexString(info, true))
        sb.append(", String BE: [")
        toCharString(sb, info)
        sb.append(']')
        sb.append(", String LE: [")
        toCharString(sb, swapInteger(info))
        sb.append(']')
        return sb.toString()
    }
}