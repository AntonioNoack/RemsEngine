package me.anno.image.exr

import me.anno.io.Streams.skipN
import java.awt.Rectangle
import java.awt.geom.Rectangle2D
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteOrder
import javax.imageio.stream.ImageInputStreamImpl

/**
 * OpenEXR file format reader
 *
 * @author notzed
 */
internal class OpenEXRInputStream(private val input: InputStream) : ImageInputStreamImpl() {

    init {
        setByteOrder(ByteOrder.LITTLE_ENDIAN)
    }

    override fun read(): Int {
        val v = input.read()
        bitOffset = 0
        if (v != -1) {
            streamPos++
        }
        return v
    }

    fun skipN(numBytes: Long): Long {
        bitOffset = 0
        var toReadBytes = numBytes
        while (toReadBytes > 0) {
            val n = input.skipN(toReadBytes)
            if (n < 0) throw EOFException()
            toReadBytes -= n
            streamPos += n
        }
        return numBytes
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val rlen = input.read(b, off, len)
        if (rlen > 0) {
            streamPos += rlen.toLong()
        }
        bitOffset = 0
        return rlen
    }

    fun readbox2i(): Rectangle {
        val r = Rectangle()

        r.x = readInt()
        r.y = readInt()
        r.width = readInt() - r.x + 1
        r.height = readInt() - r.y + 1
        return r
    }

    fun readbox2f(): Rectangle2D.Float {
        val r = Rectangle2D.Float()
        r.x = readFloat()
        r.y = readFloat()
        r.width = readFloat() - r.x + 1
        r.height = readFloat() - r.y + 1
        return r
    }

    fun readStringZ(): String {
        val sb = StringBuilder()
        var c: Int

        // if len > 32 ... error
        c = readByte().toInt() and 0xff
        while (c != 0) {
            sb.append(c.toChar())
            c = readByte().toInt() and 0xff
        }

        return sb.toString()
    }

    internal class ChannelAttr {
        @JvmField
        var name: String? = null

        @JvmField
        var pixType: Int = 0
        var linear: Byte = 0
        var xSampling: Int = 0
        var ySampling: Int = 0

        override fun toString(): String {
            return String.format(
                "[name=%s, type=%d, linear=%d, xsamp=%d, ysamp=%d]",
                name, pixType, linear, xSampling, ySampling
            )
        }
    }

    fun readChannels(): MutableList<ChannelAttr> {
        val chanList: ArrayList<ChannelAttr> = ArrayList()

        val name = StringBuilder()
        var c: Int

        c = readByte().toInt() and 0xff
        while (c != 0) {
            val attr = ChannelAttr()
            name.setLength(0)
            do {
                name.append(c.toChar())
                c = readByte().toInt() and 0xff
            } while (c != 0)
            attr.name = name.toString()
            println(" channel name $name")
            attr.pixType = readInt()
            println("  type = " + attr.pixType)
            attr.linear = readByte()
            println("  linear = " + attr.linear)
            readByte()
            readByte()
            readByte()
            attr.xSampling = readInt()
            println("  xSampling= " + attr.xSampling)
            attr.ySampling = readInt()
            println("  ySampling= " + attr.ySampling)
            chanList.add(attr)
            c = readByte().toInt() and 0xff
        }

        return chanList
    }
}
