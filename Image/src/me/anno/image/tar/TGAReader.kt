package me.anno.image.tar

import me.anno.image.raw.ByteImage
import me.anno.io.Streams.readBE16
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readNBytes2
import me.anno.io.Streams.skipN
import me.anno.utils.Color
import me.anno.utils.structures.tuples.IntPair
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * @author Mark Powell
 * @author Joshua Slack - cleaned, commented, added ability to read 16bit true color and color-mapped TGAs.
 * @author Kirill Vainer - ported to JMonkeyEngine3
 * @author Antonio Noack - added black & white support; fixed naming (?), tested with crytek sponza; fixed 32-bit color order(?)
 * at least for my test cases, everything was correct, and the same as Gimp; optimized it a bit; added support for x-flip
 * @version $Id: TGALoader.java 4131 2009-03-19 20:15:28Z blaine.dev $
 *
 * Copyright (c) 2009-2021 jMonkeyEngine
 * blablabla,
 * I am trying to support everything, so I'll be extending it
 */
object TGAReader {

    private const val NO_IMAGE = 0
    private const val COLORMAPPED = 1
    private const val TRUE_COLOR = 2
    private const val GRAYSCALE = 3
    private const val COLORMAPPED_RLE = 9
    private const val TRUE_COLOR_RLE = 10
    private const val GRAYSCALE_RLE = 11

    @JvmStatic
    fun findSize(input: InputStream): IntPair {
        input.skipN(12)
        val width = input.readLE16()
        val height = input.readLE16()
        return IntPair(width, height)
    }

    /**
     * `loadImage` is a manual image loader, which is entirely
     * independent of AWT. OUT: RGB888 or RGBA8888 Image object
     *
     * @param input InputStream of an uncompressed 24b RGB or 32b RGBA TGA
     * @param flip Flip the image vertically
     * @return `Image` object that contains the
     * image, either as a R8, a RGB888 or RGBA8888
     * @throws IOException if an I/O error occurs
     */
    @JvmStatic
    fun read(input: InputStream, flip: Boolean): ByteImage {

        var flipY = flip
        var flipX = false

        // ---------- Start Reading the TGA header ---------- //
        // length of the image id (1 byte)
        val idLength = input.read()

        // Color map (if any) included with the image
        // 0 - no color map data is included
        // 1 - a color map is included
        val colorMapType = input.read()

        // Image type being read:
        val imageType = input.read()

        // Read Color Map Specification (5 bytes)
        // Index of first color map entry (if we want to use it, uncomment and remove extra read.)
        // short cMapStart = flipEndian(dis.readShort());
        input.readLE16()
        // number of entries in the color map
        val cMapLength = input.readLE16()
        // number of bits per color map entry
        val cMapDepth = input.read()

        // Read Image Specification (10 bytes)
        // horizontal coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
        // int xOffset = flipEndian(dis.readShort());
        input.readLE16()
        // vertical coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
        // int yOffset = flipEndian(dis.readShort());
        input.readLE16()
        // width of image - in pixels
        val width = input.readLE16()
        // height of image - in pixels
        val height = input.readLE16()
        // bits per pixel in image.
        val pixelDepth = input.read()
        val imageDescriptor = input.read()
        if (imageDescriptor and 32 != 0) { // bit 5 : if 1, flip top/bottom ordering
            flipY = !flipY
        }
        if (imageDescriptor and 16 != 0) { // bit 4 : if 1, flip left/right ordering
            flipX = true
        }

        // ---------- Done Reading the TGA header ---------- //

        // Skip image ID
        if (idLength > 0) {
            input.skipN(idLength.toLong())
        }

        var cMapEntries: IntArray? = null
        if (colorMapType != 0) {

            // read the color map
            val bytesInColorMap = cMapDepth * cMapLength shr 3
            val bitsPerColor = min(cMapDepth / 3, 8)
            val cMapData = input.readNBytes2(bytesInColorMap, true)

            // Only go to the trouble of constructing the color map
            // table if this is declared a color mapped image.
            if (imageType == COLORMAPPED || imageType == COLORMAPPED_RLE) {
                cMapEntries = IntArray(cMapLength)
                val alphaSize = cMapDepth - 3 * bitsPerColor
                val scalar = (1 shl bitsPerColor) - 1
                val alphaScalar = (1 shl alphaSize) - 1
                if (alphaSize > 0) {
                    for (i in 0 until cMapLength) {
                        val offset = cMapDepth * i
                        val b = (bitsToByte(cMapData, offset, bitsPerColor) * 255 / scalar)
                        val g = (bitsToByte(cMapData, offset + bitsPerColor, bitsPerColor) * 255 / scalar)
                        val r = (bitsToByte(cMapData, offset + 2 * bitsPerColor, bitsPerColor) * 255 / scalar)
                        val a = (bitsToByte(cMapData, offset + 3 * bitsPerColor, alphaSize) * 255 / alphaScalar)
                        cMapEntries[i] = bgra(r, g, b, a)
                    }
                } else {
                    for (i in 0 until cMapLength) {
                        val offset = cMapDepth * i
                        val b = (bitsToByte(cMapData, offset, bitsPerColor) * 255 / scalar)
                        val g = (bitsToByte(cMapData, offset + bitsPerColor, bitsPerColor) * 255 / scalar)
                        val r = (bitsToByte(cMapData, offset + 2 * bitsPerColor, bitsPerColor) * 255 / scalar)
                        cMapEntries[i] = bgr(r, g, b)
                    }
                }
            }
        }

        // Allocate image data array
        val dl = if (pixelDepth == 32) 4 else
            if (imageType == GRAYSCALE || imageType == GRAYSCALE_RLE) 1 else 3
        val rawDataSize = width * height * dl
        if (rawDataSize < 0) throw IOException("Invalid size: $width x $height x $dl")
        val rawData = ByteArray(rawDataSize)
        val numChannels = when (imageType) {
            TRUE_COLOR -> readTrueColor(pixelDepth, width, height, flipY, rawData, dl, input)
            TRUE_COLOR_RLE -> readTrueColorRLE(pixelDepth, width, height, flipY, rawData, dl, input)
            COLORMAPPED -> readColorMapped(pixelDepth, width, height, flipY, rawData, dl, input, cMapEntries!!)
            NO_IMAGE -> throw IOException("No image is not supported")
            GRAYSCALE -> readGrayscale(pixelDepth, width, height, flipY, rawData, dl, input)
            COLORMAPPED_RLE -> throw IOException("Colormapped RLE is not supported")
            GRAYSCALE_RLE -> throw IOException("Black & White RLE is not supported")
            else -> throw IOException("Unknown TGA type $imageType")
        }
        input.close()

        // Create the Image object
        if (flipX) flipX(rawData, width, height, numChannels)
        // image.originalImageType = imageType
        // image.originalPixelDepth = pixelDepth
        val format = when (numChannels) {
            1 -> ByteImage.Format.R
            2 -> ByteImage.Format.RG
            3 -> ByteImage.Format.BGR
            4 -> ByteImage.Format.BGRA
            else -> throw NotImplementedError()
        }
        return ByteImage(width, height, format, rawData)
    }

    @JvmStatic
    private fun flipX1(i0: Int, i1: Int, data: ByteArray, i2: Int) {
        var j = i2
        for (i in i0 until i1) {
            val t = data[i]
            data[i] = data[j]
            data[j] = t
            j--
        }
    }

    @JvmStatic
    private fun flipX2(i0: Int, i1: Int, data: ByteArray, i2: Int) {
        var j = i2
        for (i in i0 until i1 step 2) {
            val t0 = data[i]
            val t1 = data[i + 1]
            data[i] = data[j]
            data[i + 1] = data[j + 1]
            data[j] = t0
            data[j + 1] = t1
            j -= 2
        }
    }

    @JvmStatic
    private fun flipX3(i0: Int, i1: Int, data: ByteArray, i2: Int) {
        var j = i2
        for (i in i0 until i1 step 3) {
            val t0 = data[i]
            val t1 = data[i + 1]
            val t2 = data[i + 2]
            data[i] = data[j]
            data[i + 1] = data[j + 1]
            data[i + 2] = data[j + 2]
            data[j] = t0
            data[j + 1] = t1
            data[j + 2] = t2
            j -= 3
        }
    }

    @JvmStatic
    private fun flipX4(i0: Int, i1: Int, data: ByteArray, i2: Int) {
        var j = i2
        for (i in i0 until i1 step 4) {
            val t0 = data[i]
            val t1 = data[i + 1]
            val t2 = data[i + 2]
            val t3 = data[i + 3]
            data[i] = data[j]
            data[i + 1] = data[j + 1]
            data[i + 2] = data[j + 2]
            data[i + 3] = data[j + 3]
            data[j] = t0
            data[j + 1] = t1
            data[j + 2] = t2
            data[j + 3] = t3
            j -= 4
        }
    }

    @JvmStatic
    private fun flipX(data: ByteArray, width: Int, height: Int, c: Int) {
        val dx = c * (width shr 1)
        for (y in 0 until height) {
            val i0 = y * width * c
            val i1 = i0 + dx
            val i2 = i0 + c * (width - 1)
            when (c) {
                1 -> flipX1(i0, i1, data, i2)
                2 -> flipX2(i0, i1, data, i2)
                3 -> flipX3(i0, i1, data, i2)
                4 -> flipX4(i0, i1, data, i2)
            }
        }
    }

    @JvmStatic
    private fun readColorMapped(
        pixelDepth: Int,
        width: Int, height: Int,
        flip: Boolean, rawData: ByteArray,
        dl: Int, dis: InputStream,
        cMapEntries: IntArray
    ): Int {
        var rawDataIndex = 0
        when (val bytesPerIndex = pixelDepth / 8) {
            1 -> {
                for (i in 0 until height) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl
                    }
                    for (j in 0 until width) {
                        val index = dis.read()
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
                    if (dl == 4) {
                        for (j in 0 until width) {
                            val index = dis.readLE16()
                            val entry = cMapEntries[index]
                            rawData[rawDataIndex++] = (entry shr 16).toByte()
                            rawData[rawDataIndex++] = (entry shr 8).toByte()
                            rawData[rawDataIndex++] = entry.toByte()
                            rawData[rawDataIndex++] = (entry shr 24).toByte()
                        }
                    } else {
                        for (j in 0 until width) {
                            val index = dis.readLE16()
                            val entry = cMapEntries[index]
                            rawData[rawDataIndex++] = (entry shr 16).toByte()
                            rawData[rawDataIndex++] = (entry shr 8).toByte()
                            rawData[rawDataIndex++] = entry.toByte()
                        }
                    }
                }
            }
            else -> throw IOException("TGA: unknown ColorMap indexing size used: $bytesPerIndex")
        }
        return if (dl == 4) 4 else 3
    }

    @JvmStatic
    private fun readTrueColor(
        pixelDepth: Int,
        width: Int, height: Int,
        flip: Boolean, rawData: ByteArray,
        dl: Int, dis: InputStream
    ): Int {
        var rawDataIndex = 0
        return when (pixelDepth) {
            16 -> {
                for (y in 0 until height) {
                    if (!flip) rawDataIndex = (height - 1 - y) * width * dl
                    for (x in 0 until width) {
                        val v = (dis.read() shl 8) + dis.read()
                        rawData[rawDataIndex++] = (((v ushr 1) and 31) * 255 / 31).toByte()
                        rawData[rawDataIndex++] = (((v ushr 6) and 31) * 255 / 31).toByte()
                        rawData[rawDataIndex++] = (((v ushr 11) and 31) * 255 / 31).toByte()
                        if (dl == 4) {
                            // create an alpha channel
                            rawData[rawDataIndex++] = ((v and 1) * 255).toByte()
                        }
                    }
                }
                if (dl == 4) 4 else 3
            }
            24 -> {
                for (y in 0 until height) {
                    rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                    dis.readNBytes2(rawData, rawDataIndex, width * dl)
                }
                3
            }
            32 -> {
                for (y in 0 until height) {
                    rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                    dis.readNBytes2(rawData, rawDataIndex, width * 4)
                }
                4
            }
            else -> throw IOException("Unsupported TGA true color depth: $pixelDepth")
        }
    }

    @JvmStatic
    private fun readGrayscale(
        pixelDepth: Int,
        width: Int, height: Int,
        flip: Boolean, rawData: ByteArray,
        dl: Int, dis: InputStream
    ): Int {
        for (y in 0 until height) {
            var rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
            for (x in 0 until width) {
                val v = dis.read().toByte()
                rawData[rawDataIndex++] = v
                if (pixelDepth >= 16) rawData[rawDataIndex++] = v
                if (pixelDepth >= 24) rawData[rawDataIndex++] = v
                if (pixelDepth >= 32) rawData[rawDataIndex++] = -1
            }
        }
        return pixelDepth / 8
    }

    @JvmStatic
    private fun readTrueColorRLE(
        pixelDepth: Int,
        width: Int, height: Int,
        flip: Boolean, rawData: ByteArray,
        dl: Int, dis: InputStream
    ): Int {

        val format: Int
        var rawDataIndex: Int

        // Faster than doing a 16-or-24-or-32 check on each individual pixel,
        // just make a separate loop for each.
        when (pixelDepth) {
            32 -> {
                for (y in 0 until height) {
                    rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                    var x = 0
                    while (x < width) {

                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        var count = dis.read()
                        if (count and 0x80 != 0) {
                            // It's an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count = count and 0x07f
                            x += count
                            val b = dis.read().toByte()
                            val g = dis.read().toByte()
                            val r = dis.read().toByte()
                            val a = dis.read().toByte()
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
                                val b = dis.read().toByte()
                                val g = dis.read().toByte()
                                val r = dis.read().toByte()
                                val a = dis.read().toByte()
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
                        var count = dis.read()
                        if (count >= 0x80) {
                            // It's an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count -= 0x80
                            x += count
                            val r = dis.read().toByte()
                            val g = dis.read().toByte()
                            val b = dis.read().toByte()
                            while (count-- >= 0) {
                                rawData[rawDataIndex++] = b
                                rawData[rawDataIndex++] = g
                                rawData[rawDataIndex++] = r
                            }
                        } else {
                            // It's not RLE packed, but the next <count> pixels are raw.
                            x += count
                            while (count-- >= 0) {
                                val r = dis.read().toByte()
                                val g = dis.read().toByte()
                                val b = dis.read().toByte()
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
                for (y in 0 until height) {
                    rawDataIndex = (if (flip) y else height - 1 - y) * width * dl
                    var x = 0
                    while (x < width) {
                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        var count = dis.read()
                        if (count >= 0x80) {
                            // It's an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count -= 0x80
                            x += count
                            val v = dis.readBE16()
                            val r = (((v ushr 1) and 31) * 255 / 31).toByte()
                            val g = (((v ushr 6) and 31) * 255 / 31).toByte()
                            val b = (((v ushr 11) and 31) * 255 / 31).toByte()
                            while (count-- >= 0) {
                                rawData[rawDataIndex++] = b
                                rawData[rawDataIndex++] = g
                                rawData[rawDataIndex++] = r
                            }
                        } else {
                            // It's not RLE packed, but the next <count> pixels are raw.
                            x += count
                            while (count-- >= 0) {
                                val v = dis.readBE16()
                                val r = (((v ushr 1) and 31) * 255 / 31).toByte()
                                val g = (((v ushr 6) and 31) * 255 / 31).toByte()
                                val b = (((v ushr 11) and 31) * 255 / 31).toByte()
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

    @JvmStatic
    private fun bitsToByte(data: ByteArray, offset: Int, length: Int): Int {
        var offsetBytes = offset shr 3
        var indexBits = offset and 7
        var rVal = 0
        // start at data[offsetBytes]...  spill into next byte as needed.
        for (i in length - 1 downTo 0) {
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
        return rVal
    }

    @JvmStatic
    private fun bgra(b: Int, g: Int, r: Int, a: Int): Int {
        return (r and 255 shl 16) or (g and 255 shl 8) or (b and 255) or (a shl 24)
    }

    @JvmStatic
    private fun bgr(b: Int, g: Int, r: Int): Int {
        return (r shl 16) or (g and 255 shl 8) or (b and 255) or Color.black
    }
}