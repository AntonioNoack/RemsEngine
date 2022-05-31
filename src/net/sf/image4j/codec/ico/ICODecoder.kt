/*
 * ICODecoder.java
 *
 * Created on May 9, 2006, 9:31 PM
 *
 */
package net.sf.image4j.codec.ico

import me.anno.image.Image
import me.anno.image.raw.BIImage
import net.sf.image4j.codec.bmp.BMPDecoder
import net.sf.image4j.codec.bmp.InfoHeader
import net.sf.image4j.io.CountingInputStream
import net.sf.image4j.io.Utils
import net.sf.image4j.io.LittleEndianInputStream
import org.apache.logging.log4j.LogManager
import java.io.*
import javax.imageio.ImageIO

/**
 * Decodes images in ICO format.
 *
 * @author Ian McDonagh
 * Modified by Antonio Noack to read our own Image class, not BufferedImage
 */
object ICODecoder {

    private val LOGGER = LogManager.getLogger(ICODecoder::class)
    private const val PNG_MAGIC = -0x76afb1b9
    private const val PNG_MAGIC_LE = 0x474E5089
    private const val PNG_MAGIC2 = 0x0D0A1A0A
    private const val PNG_MAGIC2_LE = 0x0A1A0A0D

    /**
     * Reads and decodes ICO data from the given source. The returned list of
     * images is in the order in which they appear in the source ICO data.
     *
     * @param `is` the source <tt>InputStream</tt> to read
     * @return the list of images decoded from the ICO data
     * @throws java.io.IOException if an error occurs
     */
    @Throws(IOException::class)
    fun read(input0: InputStream): List<Image> {

        // long t = System.currentTimeMillis()
        val input1 = LittleEndianInputStream(CountingInputStream(input0))

        // Reserved 2 byte =0
        input1.readShortLE()

        // Type 2 byte =1
        input1.readShortLE()

        // Count 2 byte Number of Icons in this file
        val sCount = input1.readShortLE().toInt().and(0xffff)

        // Entries Count * 16 list of icons
        val entries = Array(sCount) { IconEntry(input1) }

        // Seems like we don't need this, but you never know!
        // entries = sortByFileOffset(entries)
        // images list of bitmap structures in BMP/PNG format
        val ret = ArrayList<Image>(sCount)
        for (i in 0 until sCount) {
            try {

                // Make sure we're at the right file offset!
                val fileOffset = input1.count
                if (fileOffset != entries[i].iFileOffset) {
                    throw IOException("Cannot read image #$i starting at unexpected file offset.")
                }
                val info = input1.readIntLE()
                LOGGER.info("Image #$i @ ${input1.count} info = ${Utils.toInfoString(info)}")

                when (info) {
                    40 -> {
                        // read XOR bitmap
                        // BMPDecoder bmp = new BMPDecoder(is);
                        val infoHeader = BMPDecoder.readInfoHeader(input1, info)
                        val andHeader = InfoHeader(infoHeader)
                        andHeader.iHeight = infoHeader.iHeight / 2
                        val xorHeader = InfoHeader(infoHeader)
                        xorHeader.iHeight = andHeader.iHeight
                        andHeader.sBitCount = 1
                        andHeader.iNumColors = 2

                        // for now, just read all the raster data (xor + and)
                        // and store as separate images
                        val img = BMPDecoder.read(xorHeader, input1)
                        // If we want to be sure we've decoded the XOR mask
                        // correctly, we can write it out as a PNG to a temp file here.
                        // try {
                        // File temp = File.createTempFile("image4j", ".png");
                        // ImageIO.write(xor, "png", temp);
                        // log.info("Wrote xor mask for image #" + i + " to "
                        // + temp.getAbsolutePath());
                        // } catch (Throwable ex) {
                        // }
                        // Or just add it to the output list:
                        // img.add(xor);
                        val andColorTable = intArrayOf(-1, 0)
                        if (infoHeader.sBitCount == 32) {

                            // transparency from alpha
                            // ignore bytes after XOR bitmap
                            val size = entries[i].iSizeInBytes
                            val infoHeaderSize = infoHeader.iSize
                            // data size = w * h * 4
                            val dataSize = xorHeader.iWidth * xorHeader.iHeight * 4
                            val skip = size - infoHeaderSize - dataSize
                            // int skip2 = entries[i].iFileOffset + size - in.getCount();

                            // ignore AND bitmap since alpha channel stores
                            // transparency
                            if (input1.skip(skip, false) < skip && i < sCount - 1) {
                                throw EOFException("Unexpected end of input")
                            }
                            // If we skipped less bytes than expected, the AND mask
                            // is probably badly formatted.
                            // If we're at the last/only entry in the file, silently
                            // ignore and continue processing...

                            // // read AND bitmap
                            // BufferedImage and = BMPDecoder.read(andHeader, in,
                            // andColorTable);
                            // this.img.add(and);
                        } else {

                            // replace the alpha with the next image
                            val alphaData = BMPDecoder.read(andHeader, input1, andColorTable)
                            val alphaData2 = alphaData.data
                            val data0 = img.data
                            System.arraycopy(img.data, 0, data0, 0, data0.size)
                            var j = 0
                            val l = data0.size
                            while (j < l) {
                                data0[j] = data0[j] and 0xffffff or (alphaData2[j] and -0x1000000)
                                j++
                            }
                            img.hasAlphaChannel = true
                        }

                        // create ICOImage
                        ret.add(img)
                    }
                    PNG_MAGIC_LE -> {
                        val info2 = input1.readIntLE()
                        if (info2 != PNG_MAGIC2_LE) {
                            throw IOException("Unrecognized icon format for image #$i")
                        }

                        val e = entries[i]
                        val size = e.iSizeInBytes - 8
                        val pngData = ByteArray(size)
                        input1.readFully(pngData)
                        val out1 = ByteArrayOutputStream()
                        val out2 = DataOutputStream(out1)
                        out2.writeInt(PNG_MAGIC)
                        out2.writeInt(PNG_MAGIC2)
                        out2.write(pngData)
                        val pngData2 = out1.toByteArray()
                        val bin = ByteArrayInputStream(pngData2)
                        val input = ImageIO.createImageInputStream(bin)
                        val reader = reader
                        reader!!.input = input
                        val img = reader.read(0)

                        // create ICOImage
                        ret.add(BIImage(img))
                    }
                    else -> throw IOException("Unrecognized icon format for image #$i")
                }
            } catch (ex: IOException) {
                throw IOException("Failed to read image #$i", ex)
            }
        }

        // long t2 = System.currentTimeMillis()
        // LOGGER.debug("Loaded ICO file in "+(t2 - t)+"ms")

        return ret
    }

    private val reader by lazy {
        val itr = ImageIO.getImageReadersByFormatName("png")
        if (itr.hasNext()) itr.next() else null
    }

}