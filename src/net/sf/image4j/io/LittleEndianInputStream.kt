/*
 * LittleEndianInputStream.java
 *
 * Created on 07 November 2006, 08:26
 *
 */
package net.sf.image4j.io

import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * Reads little-endian data from a source <tt>InputStream</tt> by reversing byte ordering.
 * Creates a new instance of <tt>LittleEndianInputStream</tt>, which will read from the specified source.
 * @param input the source <tt>InputStream</tt>
 * @author Ian McDonagh
 */
class LittleEndianInputStream(input: CountingInputStream) : DataInputStream(input) {

    val count: Int
        get() = (`in` as CountingInputStream).count

    @Throws(IOException::class)
    fun skip(count: Int, strict: Boolean): Int {
        return Utils.skip(this, count, strict)
    }

    /**
     * Reads a little-endian <tt>short</tt> value
     * @throws IOException if an error occurs
     * @return <tt>short</tt> value with reversed byte order
     */
    @Throws(IOException::class)
    fun readShortLE(): Short {
        val b1 = read()
        val b2 = read()
        if (b1 < 0 || b2 < 0) {
            throw EOFException()
        }
        return ((b2 shl 8) + b1).toShort()
    }

    /**
     * Reads a little-endian <tt>int</tt> value.
     * @throws IOException if an error occurs
     * @return <tt>int</tt> value with reversed byte order
     */
    @Throws(IOException::class)
    fun readIntLE(): Int {
        val b1 = read()
        val b2 = read()
        val b3 = read()
        val b4 = read()
        if (b1 < -1 || b2 < -1 || b3 < -1 || b4 < -1) {
            throw EOFException()
        }
        return (b4 shl 24) + (b3 shl 16) + (b2 shl 8) + b1
    }

}