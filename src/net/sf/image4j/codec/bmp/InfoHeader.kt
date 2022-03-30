/*
 * InfoHeader.java
 *
 * Created on 10 May 2006, 08:10
 *
 */
package net.sf.image4j.codec.bmp

import net.sf.image4j.io.LittleEndianInputStream
import java.io.IOException

/**
 * Represents a bitmap <tt>InfoHeader</tt> structure, which provides header information.
 * @author Ian McDonagh
 */
class InfoHeader {

    /**
     * The size of this <tt>InfoHeader</tt> structure in bytes.
     */
    @JvmField
    var iSize = 0

    /**
     * The width in pixels of the bitmap represented by this <tt>InfoHeader</tt>.
     */
    @JvmField
    var iWidth = 0

    /**
     * The height in pixels of the bitmap represented by this <tt>InfoHeader</tt>.
     */
    @JvmField
    var iHeight = 0

    /**
     * The number of planes, which should always be <tt>1</tt>.
     */
    var sPlanes: Short = 0

    /**
     * The bit count, which represents the colour depth (bits per pixel).
     * This should be either <tt>1</tt>, <tt>4</tt>, <tt>8</tt>, <tt>24</tt> or <tt>32</tt>.
     */
    @JvmField
    var sBitCount = 0

    /**
     * The compression type, which should be one of the following:
     *
     *  * [BI_RGB][BMPDecoder.BI_RGB] - no compression
     *  * [BI_RLE8][BMPDecoder.BI_RLE8] - 8-bit RLE compression
     *  * [BI_RLE4][BMPDecoder.BI_RLE4] - 4-bit RLE compression
     *
     */
    @JvmField
    var iCompression = 0

    /**
     * The compressed size of the image in bytes, or <tt>0</tt> if <tt>iCompression</tt> is <tt>0</tt>.
     */
    var iImageSize = 0

    /**
     * Horizontal resolution in pixels/m.
     */
    var iXpixelsPerM = 0

    /**
     * Vertical resolution in pixels/m.
     */
    var iYpixelsPerM = 0

    /**
     * Number of colours actually used in the bitmap.
     */
    var iColorsUsed = 0

    /**
     * Number of important colours (<tt>0</tt> = all).
     */
    var iColorsImportant = 0

    /**
     * Calculated number of colours, based on the colour depth specified by [sBitCount][.sBitCount].
     */
    @JvmField
    var iNumColors = 0

    /**
     * Creates an <tt>InfoHeader</tt> structure from the source input.
     * @param `in` the source input
     * @throws java.io.IOException if an error occurs
     */
    constructor(input: LittleEndianInputStream) {
        // Size of InfoHeader structure = 40
        iSize = input.readIntLE()
        init(input, iSize)
    }

    /**
     * @since 0.6
     */
    constructor(input: LittleEndianInputStream, infoSize: Int) {
        init(input, infoSize)
    }

    /**
     * @since 0.6
     */
    @Throws(IOException::class)
    fun init(input: LittleEndianInputStream, infoSize: Int) {
        iSize = infoSize

        iWidth = input.readIntLE()

        iHeight = input.readIntLE()

        // typically 1
        sPlanes = input.readShortLE()

        sBitCount = input.readShortLE().toInt()

        iNumColors = 1 shl sBitCount

        iCompression = input.readIntLE()

        // compressed size of image or 0 if Compression = 0
        iImageSize = input.readIntLE()

        // horizontal resolution pixels/meter
        iXpixelsPerM = input.readIntLE()

        // vertical resolution pixels/meter
        iYpixelsPerM = input.readIntLE()

        // number of colors actually used
        iColorsUsed = input.readIntLE()

        // number of important colors 0 = all
        iColorsImportant = input.readIntLE()
    }

    /**
     * Creates a copy of the source <tt>InfoHeader</tt>.
     * @param source the source to copy
     */
    constructor(source: InfoHeader) {
        iColorsImportant = source.iColorsImportant
        iColorsUsed = source.iColorsUsed
        iCompression = source.iCompression
        iHeight = source.iHeight
        iWidth = source.iWidth
        iImageSize = source.iImageSize
        iNumColors = source.iNumColors
        iSize = source.iSize
        iXpixelsPerM = source.iXpixelsPerM
        iYpixelsPerM = source.iYpixelsPerM
        sBitCount = source.sBitCount
        sPlanes = source.sPlanes
    }
}