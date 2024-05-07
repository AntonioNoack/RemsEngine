package net.sf.image4j.codec.bmp

import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.skipN
import me.anno.utils.structures.CountingInputStream

/**
 * Represents a bitmap InfoHeader structure, which provides header information.
 * @author Ian McDonagh
 */
class InfoHeader {

    var width = 0
    var height = 0

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
     * Calculated number of colours, based on the colour depth specified by [sBitCount][.sBitCount].
     */
    val numColors get() = 1 shl bitCount

    constructor(input: CountingInputStream) {
        width = input.readLE32()
        height = input.readLE32()
        /*planes = */input.readLE16()
        bitCount = input.readLE16()
        compression = input.readLE32()
        input.skipN(20)
        // imageSize = input.readLE32()
        // pixelsPerMeterX = input.readLE32()
        // pixelsPerMeterY = input.readLE32()
        // numUsedColors = input.readLE32()
        // numImportantColors = input.readLE32()
    }

    /**
     * Creates a copy of the source InfoHeader.
     * @param source the source to copy
     */
    constructor(source: InfoHeader) {
        compression = source.compression
        height = source.height
        width = source.width
        bitCount = source.bitCount
    }
}