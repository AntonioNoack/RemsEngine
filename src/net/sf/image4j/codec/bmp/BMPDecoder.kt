package net.sf.image4j.codec.bmp

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.io.Streams.readLE32
import net.sf.image4j.io.CountingInputStream
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
         * no compression
         * */
        const val BI_RGB = 0
        const val BI_RLE8 = 1
        const val BI_RLE4 = 2

        /**
         * Retrieves a bit from the lowest order byte of the given integer.
         *
         * @param bits  the source integer, treated as an unsigned byte
         * @param index the index of the bit to retrieve, which must be in the range <tt>0..7</tt>.
         * @return the bit at the specified index, which will be either <tt>0</tt> or <tt>1</tt>.
         */
        @JvmStatic
        private fun getBit(bits: Int, index: Int): Int {
            return (bits shr (7 - index)) and 1
        }

        /**
         * Retrieves a nibble (4 bits) from the lowest order byte of the given integer.
         *
         * @param nibbles the source integer, treated as an unsigned byte
         * @param index   the index of the nibble to retrieve, which must be in the range <tt>0..1</tt>.
         * @return the nibble at the specified index, as an unsigned byte.
         */
        @JvmStatic
        private fun getNibble(nibbles: Int, index: Int): Int {
            return nibbles shr 4 * (1 - index) and 0xf
        }

        @JvmStatic
        fun readInfoHeader(lis: CountingInputStream) =
            InfoHeader(lis)

        @JvmStatic
        fun readInfoHeader(lis: CountingInputStream, infoSize: Int) =
            InfoHeader(lis, infoSize)

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
        @JvmStatic
        fun read(infoHeader: InfoHeader, lis: CountingInputStream): IntImage {
            // Color table (palette)
            var colorTable: IntArray? = null
            // color table is only present for 1, 4 or 8 bit (indexed) images
            if (infoHeader.bitCount <= 8) {
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
        @JvmStatic
        fun read(infoHeader: InfoHeader, lis: CountingInputStream, colorTable: IntArray?): IntImage {
            val image = if (infoHeader.compression == BI_RGB) {
                when (infoHeader.bitCount) {
                    1 -> read1(infoHeader, lis, colorTable!!) // 1-bit (monochrome) uncompressed
                    4 -> read4(infoHeader, lis, colorTable!!) // 4-bit uncompressed
                    8 -> read8(infoHeader, lis, colorTable!!) // 8-bit uncompressed
                    24 -> read24(infoHeader, lis) // 24-bit uncompressed
                    32 -> read32(infoHeader, lis) // 32bit uncompressed
                    else -> null
                }
            } else null
            return image
                ?: throw IOException("Unrecognized bitmap format: bit count=${infoHeader.bitCount}, compression=${infoHeader.compression}")
        }

        @JvmStatic
        fun readColorTable(infoHeader: InfoHeader, lis: CountingInputStream) =
            IntArray(infoHeader.numColors) { lis.readLE32() }

        /**
         * Reads 1-bit uncompressed bitmap raster data, which may be monochrome depending on the
         * palette entries in <tt>colorTable</tt>.
         */
        @JvmStatic
        fun read1(infoHeader: InfoHeader, lis: CountingInputStream, colors: IntArray): IntImage {
            //1 bit per pixel or 8 pixels per byte
            //each pixel specifies the palette index

            // padding
            var bitsPerLine = infoHeader.width
            if (bitsPerLine % 32 != 0) {
                bitsPerLine = (bitsPerLine / 32 + 1) * 32
            }
            val bytesPerLine = bitsPerLine / 8
            val line = IntArray(bytesPerLine)
            val data = IntArray(infoHeader.width * infoHeader.height)
            for (y in infoHeader.height - 1 downTo 0) {
                for (i in 0 until bytesPerLine) {
                    line[i] = lis.read()
                }
                var ctr = y * infoHeader.width
                for (x in 0 until infoHeader.width) {
                    val i = x shr 3
                    val v = line[i]
                    val b = x and 7
                    val index = getBit(v, b)
                    // img.setRGB(x, y, rgb);
                    // set the sample (colour index) for the pixel
                    data[ctr++] = colors[index]
                }
            }
            return IntImage(infoHeader.width, infoHeader.height, data, false)
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
        @JvmStatic
        fun read4(infoHeader: InfoHeader, lis: CountingInputStream, colors: IntArray): IntImage {

            // 2 pixels per byte or 4 bits per pixel.
            // Colour for each pixel specified by the color index in the palette.

            //padding
            var bitsPerLine = infoHeader.width * 4
            if (bitsPerLine % 32 != 0) {
                bitsPerLine = (bitsPerLine / 32 + 1) * 32
            }
            val bytesPerLine = bitsPerLine / 8
            val line = IntArray(bytesPerLine)
            val data = IntArray(infoHeader.width * infoHeader.height)
            for (y in infoHeader.height - 1 downTo 0) {

                // scan line
                for (i in 0 until bytesPerLine) {
                    line[i] = lis.read()
                }

                // get pixels
                var ctr = y * infoHeader.width
                for (x in 0 until infoHeader.width) {
                    //get byte index for line
                    val b = x shr 1 // 2 pixels per byte
                    val i = x and 1
                    val n = line[b]
                    val index = getNibble(n, i)
                    data[ctr++] = colors[index]
                }
            }
            return IntImage(infoHeader.width, infoHeader.height, data, false)
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
        @JvmStatic
        fun read8(infoHeader: InfoHeader, lis: CountingInputStream, colors: IntArray): IntImage {

            // 1 byte per pixel
            //  color index 1 (index of color in palette)
            // lines padded to nearest 32bits
            // no alpha

            //padding
            val dataPerLine = infoHeader.width
            var bytesPerLine = dataPerLine
            if (bytesPerLine % 4 != 0) {
                bytesPerLine = (bytesPerLine / 4 + 1) * 4
            }
            val padBytesPerLine = bytesPerLine - dataPerLine
            val width = infoHeader.width
            val data = IntArray(width * infoHeader.height)
            for (y in infoHeader.height - 1 downTo 0) {
                var ctr = y * width
                for (x in 0 until width) {
                    data[ctr++] = colors[lis.read()]
                }
                Utils.skip(lis, padBytesPerLine, false)
            }
            return IntImage(infoHeader.width, infoHeader.height, data, false)
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
        @JvmStatic
        fun read24(infoHeader: InfoHeader, lis: CountingInputStream): IntImage {

            // 3 bytes per pixel
            //  blue 1
            //  green 1
            //  red 1
            // lines padded to nearest 32 bits
            // no alpha

            // padding to nearest 32 bits
            val dataPerLine = infoHeader.width * 3
            var bytesPerLine = dataPerLine
            if (bytesPerLine % 4 != 0) {
                bytesPerLine = (bytesPerLine / 4 + 1) * 4
            }
            val padBytesPerLine = bytesPerLine - dataPerLine
            val width = infoHeader.width
            val data = IntArray(width * infoHeader.height)
            for (y in infoHeader.height - 1 downTo 0) {
                var ctr = y * width
                for (x in 0 until width) {
                    val b = lis.read()
                    val g = lis.read()
                    val r = lis.read()
                    data[ctr++] = r shl 16 or (g shl 8) or b
                }
                Utils.skip(lis, padBytesPerLine, false)
            }
            return IntImage(infoHeader.width, infoHeader.height, data, false)
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
        @JvmStatic
        fun read32(infoHeader: InfoHeader, lis: CountingInputStream): IntImage {
            // 4 bytes per pixel
            // blue 1
            // green 1
            // red 1
            // alpha 1
            // No padding since each pixel = 32 bits
            val data = IntArray(infoHeader.width * infoHeader.height)
            for (y in infoHeader.height - 1 downTo 0) {
                var ctr = y * infoHeader.width
                for (x in 0 until infoHeader.width) {
                    data[ctr++] = lis.readLE32() // bgra
                }
            }
            return IntImage(infoHeader.width, infoHeader.height, data, true)
        }

        /**
         * Reads and decodes BMP data from the source input.
         *
         * @param `in` the source input
         * @return the decoded image read from the source file
         * @throws IOException if an error occurs
         */
        @JvmStatic
        fun read(input: InputStream) = BMPDecoder(input).image
    }

    /**
     * The decoded image read from the source input.
     *
     * @return the <tt>Image</tt> representing the BMP image.
     */
    val image: Image

    init {

        val lis = CountingInputStream(CountingInputStream(input))

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
        lis.readLE32()

        // reserved = 0 [4]
        lis.readLE32()

        //DataOffset [4] file offset to raster data
        lis.readLE32()

        /* info header [40] */
        val infoHeader = readInfoHeader(lis)

        /* Color table and Raster data */
        image = read(infoHeader, lis)

    }
}