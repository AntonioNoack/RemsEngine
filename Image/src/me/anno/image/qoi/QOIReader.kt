package me.anno.image.qoi

import me.anno.io.Streams.readBE32
import me.anno.maths.Maths
import me.anno.utils.Color
import org.joml.Vector2i
import java.io.IOException
import java.io.InputStream

/**
 * from https://github.com/saharNooby/qoi-java
 * converted from Java to Kotlin, and simplified a little
 * */
object QOIReader {

    private const val MAGIC = ('q'.code shl 24) or ('o'.code shl 16) or ('i'.code shl 8) or 'f'.code

    @JvmStatic
    fun findSize(input: InputStream): Any {
        val headerMagic = input.readBE32()
        if (headerMagic != MAGIC) return IOException("Invalid magic value, probably not a QOI image")
        val width = input.readBE32()
        val height = input.readBE32()
        if (width < 1) return IOException("Invalid image width")
        if (height < 1) return IOException("Invalid image height")
        return Vector2i(width, height)
    }

    /**
     * Reads a QOI Image, or returns an IOException if something goes wrong.
     * */
    @JvmStatic
    fun read(input: InputStream): Any {

        val size = findSize(input)
        if (size !is Vector2i) return size
        val (width, height) = size

        val numChannels = input.read()
        if (numChannels != 3 && numChannels != 4) {
            return IOException("Invalid stored channel count")
        }

        val linearColorSpace = when (val value = input.read()) {
            0 -> false
            1 -> true
            else -> return IOException("Invalid color space value $value")
        }

        val numPixels = Maths.multiplyExact(width, height)
        val data = IntArray(numPixels)
        val index = IntArray(64)
        val maskA = 255 shl 24
        val maskR = 255 shl 16
        val maskG = 255 shl 8
        val maskB = 255
        var pixel = maskA
        var pixelPos = 0
        while (pixelPos < numPixels) {
            val b1 = input.read()
            when (b1 shr 6) {
                0 -> pixel = index[b1 and 63] // indexed
                1 -> {// small difference
                    // if there was no overflow chance, we could use an easier calculation
                    val pixelR = pixel + (((b1 shr 4 and 3) - 2) shl 16)
                    val pixelG = pixel + (((b1 shr 2 and 3) - 2) shl 8)
                    val pixelB = pixel + (b1 and 3) - 2
                    pixel = (pixelR and maskR) or (pixelG and maskG) or (pixelB and maskB) or (pixel and maskA)
                }
                2 -> {// medium difference
                    val b2 = input.read()
                    // Safe widening conversion
                    // if there was no overflow chance, we could use an easier calculation
                    val vg = (b1 and 0x3f) - 32
                    val pixelR = pixel + ((vg - 8 + (b2 shr 4)) shl 16)
                    val pixelG = pixel + (vg shl 8)
                    val pixelB = pixel + vg - 8 + (b2 and 15)
                    pixel = (pixelR and maskR) or (pixelG and maskG) or (pixelB and maskB) or (pixel and maskA)
                }
                else -> when (b1) {
                    0xfe -> pixel = Color.rgb(input.read(), input.read(), input.read()) or (pixel and maskA)
                    0xff -> pixel = Color.rgba(input.read(), input.read(), input.read(), input.read())
                    else -> {// run-length encoding
                        val endIndex = pixelPos + (b1 and 0x3f)
                        data.fill(pixel, pixelPos, endIndex)
                        pixelPos = endIndex
                    }
                }
            }

            val hash = ((pixel shr 16) and 0xff) * 3 +
                    ((pixel shr 8) and 0xff) * 5 +
                    (pixel and 0xff) * 7 +
                    ((pixel shr 24) and 0xff) * 11

            index[hash and 63] = pixel
            data[pixelPos++] = pixel
        }

        return QOIImage(width, height, numChannels, linearColorSpace, data)
    }
}