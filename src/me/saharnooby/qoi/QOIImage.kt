package me.saharnooby.qoi

import me.anno.image.raw.IntImage
import me.anno.io.Streams.readBE32
import java.io.IOException
import java.io.InputStream

/**
 * from https://github.com/saharNooby/qoi-java
 * converted from Java to Kotlin, and simplified a little
 *
 * todo should we convert to SRGB, if linear?
 * */
class QOIImage(width: Int, height: Int, channels: Int, val linear: Boolean, data: IntArray) :
    IntImage(width, height, data, channels > 3) {

    companion object {

        private const val MAGIC = ('q'.code shl 24) or ('o'.code shl 16) or ('i'.code shl 8) or 'f'.code

        fun findSize(input: InputStream): Pair<Int, Int> {
            val headerMagic = input.readBE32()
            if (headerMagic != MAGIC) throw IOException("Invalid magic value, probably not a QOI image")
            val width = input.readBE32()
            val height = input.readBE32()
            if (width < 1) throw IOException("Invalid image width")
            if (height < 1) throw IOException("Invalid image height")
            return Pair(width, height)
        }

        fun rgb(r: Int, g: Int, b: Int) = r.shl(16) or g.shl(8) or b

        fun read(input: InputStream): QOIImage {

            val (width, height) = findSize(input)

            val numChannels = input.read()
            if (numChannels != 3 && numChannels != 4) {
                throw IOException("Invalid stored channel count")
            }

            val linearColorSpace = when (val value = input.read()) {
                0 -> false
                1 -> true
                else -> throw IOException("Invalid color space value $value")
            }

            val numPixels = Math.multiplyExact(width, height)
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
                        0xfe -> pixel = rgb(input.read(), input.read(), input.read()) or (pixel and maskA) // rgb
                        0xff -> pixel = rgb(input.read(), input.read(), input.read()) or (input.read() shl 24) // rgba
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
}