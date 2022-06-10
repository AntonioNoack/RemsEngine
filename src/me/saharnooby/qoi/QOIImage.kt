package me.saharnooby.qoi

import me.anno.image.raw.ByteImage
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/**
 * from [SaharNooby/QOI-Java](https://github.com/saharNooby/qoi-java),
 * converted from Java to Kotlin
 *
 * todo should we convert to SRGB, if linear?
 * */
class QOIImage(width: Int, height: Int, channels: Int, val linear: Boolean, data: ByteArray) :
    ByteImage(width, height, channels, data) {

    companion object {

        private const val QOI_OP_INDEX = 0x00
        private const val QOI_OP_DIFF = 0x40
        private const val QOI_OP_LUMA = 0x80
        private const val QOI_OP_RUN = 0xc0
        private const val QOI_OP_RGB = 0xfe
        private const val QOI_OP_RGBA = 0xff
        private const val QOI_MASK_2 = 0xc0
        private const val QOI_MAGIC = ('q'.code shl 24) or ('o'.code shl 16) or ('i'.code shl 8) or 'f'.code

        private const val HASH_TABLE_SIZE = 64
        private fun createHashTableRGBA() = ByteArray(HASH_TABLE_SIZE * 4)

        private fun getHashTableIndexRGBA(r: Int, g: Int, b: Int, a: Int): Int {
            val hash = (r and 0xFF) * 3 + (g and 0xFF) * 5 + (b and 0xFF) * 7 + (a and 0xFF) * 11
            return hash and 0x3F shl 2
        }

        fun findSize(input: InputStream): Pair<Int, Int> {

            val dis = DataInputStream(input)
            val headerMagic = dis.readInt()
            if (headerMagic != QOI_MAGIC) {
                throw IOException("Invalid magic value, probably not a QOI image")
            }

            val width = dis.readInt()
            val height = dis.readInt()
            if (width < 1) throw IOException("Invalid image width")
            if (height < 1) throw IOException("Invalid image height")

            return Pair(width, height)

        }

        fun read(input: InputStream): QOIImage {

            val dis = DataInputStream(input)
            val headerMagic = dis.readInt()
            if (headerMagic != QOI_MAGIC) {
                throw IOException("Invalid magic value, probably not a QOI image")
            }

            val width = dis.readInt()
            val height = dis.readInt()
            if (width < 1) throw IOException("Invalid image width")
            if (height < 1) throw IOException("Invalid image height")

            val storedChannels = dis.read()
            if (storedChannels != 3 && storedChannels != 4) {
                throw IOException("Invalid stored channel count")
            }

            val linearColorSpace = when (val value = dis.read()) {
                0 -> false
                1 -> true
                else -> throw IOException("Invalid color space value $value")
            }

            // Duplicating decoder for two specific cases improves performance almost by 26%
            val pixelData = if (storedChannels == 3) read3(dis, width, height) else read4(dis, width, height)
            return QOIImage(width, height, storedChannels, linearColorSpace, pixelData)

        }

        // Read into 3-channel RGB buffer
        private fun read3(input: DataInputStream, width: Int, height: Int): ByteArray {
            // Check for overflow on big images
            val pixelDataLength = Math.multiplyExact(Math.multiplyExact(width, height), 3)
            val pixelData = ByteArray(pixelDataLength)
            val index = createHashTableRGBA()
            var pixelR = 0
            var pixelG = 0
            var pixelB = 0
            var pixelPos = 0
            while (pixelPos < pixelDataLength) {
                val b1 = input.read()
                if (b1 == QOI_OP_RGB) {
                    pixelR = input.read()
                    pixelG = input.read()
                    pixelB = input.read()
                } else {
                    if (b1 == QOI_OP_RGBA) throw IOException("Unexpected RGBA packet in RGB file")
                    when (b1 and QOI_MASK_2) {
                        QOI_OP_INDEX -> {
                            var indexPos = b1 and QOI_MASK_2.inv() shl 2
                            pixelR = index[indexPos++].toInt()
                            pixelG = index[indexPos++].toInt()
                            pixelB = index[indexPos].toInt()
                        }
                        QOI_OP_DIFF -> {
                            pixelR += (b1 shr 4 and 3) - 2
                            pixelG += (b1 shr 2 and 3) - 2
                            pixelB += (b1 and 3) - 2
                        }
                        QOI_OP_LUMA -> {
                            // Safe widening conversion
                            val b2 = input.read()
                            val vg = (b1 and 0x3f) - 32
                            pixelR += (vg - 8 + (b2 shr 4 and 15))
                            pixelG += vg
                            pixelB += (vg - 8 + (b2 and 15))
                        }
                        QOI_OP_RUN -> {
                            val run = b1 and 0x3f
                            var i = 0
                            while (i < run) {
                                pixelData[pixelPos++] = pixelR.toByte()
                                pixelData[pixelPos++] = pixelG.toByte()
                                pixelData[pixelPos++] = pixelB.toByte()
                                i++
                            }
                        }
                    }
                }
                var indexPos = getHashTableIndexRGBA(pixelR, pixelG, pixelB, 0xff)
                index[indexPos++] = pixelR.toByte()
                index[indexPos++] = pixelG.toByte()
                index[indexPos] = pixelB.toByte()
                pixelData[pixelPos++] = pixelR.toByte()
                pixelData[pixelPos++] = pixelG.toByte()
                pixelData[pixelPos++] = pixelB.toByte()
            }
            return pixelData
        }

        private fun read4(input: DataInputStream, width: Int, height: Int): ByteArray {
            // Check for overflow on big images
            val pixelDataLength = Math.multiplyExact(Math.multiplyExact(width, height), 4)
            val pixelData = ByteArray(pixelDataLength)
            val index = createHashTableRGBA()
            var pixelR = 0
            var pixelG = 0
            var pixelB = 0
            var pixelA = 0xFF
            var pixelPos = 0
            while (pixelPos < pixelDataLength) {
                when (val b1 = input.read()) {
                    QOI_OP_RGB -> {
                        pixelR = input.read()
                        pixelG = input.read()
                        pixelB = input.read()
                    }
                    QOI_OP_RGBA -> {
                        pixelR = input.read()
                        pixelG = input.read()
                        pixelB = input.read()
                        pixelA = input.read()
                    }
                    else -> {
                        when (b1 and QOI_MASK_2) {
                            QOI_OP_INDEX -> {
                                var indexPos = b1 and QOI_MASK_2.inv() shl 2
                                pixelR = index[indexPos++].toInt()
                                pixelG = index[indexPos++].toInt()
                                pixelB = index[indexPos++].toInt()
                                pixelA = index[indexPos].toInt()
                            }
                            QOI_OP_DIFF -> {
                                pixelR += ((b1 shr 4 and 3) - 2)
                                pixelG += ((b1 shr 2 and 3) - 2)
                                pixelB += ((b1 and 3) - 2)
                            }
                            QOI_OP_LUMA -> {
                                // Safe widening conversion
                                val b2 = input.read()
                                val vg = (b1 and 0x3f) - 32
                                pixelR += (vg - 8 + (b2 shr 4 and 15)).toByte()
                                pixelG += vg.toByte()
                                pixelB += (vg - 8 + (b2 and 15)).toByte()
                            }
                            QOI_OP_RUN -> {
                                val run = b1 and 0x3F
                                var i = 0
                                while (i < run) {
                                    pixelData[pixelPos++] = pixelR.toByte()
                                    pixelData[pixelPos++] = pixelG.toByte()
                                    pixelData[pixelPos++] = pixelB.toByte()
                                    pixelData[pixelPos++] = pixelA.toByte()
                                    i++
                                }
                            }
                        }
                    }
                }
                var indexPos = getHashTableIndexRGBA(pixelR, pixelG, pixelB, pixelA)
                index[indexPos++] = pixelR.toByte()
                index[indexPos++] = pixelG.toByte()
                index[indexPos++] = pixelB.toByte()
                index[indexPos] = pixelA.toByte()
                pixelData[pixelPos++] = pixelR.toByte()
                pixelData[pixelPos++] = pixelG.toByte()
                pixelData[pixelPos++] = pixelB.toByte()
                pixelData[pixelPos++] = pixelA.toByte()
            }
            return pixelData
        }

    }

}