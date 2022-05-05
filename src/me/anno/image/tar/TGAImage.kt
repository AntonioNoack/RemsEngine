/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * blablabla,
 *
 * I am trying to support everything, so I'll be extending it
 */
package me.anno.image.tar

import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.io.BufferedIO.useBuffered
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * @author Mark Powell
 * @author Joshua Slack - cleaned, commented, added ability to read 16bit true color and color-mapped TGAs.
 * @author Kirill Vainer - ported to jME3
 * @author Antonio Noack - added black & white support; fixed naming (?), tested with crytek sponza; fixed 32 bit color order(?)
 * at least for my test cases, everything was correct, and the same as Gimp
 * @version $Id: TGALoader.java 4131 2009-03-19 20:15:28Z blaine.dev $
 */
class TGAImage(// bgra, even if the implementation calls it rgba
    var data: ByteArray, width: Int, height: Int, channels: Int
) : Image(width, height, channels, channels > 3) {
    var originalImageType = 0
    var originalPixelDepth = 0
    override fun createTexture(texture: Texture2D, checkRedundancy: Boolean) {
        when (numChannels) {
            1 -> texture.createMonochrome(data, checkRedundancy)
            2 -> texture.createRG(data, checkRedundancy)
            3 -> texture.createBGR(data, checkRedundancy)
            4 -> texture.createBGRA(data, checkRedundancy)
            else -> throw RuntimeException("$numChannels channels?")
        }
    }

    override fun getRGB(index: Int): Int {
        return when (numChannels) {
            1 -> 0x10101 * (data[index].toInt() and 255)
            2 -> {
                val j = index * 2
                bgra2rgba(0, data[j].toInt(), data[j + 1].toInt(), 255)
            }
            3 -> {
                val j = index * 3
                bgra2rgba(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), 255)
            }
            4 -> {
                val j = index * 4
                bgra2rgba(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), data[j + 3].toInt())
            }
            else -> throw RuntimeException("$numChannels is not supported for TGA images")
        }
    }

    override fun createBufferedImage(): BufferedImage {
        val width = width
        val height = height
        val channels = numChannels
        if (channels == 2) return super.createBufferedImage()
        val image = BufferedImage(
            width, height,
            if (channels > 3) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        )
        val buffer = image.raster.dataBuffer
        val size = width * height
        val data = data
        when (channels) {
            1 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    buffer.setElem(i, 0x10101 * (data[j].toInt() and 255))
                    i++
                    j++
                }
            }
            3 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    buffer.setElem(i, bgra2rgba(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), 255))
                    i++
                    j += 3
                }
            }
            4 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    buffer.setElem(
                        i,
                        bgra2rgba(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), data[j + 3].toInt())
                    )
                    i++
                    j += 4
                }
            }
            else -> throw RuntimeException("$channels channels is not supported for TGA images")
        }
        return image
    }

    override fun createIntImage(): IntImage {
        val width = width
        val height = height
        val channels = numChannels
        if (channels != 1 && channels != 3 && channels != 4) return super.createIntImage()
        val size = width * height
        val data = data
        val dst = IntArray(size)
        when (channels) {
            1 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    dst[i] = 0x10101 * (data[j].toInt() and 255)
                    i++
                    j++
                }
            }
            3 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    dst[i] = bgra2rgba(
                        data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), 255
                    )
                    i++
                    j += 3
                }
            }
            4 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    dst[i] = bgra2rgba(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), data[j + 3].toInt())
                    i++
                    j += 4
                }
            }
        }
        return IntImage(width, height, dst, hasAlphaChannel)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(TGAImage::class)

        private fun bgra2rgba(b: Int, g: Int, r: Int, a: Int): Int {
            return r and 255 shl 16 or (g and 255 shl 8) or (b and 255) or (a and 255 shl 24)
        }

        // 0 - no image data in file
        private const val TYPE_NO_IMAGE = 0

        // 1 - uncompressed, color-mapped image
        private const val TYPE_COLORMAPPED = 1

        // 2 - uncompressed, true-color image
        private const val TYPE_TRUECOLOR = 2

        // 3 - uncompressed, black and white image
        private const val TYPE_BLACKANDWHITE = 3

        // 9 - run-length encoded, color-mapped image
        private const val TYPE_COLORMAPPED_RLE = 9

        // 10 - run-length encoded, true-color image
        private const val TYPE_TRUECOLOR_RLE = 10

        // 11 - run-length encoded, black and white image
        private const val TYPE_BLACKANDWHITE_RLE = 11

        /**
         * `loadImage` is a manual image loader which is entirely
         * independent of AWT. OUT: RGB888 or RGBA8888 Image object
         *
         * @param input InputStream of an uncompressed 24b RGB or 32b RGBA TGA
         * @param flip Flip the image vertically
         * @return `Image` object that contains the
         * image, either as a R8, a RGB888 or RGBA8888
         * @throws IOException if an I/O error occurs
         */
        @Throws(IOException::class)
        fun read(input: InputStream, flip: Boolean): TGAImage {

            var flipY = flip
            var flipX = false

            // open a stream to the file
            val dis = DataInputStream(input.useBuffered())

            // ---------- Start Reading the TGA header ---------- //
            // length of the image id (1 byte)
            val idLength = dis.readUnsignedByte()

            // Type of color map (if any) included with the image
            // 0 - no color map data is included
            // 1 - a color map is included
            val colorMapType = dis.readUnsignedByte()

            // Type of image being read:
            val imageType = dis.readUnsignedByte()

            // Read Color Map Specification (5 bytes)
            // Index of first color map entry (if we want to use it, uncomment and remove extra read.)
            // short cMapStart = flipEndian(dis.readShort());
            dis.readShort()
            // number of entries in the color map
            val cMapLength = flipEndian(dis.readShort())
            // number of bits per color map entry
            val cMapDepth = dis.readUnsignedByte()

            // Read Image Specification (10 bytes)
            // horizontal coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
            // int xOffset = flipEndian(dis.readShort());
            dis.readShort()
            // vertical coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
            // int yOffset = flipEndian(dis.readShort());
            dis.readShort()
            // width of image - in pixels
            val width = flipEndian(dis.readShort()).toInt()
            // height of image - in pixels
            val height = flipEndian(dis.readShort()).toInt()
            // bits per pixel in image.
            val pixelDepth = dis.readUnsignedByte()
            val imageDescriptor = dis.readUnsignedByte()
            if (imageDescriptor and 32 != 0) { // bit 5 : if 1, flip top/bottom ordering
                flipY = !flipY
            }
            if (imageDescriptor and 16 != 0) { // bit 4 : if 1, flip left/right ordering
                flipX = true
                LOGGER.warn("X-flipped TGAs haven't been implemented yet")
            }

            // ---------- Done Reading the TGA header ---------- //

            // Skip image ID
            if (idLength > 0) {
                dis.skipBytes(idLength)
            }
            var cMapEntries: IntArray? = null
            if (colorMapType != 0) {

                // read the color map
                val bytesInColorMap = cMapDepth * cMapLength shr 3
                val bitsPerColor = min(cMapDepth / 3, 8)
                val cMapData = ByteArray(bytesInColorMap)
                dis.readFully(cMapData, 0, bytesInColorMap)

                // Only go to the trouble of constructing the color map
                // table if this is declared a color mapped image.
                if (imageType == TYPE_COLORMAPPED || imageType == TYPE_COLORMAPPED_RLE) {
                    cMapEntries = IntArray(cMapLength.toInt())
                    val alphaSize = cMapDepth - 3 * bitsPerColor
                    val scalar = 255f / ((1 shl bitsPerColor) - 1)
                    val alphaScalar = 255f / ((1 shl alphaSize) - 1)
                    var r: Int
                    var g: Int
                    var b: Int
                    var a = 255
                    for (i in 0 until cMapLength) {
                        val offset = cMapDepth * i
                        b = (getBitsAsByte(cMapData, offset, bitsPerColor) * scalar).toInt()
                        g = (getBitsAsByte(cMapData, offset + bitsPerColor, bitsPerColor) * scalar).toInt()
                        r = (getBitsAsByte(cMapData, offset + 2 * bitsPerColor, bitsPerColor) * scalar).toInt()
                        if (alphaSize > 0) {
                            a = (getBitsAsByte(cMapData, offset + 3 * bitsPerColor, alphaSize) * alphaScalar).toInt()
                        }
                        cMapEntries[i] = abgr(r, g, b, a)
                    }
                }
            }


            // Allocate image data array
            val format: Int
            val dl =
                if (pixelDepth == 32) 4 else if (imageType == TYPE_BLACKANDWHITE || imageType == TYPE_BLACKANDWHITE_RLE) 1 else 3
            val rawData = ByteArray(width * height * dl)
            format = when (imageType) {
                TYPE_TRUECOLOR -> readTrueColor(pixelDepth, width, height, flipY, rawData, dl, dis)
                TYPE_TRUECOLOR_RLE -> readTrueColorRLE(pixelDepth, width, height, flipY, rawData, dl, dis)
                TYPE_COLORMAPPED -> readColorMapped(pixelDepth, width, height, flipY, rawData, dl, dis, cMapEntries)
                TYPE_NO_IMAGE -> throw IOException("No image is not supported")
                TYPE_BLACKANDWHITE -> readGrayscale(pixelDepth, width, height, flipY, rawData, dl, dis)
                TYPE_COLORMAPPED_RLE -> throw IOException("Colormapped RLE is not supported")
                TYPE_BLACKANDWHITE_RLE -> throw IOException("Black & White RLE is not supported")
                else -> throw IOException("Unknown TGA type $imageType")
            }
            input.close()

            // Create the Image object
            val image = TGAImage(rawData, width, height, format)
            image.originalImageType = imageType
            image.originalPixelDepth = pixelDepth
            return image
        }

        @Throws(IOException::class)
        private fun readColorMapped(
            pixelDepth: Int,
            width: Int,
            height: Int,
            flip: Boolean,
            rawData: ByteArray,
            dl: Int,
            dis: DataInputStream,
            cMapEntries: IntArray?
        ): Int {
            var rawDataIndex = 0
            when (val bytesPerIndex = pixelDepth / 8) {
                1 -> {
                    for (i in 0 until height) {
                        if (!flip) {
                            rawDataIndex = (height - 1 - i) * width * dl
                        }
                        for (j in 0 until width) {
                            val index = dis.readUnsignedByte()
                            if (index >= cMapEntries!!.size) {
                                throw IOException("TGA: Invalid color map entry referenced: $index")
                            }
                            val entry = cMapEntries[index]
                            rawData[rawDataIndex++] = (entry shr 16).toByte()
                            rawData[rawDataIndex++] = (entry shr 8).toByte()
                            rawData[rawDataIndex++] = entry.toByte()
                            if (dl == 4) {
                                rawData[rawDataIndex++] = (entry ushr 24).toByte()
                            }
                        }
                    }
                }
                2 -> {
                    for (i in 0 until height) {
                        if (!flip) {
                            rawDataIndex = (height - 1 - i) * width * dl
                        }
                        for (j in 0 until width) {
                            val index = flipEndian(dis.readShort()).toInt()
                            if (index >= cMapEntries!!.size || index < 0) {
                                throw IOException("TGA: Invalid color map entry referenced: $index")
                            }
                            val entry = cMapEntries[index]
                            rawData[rawDataIndex++] = (entry shr 16).toByte()
                            rawData[rawDataIndex++] = (entry shr 8).toByte()
                            rawData[rawDataIndex++] = entry.toByte()
                            if (dl == 4) {
                                rawData[rawDataIndex++] = (entry shr 24).toByte()
                            }
                        }
                    }
                }
                else -> throw IOException("TGA: unknown ColorMap indexing size used: $bytesPerIndex")
            }
            return if (dl == 4) 4 else 3
        }

        @Throws(IOException::class)
        private fun readTrueColor(
            pixelDepth: Int,
            width: Int,
            height: Int,
            flip: Boolean,
            rawData: ByteArray,
            dl: Int,
            dis: DataInputStream
        ): Int {

            var rawDataIndex = 0

            // Faster than doing a 16-or-24-or-32 check on each individual pixel,
            // just make a separate loop for each.
            return when (pixelDepth) {
                16 -> {
                    val data = ByteArray(2)
                    val scalar = 255f / 31f
                    for (i in 0 until height) {
                        if (!flip) {
                            rawDataIndex = (height - 1 - i) * width * dl
                        }
                        for (j in 0 until width) {
                            data[1] = dis.readByte()
                            data[0] = dis.readByte()
                            rawData[rawDataIndex++] = (getBitsAsByte(data, 1, 5) * scalar).toInt().toByte()
                            rawData[rawDataIndex++] = (getBitsAsByte(data, 6, 5) * scalar).toInt().toByte()
                            rawData[rawDataIndex++] = (getBitsAsByte(data, 11, 5) * scalar).toInt().toByte()
                            if (dl == 4) {
                                // create an alpha channel
                                var a = getBitsAsByte(data, 0, 1)
                                if (a.toInt() == 1) {// what, why?
                                    a = 255.toByte()
                                }
                                rawData[rawDataIndex++] = a
                            }
                        }
                    }
                    if (dl == 4) 4 else 3
                }
                24 -> {
                    for (y in 0 until height) {
                        rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                        dis.readFully(rawData, rawDataIndex, width * dl)
                    }
                    3
                }
                32 -> {
                    for (y in 0 until height) {
                        rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                        dis.readFully(rawData, rawDataIndex, width * 4)
                    }
                    4
                }
                else -> throw IOException("Unsupported TGA true color depth: $pixelDepth")
            }
        }

        @Throws(IOException::class)
        private fun readGrayscale(
            pixelDepth: Int,
            width: Int,
            height: Int,
            flip: Boolean,
            rawData: ByteArray,
            dl: Int,
            dis: DataInputStream
        ): Int {
            for (y in 0 until height) {
                var rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                for (x in 0 until width) {
                    val v = dis.readByte()
                    rawData[rawDataIndex++] = v
                    if (pixelDepth >= 16) rawData[rawDataIndex++] = v
                    if (pixelDepth >= 24) rawData[rawDataIndex++] = v
                    if (pixelDepth >= 32) rawData[rawDataIndex++] = 255.toByte()
                }
            }
            return pixelDepth / 8
        }

        @Throws(IOException::class)
        private fun readTrueColorRLE(
            pixelDepth: Int,
            width: Int,
            height: Int,
            flip: Boolean,
            rawData: ByteArray,
            dl: Int,
            dis: DataInputStream
        ): Int {

            val format: Int
            var rawDataIndex: Int
            var b: Byte
            var g: Byte
            var r: Byte
            var a: Byte

            // Faster than doing a 16-or-24-or-32 check on each individual pixel,
            // just make a separate loop for each.
            when (pixelDepth) {
                32 -> {
                    for (y in 0 until height) {
                        rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                        var x = 0
                        while (x < width) {

                            // Get the number of pixels the next chunk covers (either packed or unpacked)
                            var count = dis.readByte().toInt()
                            if (count and 0x80 != 0) {
                                // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                                count = count and 0x07f
                                x += count
                                b = dis.readByte()
                                g = dis.readByte()
                                r = dis.readByte()
                                a = dis.readByte()
                                while (count-- >= 0) {
                                    rawData[rawDataIndex++] = b
                                    rawData[rawDataIndex++] = g
                                    rawData[rawDataIndex++] = r
                                    rawData[rawDataIndex++] = a
                                }
                            } else {
                                // It's not RLE packed, but the next <count> pixels are raw.
                                x += count
                                while (count-- >= 0) {
                                    b = dis.readByte()
                                    g = dis.readByte()
                                    r = dis.readByte()
                                    a = dis.readByte()
                                    rawData[rawDataIndex++] = b
                                    rawData[rawDataIndex++] = g
                                    rawData[rawDataIndex++] = r
                                    rawData[rawDataIndex++] = a
                                }
                            }
                            ++x
                        }
                    }
                    format = 4
                }
                24 -> {
                    for (y in 0 until height) {
                        rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                        var x = 0
                        while (x < width) {

                            // Get the number of pixels the next chunk covers (either packed or unpacked)
                            var count = dis.readByte().toInt()
                            if (count and 0x80 != 0) {
                                // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                                count = count and 0x07f
                                x += count
                                r = dis.readByte()
                                g = dis.readByte()
                                b = dis.readByte()
                                while (count-- >= 0) {
                                    rawData[rawDataIndex++] = b
                                    rawData[rawDataIndex++] = g
                                    rawData[rawDataIndex++] = r
                                }
                            } else {
                                // It's not RLE packed, but the next <count> pixels are raw.
                                x += count
                                while (count-- >= 0) {
                                    r = dis.readByte()
                                    g = dis.readByte()
                                    b = dis.readByte()
                                    rawData[rawDataIndex++] = b
                                    rawData[rawDataIndex++] = g
                                    rawData[rawDataIndex++] = r
                                }
                            }
                            ++x
                        }
                    }
                    format = 3
                }
                16 -> {
                    val data = ByteArray(2)
                    val scalar = 255f / 31f
                    for (y in 0 until height) {
                        rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                        var x = 0
                        while (x < width) {

                            // Get the number of pixels the next chunk covers (either packed or unpacked)
                            var count = dis.readByte().toInt()
                            if (count and 0x80 != 0) {
                                // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                                count = count and 0x07f
                                x += count
                                data[1] = dis.readByte()
                                data[0] = dis.readByte()
                                r = (getBitsAsByte(data, 1, 5) * scalar).toInt().toByte()
                                g = (getBitsAsByte(data, 6, 5) * scalar).toInt().toByte()
                                b = (getBitsAsByte(data, 11, 5) * scalar).toInt().toByte()
                                while (count-- >= 0) {
                                    rawData[rawDataIndex++] = b
                                    rawData[rawDataIndex++] = g
                                    rawData[rawDataIndex++] = r
                                }
                            } else {
                                // It's not RLE packed, but the next <count> pixels are raw.
                                x += count
                                while (count-- >= 0) {
                                    data[1] = dis.readByte()
                                    data[0] = dis.readByte()
                                    r = (getBitsAsByte(data, 1, 5) * scalar).toInt().toByte()
                                    g = (getBitsAsByte(data, 6, 5) * scalar).toInt().toByte()
                                    b = (getBitsAsByte(data, 11, 5) * scalar).toInt().toByte()
                                    rawData[rawDataIndex++] = b
                                    rawData[rawDataIndex++] = g
                                    rawData[rawDataIndex++] = r
                                }
                            }
                            x++
                        }
                    }
                    format = 3
                }
                else -> throw IOException("Unsupported TGA true color depth: $pixelDepth")
            }
            return format
        }

        private fun getBitsAsByte(data: ByteArray, offset: Int, length: Int): Byte {
            var offsetBytes = offset / 8
            var indexBits = offset % 8
            var rVal = 0

            // start at data[offsetBytes]...  spill into next byte as needed.
            var i = length
            while (--i >= 0) {
                val b = data[offsetBytes]
                val test = if (indexBits == 7) 1 else 2 shl 6 - indexBits
                if (b.toInt() and test != 0) {
                    if (i == 0) {
                        rVal++
                    } else {
                        rVal += 2 shl i - 1
                    }
                }
                indexBits++
                if (indexBits == 8) {
                    indexBits = 0
                    offsetBytes++
                }
            }
            return rVal.toByte()
        }

        /**
         * `flipEndian` is used to flip the endian bit of the header
         * file.
         *
         * @param signedShort the bit to flip.
         * @return the flipped bit.
         */
        private fun flipEndian(signedShort: Short): Short {
            val input: Int = signedShort.toInt() and 0xFFFF
            return (input shl 8 or (input and 0xFF00) ushr 8).toShort()
        }

        private fun abgr(r: Int, g: Int, b: Int, a: Int): Int {
            return b and 255 shl 16 or (g and 255 shl 8) or (r and 255) or (a and 255 shl 24)
        }
    }
}