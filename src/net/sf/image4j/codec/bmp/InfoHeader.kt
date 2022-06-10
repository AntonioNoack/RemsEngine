/*
 * InfoHeader.java
 *
 * Created on 10 May 2006, 08:10
 *
 */
package net.sf.image4j.codec.bmp

import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import net.sf.image4j.io.CountingInputStream

/**
 * Represents a bitmap InfoHeader structure, which provides header information.
 * @author Ian McDonagh
 */
class InfoHeader {

    /**
     * The size of this InfoHeader structure in bytes.
     */
    var size = 0

    /**
     * The width in pixels of the bitmap represented by this InfoHeader.
     */
    var width = 0

    /**
     * The height in pixels of the bitmap represented by this InfoHeader.
     */
    var height = 0

    /**
     * The number of planes, which should always be 1.
     */
    var planes: Short = 0

    /**
     * The bit count, which represents the colour depth (bits per pixel).
     * This should be either 1, 4, 8, 24 or 32.
     */
    var bitCount = 0

    /**
     * The compression type, which should be one of the following:
     *
     *  * [BI_RGB][BMPDecoder.BI_RGB] - no compression
     *  * [BI_RLE8][BMPDecoder.BI_RLE8] - 8-bit RLE compression
     *  * [BI_RLE4][BMPDecoder.BI_RLE4] - 4-bit RLE compression
     *
     */
    var compression = 0

    /**
     * The compressed size of the image in bytes, or 0 if compression is 0.
     */
    var imageSize = 0

    var pixelsPerMeterX = 0
    var pixelsPerMeterY = 0

    /**
     * Number of colours actually used in the bitmap.
     */
    var numUsedColors = 0

    /**
     * Number of important colours (0 = all).
     */
    var numImportantColors = 0

    /**
     * Calculated number of colours, based on the colour depth specified by [sBitCount][.sBitCount].
     */
    var numColors = 0

    constructor(input: CountingInputStream) {
        // Size of InfoHeader structure = 40
        size = input.readLE32()
        init(input, size)
    }

    constructor(input: CountingInputStream, infoSize: Int) {
        init(input, infoSize)
    }

    fun init(input: CountingInputStream, infoSize: Int) {
        size = infoSize
        width = input.readLE32()
        height = input.readLE32()
        planes = input.readLE16().toShort()
        bitCount = input.readLE16()
        numColors = 1 shl bitCount
        compression = input.readLE32()
        imageSize = input.readLE32()
        pixelsPerMeterX = input.readLE32()
        pixelsPerMeterY = input.readLE32()
        numUsedColors = input.readLE32()
        numImportantColors = input.readLE32()
    }

    /**
     * Creates a copy of the source InfoHeader.
     * @param source the source to copy
     */
    constructor(source: InfoHeader) {
        numImportantColors = source.numImportantColors
        numUsedColors = source.numUsedColors
        compression = source.compression
        height = source.height
        width = source.width
        imageSize = source.imageSize
        numColors = source.numColors
        size = source.size
        pixelsPerMeterX = source.pixelsPerMeterX
        pixelsPerMeterY = source.pixelsPerMeterY
        bitCount = source.bitCount
        planes = source.planes
    }
}