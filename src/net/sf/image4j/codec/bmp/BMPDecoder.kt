/*
 * Decodes a BMP image from an <tt>InputStream</tt> to a <tt>BufferedImage</tt>
 *
 * @author Ian McDonagh
 */
package net.sf.image4j.codec.bmp

import me.anno.image.Image
import me.anno.image.raw.IntImage
import net.sf.image4j.io.CountingInputStream
import net.sf.image4j.io.LittleEndianInputStream
import net.sf.image4j.io.Utils
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Decodes images in BMP format.
 * Creates a new instance of BMPDecoder and reads the BMP data from the source.
 *
 * @param input the source <tt>InputStream</tt> from which to read the BMP data
 * @throws IOException if an error occurs
 * @author Ian McDonagh
 */
class BMPDecoder(input: InputStream) {

    companion object {

        /**
         * Specifies no compression.
         *
         * @see InfoHeader.iCompression InfoHeader
         */
        const val BI_RGB = 0 // no compression

        /**
         * Specifies 8-bit RLE compression.
         *
         * @see InfoHeader.iCompression InfoHeader
         */
        const val BI_RLE8 = 1 // 8bit RLE compression

        /**
         * Specifies 4-bit RLE compression.
         *
         * @see InfoHeader.iCompression InfoHeader
         */
        const val BI_RLE4 = 2 // 4bit RLE compression

        /**
         * Retrieves a bit from the lowest order byte of the given integer.
         *
         * @param bits  the source integer, treated as an unsigned byte
         * @param index the index of the bit to retrieve, which must be in the range <tt>0..7</tt>.
         * @return the bit at the specified index, which will be either <tt>0</tt> or <tt>1</tt>.
         */
        private fun getBit(bits: Int, index: Int): Int {
            return bits shr 7 - index and 1
        }

        /**
         * Retrieves a nibble (4 bits) from the lowest order byte of the given integer.
         *
         * @param nibbles the source integer, treated as an unsigned byte
         * @param index   the index of the nibble to retrieve, which must be in the range <tt>0..1</tt>.
         * @return the nibble at the specified index, as an unsigned byte.
         */
        private fun getNibble(nibbles: Int, index: Int): Int {
            return nibbles shr 4 * (1 - index) and 0xF
        }

        /**
         * Reads the BMP info header structure from the given <tt>InputStream</tt>.
         *
         * @param lis the <tt>InputStream</tt> to read
         * @return the <tt>InfoHeader</tt> structure
         * @throws java.io.IOException if an error occurred
         */
        @Throws(IOException::class)
        fun readInfoHeader(lis: LittleEndianInputStream?): InfoHeader {
            return InfoHeader(lis!!)
        }

        /**
         * @since 0.6
         */
        @Throws(IOException::class)
        fun readInfoHeader(lis: LittleEndianInputStream?, infoSize: Int): InfoHeader {
            return InfoHeader(lis!!, infoSize)
        }

        /**
         * Reads the BMP data from the given <tt>InputStream</tt> using the information
         * contained in the <tt>InfoHeader</tt>.
         *
         * @param lis        the source input
         * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
         * [readInfoHeader()][.readInfoHeader].
         * @return the decoded image read from the source input
         * @throws java.io.IOException if an error occurs
         */
        @Throws(IOException::class)
        fun read(infoHeader: InfoHeader, lis: LittleEndianInputStream): IntImage {

            /* Color table (palette) */
            var colorTable: IntArray? = null
            // color table is only present for 1, 4 or 8 bit (indexed) images
            if (infoHeader.sBitCount <= 8) {
                colorTable = readColorTable(infoHeader, lis)
            }
            return read(infoHeader, lis, colorTable)
        }

        /**
         * Reads the BMP data from the given <tt>InputStream</tt> using the information
         * contained in the <tt>InfoHeader</tt>.
         *
         * @param colorTable <tt>ColorEntry</tt> array containing palette
         * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
         * [readInfoHeader()][.readInfoHeader].
         * @param lis        the source input
         * @return the decoded image read from the source input
         * @throws java.io.IOException if any error occurs
         */
        @Throws(IOException::class)
        fun read(infoHeader: InfoHeader, lis: LittleEndianInputStream, colorTable: IntArray?): IntImage {
            if (infoHeader.iCompression == BI_RGB) {
                when (infoHeader.sBitCount) {
                    1 -> read1(infoHeader, lis, colorTable) // 1-bit (monochrome) uncompressed
                    4 -> read4(infoHeader, lis, colorTable) // 4-bit uncompressed
                    8 -> read8(infoHeader, lis, colorTable) // 8-bit uncompressed
                    24 -> read24(infoHeader, lis) // 24-bit uncompressed
                    32 -> read32(infoHeader, lis) // 32bit uncompressed
                }
            }
            throw IOException("Unrecognized bitmap format: bit count=${infoHeader.sBitCount}, compression=${infoHeader.iCompression}")
        }

        /**
         * Reads the <tt>ColorEntry</tt> table from the given <tt>InputStream</tt> using
         * the information contained in the given <tt>infoHeader</tt>.
         *
         * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
         * [readInfoHeader()][.readInfoHeader]
         * @param lis        the <tt>InputStream</tt> to read
         * @return the decoded image read from the source input
         * @throws IOException if an error occurs
         */
        @Throws(IOException::class)
        fun readColorTable(infoHeader: InfoHeader, lis: LittleEndianInputStream): IntArray {
            return IntArray(infoHeader.iNumColors) { lis.readIntLE() }
        }

        /**
         * Reads 1-bit uncompressed bitmap raster data, which may be monochrome depending on the
         * palette entries in <tt>colorTable</tt>.
         *
         * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
         * [readInfoHeader()][.readInfoHeader]
         * @param lis        the source input
         * @param colors     int[] specifying the palette, which
         * must not be <tt>null</tt>.
         * @return the decoded image read from the source input
         * @throws IOException if an error occurs
         */
        @Throws(IOException::class)
        fun read1(infoHeader: InfoHeader, lis: LittleEndianInputStream, colors: IntArray?): IntImage {
            //1 bit per pixel or 8 pixels per byte
            //each pixel specifies the palette index

            // padding
            var bitsPerLine = infoHeader.iWidth
            if (bitsPerLine % 32 != 0) {
                bitsPerLine = (bitsPerLine / 32 + 1) * 32
            }
            val bytesPerLine = bitsPerLine / 8
            val line = IntArray(bytesPerLine)
            val data = IntArray(infoHeader.iWidth * infoHeader.iHeight)
            for (y in infoHeader.iHeight - 1 downTo 0) {
                for (i in 0 until bytesPerLine) {
                    line[i] = lis.readUnsignedByte()
                }
                var ctr = y * infoHeader.iWidth
                for (x in 0 until infoHeader.iWidth) {
                    val i = x shr 3
                    val v = line[i]
                    val b = x and 7
                    val index = getBit(v, b)
                    // img.setRGB(x, y, rgb);
                    // set the sample (colour index) for the pixel
                    data[ctr++] = colors!![index]
                }
            }
            return IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false)
        }

        /**
         * Reads 4-bit uncompressed bitmap raster data, which is interpreted based on the colours
         * specified in the palette.
         *
         * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
         * [readInfoHeader()][.readInfoHeader]
         * @param lis        the source input
         * @param colors     int[] specifying the palette, which
         * must not be <tt>null</tt>.
         * @return the decoded image read from the source input
         * @throws IOException if an error occurs
         */
        @Throws(IOException::class)
        fun read4(infoHeader: InfoHeader, lis: LittleEndianInputStream, colors: IntArray?): IntImage {

            // 2 pixels per byte or 4 bits per pixel.
            // Colour for each pixel specified by the color index in the palette.

            //padding
            var bitsPerLine = infoHeader.iWidth * 4
            if (bitsPerLine % 32 != 0) {
                bitsPerLine = (bitsPerLine / 32 + 1) * 32
            }
            val bytesPerLine = bitsPerLine / 8
            val line = IntArray(bytesPerLine)
            val data = IntArray(infoHeader.iWidth * infoHeader.iHeight)
            for (y in infoHeader.iHeight - 1 downTo 0) {

                // scan line
                for (i in 0 until bytesPerLine) {
                    val b = lis.readUnsignedByte()
                    line[i] = b
                }

                // get pixels
                var ctr = y * infoHeader.iWidth
                for (x in 0 until infoHeader.iWidth) {
                    //get byte index for line
                    val b = x shr 1 // 2 pixels per byte
                    val i = x and 1
                    val n = line[b]
                    val index = getNibble(n, i)
                    data[ctr++] = colors!![index]
                }
            }
            return IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false)
        }

        /**
         * Reads 8-bit uncompressed bitmap raster data, which is interpreted based on the colours
         * specified in the palette.
         *
         * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
         * [readInfoHeader()][.readInfoHeader]
         * @param lis        the source input
         * @param colors     int[] specifying the palette, which
         * must not be <tt>null</tt>.
         * @return the decoded image read from the source input
         * @throws IOException if an error occurs
         */
        @Throws(IOException::class)
        fun read8(infoHeader: InfoHeader, lis: LittleEndianInputStream, colors: IntArray?): IntImage {

            // 1 byte per pixel
            //  color index 1 (index of color in palette)
            // lines padded to nearest 32bits
            // no alpha

            //padding
            val dataPerLine = infoHeader.iWidth
            var bytesPerLine = dataPerLine
            if (bytesPerLine % 4 != 0) {
                bytesPerLine = (bytesPerLine / 4 + 1) * 4
            }
            val padBytesPerLine = bytesPerLine - dataPerLine
            val data = IntArray(infoHeader.iWidth * infoHeader.iHeight)
            for (y in infoHeader.iHeight - 1 downTo 0) {
                var ctr = y * infoHeader.iWidth
                for (x in 0 until infoHeader.iWidth) {
                    val b = lis.readUnsignedByte()
                    data[ctr++] = colors!![b]
                }
                Utils.skip(lis, padBytesPerLine, false)
            }
            return IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false)
        }

        /**
         * Reads 24-bit uncompressed bitmap raster data.
         *
         * @param lis        the source input
         * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
         * [readInfoHeader()][.readInfoHeader]
         * @return the decoded image read from the source input
         * @throws IOException if an error occurs
         */
        @Throws(IOException::class)
        fun read24(infoHeader: InfoHeader, lis: LittleEndianInputStream): IntImage {

            // 3 bytes per pixel
            //  blue 1
            //  green 1
            //  red 1
            // lines padded to nearest 32 bits
            // no alpha

            // padding to nearest 32 bits
            val dataPerLine = infoHeader.iWidth * 3
            var bytesPerLine = dataPerLine
            if (bytesPerLine % 4 != 0) {
                bytesPerLine = (bytesPerLine / 4 + 1) * 4
            }
            val padBytesPerLine = bytesPerLine - dataPerLine
            val data = IntArray(infoHeader.iHeight * infoHeader.iWidth)
            for (y in infoHeader.iHeight - 1 downTo 0) {
                var ctr = y * infoHeader.iWidth
                for (x in 0 until infoHeader.iWidth) {
                    val b = lis.readUnsignedByte()
                    val g = lis.readUnsignedByte()
                    val r = lis.readUnsignedByte()
                    data[ctr++] = r shl 16 or (g shl 8) or b
                }
                Utils.skip(lis, padBytesPerLine, false)
            }
            return IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false)
        }

        /**
         * Reads 32-bit uncompressed bitmap raster data, with transparency.
         *
         * @param lis        the source input
         * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
         * [readInfoHeader()][.readInfoHeader]
         * @return the decoded image read from the source input
         * @throws IOException if an error occurs
         */
        @Throws(IOException::class)
        fun read32(infoHeader: InfoHeader, lis: LittleEndianInputStream): IntImage {
            // 4 bytes per pixel
            // blue 1
            // green 1
            // red 1
            // alpha 1
            // No padding since each pixel = 32 bits
            val data = IntArray(infoHeader.iWidth * infoHeader.iHeight)
            for (y in infoHeader.iHeight - 1 downTo 0) {
                var ctr = y * infoHeader.iWidth
                for (x in 0 until infoHeader.iWidth) {
                    data[ctr++] = lis.readIntLE() // bgra
                }
            }
            return IntImage(infoHeader.iWidth, infoHeader.iHeight, data, true)
        }

        /**
         * Reads and decodes BMP data from the source input.
         *
         * @param `in` the source input
         * @return the decoded image read from the source file
         * @throws IOException if an error occurs
         */
        @Throws(IOException::class)
        fun read(input: InputStream) = BMPDecoder(input).image
    }

    /**
     * The decoded image read from the source input.
     *
     * @return the <tt>BufferedImage</tt> representing the BMP image.
     */
    val image: Image

    init {

        val lis = LittleEndianInputStream(CountingInputStream(input))

        /* header [14] */

        // signature "BM" [2]
        val s0 = lis.read()
        val s1 = lis.read()
        if (s0 < 0 || s1 < 0) throw EOFException()
        if (s0 != 'B'.code || s1 != 'M'.code) {
            val signature = String(byteArrayOf(s0.toByte(), s1.toByte()), StandardCharsets.UTF_8)
            throw IOException("Invalid signature '$signature' for BMP format")
        }

        // file size [4]
        lis.readIntLE()

        // reserved = 0 [4]
        lis.readIntLE()

        //DataOffset [4] file offset to raster data
        lis.readIntLE()

        /* info header [40] */
        val infoHeader = readInfoHeader(lis)

        /* Color table and Raster data */
        image = read(infoHeader, lis)

    }
}