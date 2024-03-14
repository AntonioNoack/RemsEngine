package me.anno.image.hdr

import me.anno.image.raw.FloatImage
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * src/author: https://github.com/aicp7/HDR_file_readin
 *
 * modified for our needs
 * This class is used to convert an HDR format image into a three-dimensional float array representing the RGB channels of the original image.
 * */
object HDRReader {

    fun read(input: InputStream): FloatImage {
        return optimizeStream(input).use(HDRReader::read1)
    }

    private fun optimizeStream(input: InputStream): InputStream {
        return if (input is BufferedInputStream ||
            input is ByteArrayInputStream
        ) input else BufferedInputStream(input)
    }

    // Construction method if the input is a InputStream.
    // Parse the HDR file by its format. HDR format encode can be seen in Radiance HDR(.pic,.hdr) file format
    private fun read1(input: InputStream): FloatImage {

        // Parse HDR file's header line
        // The first line of each HDR file must be "#?RADIANCE".
        val isHDR = readLine(input)
        assertEquals(HDR_MAGIC, isHDR, "Unrecognized format")

        // Besides the first line, there are several lines describing the different information of this HDR file.
        // Maybe it will have the exposure time, format(Must be either"32-bit_rle_rgbe" or "32-bit_rle_xyze")
        // Also the owner's information, the software's version, etc.

        // The above information is not so important for us.
        // The only important information for us is the resolution, which shows the size of the HDR image
        // The resolution information's format is fixed. Usually, it will be -Y 1024 +X 2048 something like this.
        var inform = readLine(input)
        while (inform != "") {
            inform = readLine(input)
        }
        inform = readLine(input)
        val tokens = inform.split(" ")
        val width: Int
        val height: Int
        if (tokens[0][1] == 'Y') {
            width = tokens[3].toInt()
            height = tokens[1].toInt()
        } else {
            width = tokens[1].toInt()
            height = tokens[3].toInt()
        }
        assertTrue(width > 0, "HDR Width must be positive")
        assertTrue(height > 0, "HDR Height must be positive")

        // In the above, the basic information has been collected. Now, we will deal with the pixel data.
        // According to the HDR format document, each pixel is stored as 4 bytes, one bytes mantissa for each RGB and a shared one byte exponent.
        // The pixel data may be stored uncompressed or using a straightforward run length encoding scheme.
        val din = DataInputStream(input)
        val pixels = FloatArray(height * width * 3)

        // optimized from the original; it does not need to be full image size; one row is enough
        // besides it only needs 8 bits of space per component, not 32
        // effectively this halves the required RAM for this program part
        val lineBuffer = ByteArray(width * 4)
        var index = 0

        // We read the information row by row. In each row, the first four bytes store the column number information.
        // The first and second bytes store "2". And the third byte stores the higher 8 bits of col num, the fourth byte stores the lower 8 bits of col num.
        // After these four bytes, these are the real pixel data.
        for (y in 0 until height) {
            // The following code patch is checking whether the hdr file is compressed by run length encode(RLE).
            // For every line of the data part, the first and second byte should be 2(DEC).
            // The third*2^8+the fourth should equal to the width. They combined the width information.
            // For every line, we need check this kind of information. And the starting four numbers of every line is the same
            val a = din.readUnsignedByte()
            val b = din.readUnsignedByte()
            assertTrue(!(a != 2 || b != 2), "Only HDRs with run length encoding are supported.")
            val checksum = din.readUnsignedShort()
            assertEquals(width, checksum, "Width-Checksum is incorrect. Is this file a true HDR?")

            // This inner loop is for the four channels. The way they compressed the data is in this way:
            // Firstly, they compressed a row.
            // Inside that row, they firstly compressed the red channel information. If there are duplicate data, they will use RLE to compress.
            // First data shows the numbers of duplicates(which should minus 128), and the following data is the duplicate one.
            // If there is no duplicate, they will store the information in order.
            // And the first data is the number of how many repeated items, and the following data stream is their associated data.
            for (channel in 0..3) { // This loop controls the four channels RGBE
                var x4 = channel
                val w4 = width * 4 + channel
                while (x4 < w4) { // alternative for x
                    var sequenceLength = din.readUnsignedByte()
                    if (sequenceLength > 128) { // copy-paste data; always the same
                        sequenceLength -= 128
                        val value = din.readUnsignedByte().toByte()
                        while (sequenceLength-- > 0) {
                            lineBuffer[x4] = value
                            x4 += 4
                        }
                    } else { // unique data for sequence length positions
                        while (sequenceLength-- > 0) {
                            lineBuffer[x4] = din.readUnsignedByte().toByte()
                            x4 += 4
                        }
                    }
                }
            }
            for (x in 0 until width) {
                val i2 = x * 4
                val exp = lineBuffer[i2 + 3].toInt() and 255
                if (exp == 0) {
                    index += 3 // black is default
                } else {
                    // could be optimized by using integer arithmetic to calculate this float
                    val exponent = 2f.pow(exp - 128 - 8)
                    pixels[index++] = (lineBuffer[i2].toInt() and 255) * exponent
                    pixels[index++] = (lineBuffer[i2 + 1].toInt() and 255) * exponent
                    pixels[index++] = (lineBuffer[i2 + 2].toInt() and 255) * exponent
                }
            }
        }
        return FloatImage(width, height, 3, pixels)
    }

    private fun readLine(input: InputStream): String {
        val bout = ByteArrayOutputStream(256)
        var i = 0
        while (true) {
            val b = input.read()
            if (b == '\n'.code || b == -1) break
            else if (i > 256) throw IOException("Line too long") // 100 seems short and unsure ;)
            if (b != '\r'.code) {
                bout.write(b)
            }
            i++
        }
        return bout.toString()
    }

    const val HDR_MAGIC = "#?RADIANCE"
}