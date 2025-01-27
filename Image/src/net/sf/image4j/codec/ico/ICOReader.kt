package net.sf.image4j.codec.ico

import me.anno.image.Image
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readNBytes2
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.structures.CountingInputStream
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.tuples.IntPair
import net.sf.image4j.codec.bmp.BMPDecoder
import net.sf.image4j.codec.bmp.InfoHeader
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * Decodes images in ICO format.
 *
 * @author Ian McDonagh
 * Modified by Antonio Noack to read our own Image class, not BufferedImage and to add a few more features
 */
object ICOReader {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(ICOReader::class)
    private const val PNG_MAGIC = -0x76afb1b9
    private const val PNG_MAGIC_LE = 0x474E5089
    private const val PNG_MAGIC2 = 0x0D0A1A0A
    private const val PNG_MAGIC2_LE = 0x0A1A0A0D

    @JvmStatic
    private fun ensureOffset(input1: CountingInputStream, entry: IconEntry, i: Int) {
        // Make sure we're at the right file offset!
        val targetOffset = entry.fileOffset
        if (input1.position < targetOffset)
            input1.skip(targetOffset - input1.position)
        val fileOffset = input1.position
        if (fileOffset != targetOffset.toLong()) {
            throw IOException(
                "Cannot read image #$i starting at unexpected file offset, " +
                        "0x${fileOffset.toString(16)} != 0x${entry.fileOffset.toString(16)}."
            )
        }
    }

    @JvmStatic
    private fun readLayer(input1: CountingInputStream, entry: IconEntry, i: Int): Any {

        ensureOffset(input1, entry, i)

        val info = input1.readLE32()
        LOGGER.info(
            "Image #$i @ ${input1.position} info = 0x${info.toString(16)}, " +
                    "${entry.width} x ${entry.height} x ${entry.bitCount} bpp"
        )

        return when (info) {
            40 -> {
                // read XOR bitmap
                // BMPDecoder bmp = new BMPDecoder(is);
                val infoHeader = BMPDecoder.readInfoHeader(input1)
                val andHeader = InfoHeader(infoHeader)
                andHeader.height = infoHeader.height shr 1
                val xorHeader = InfoHeader(infoHeader)
                xorHeader.height = andHeader.height
                andHeader.bitCount = 1

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
                if (infoHeader.bitCount == 32) {

                    // transparency from alpha
                    // ignore bytes after XOR bitmap
                    /*val size = entry.sizeInBytes
                    val infoHeaderSize = infoHeader.size
                    // data size = w * h * 4
                    val dataSize = xorHeader.width * xorHeader.height * 4
                    val skip = size - infoHeaderSize - dataSize
                    // int skip2 = entries[i].iFileOffset + size - in.getCount();

                    // ignore AND bitmap since alpha channel stores transparency
                    if (input1.skip(skip.toLong()) < skip && i < sCount - 1) {
                        throw EOFException("Unexpected end of input")
                    }*/
                    // If we skipped fewer bytes than expected, the AND mask
                    // is probably badly formatted.
                    // If we're at the last/only entry in the file, silently
                    // ignore and continue processing...

                    // // read AND bitmap
                    // Image and = BMPDecoder.read(andHeader, in, andColorTable);
                    // this.img.add(and);
                } else {

                    // replace the alpha with the next image
                    val alphaData = BMPDecoder.read(andHeader, input1, andColorTable)
                    val alphaData2 = alphaData.data
                    val data0 = img.data
                    img.data.copyInto(data0)
                    var j = 0
                    val l = data0.size
                    while (j < l) {
                        data0[j] = data0[j] and 0xffffff or (alphaData2[j] and -0x1000000)
                        j++
                    }
                    img.hasAlphaChannel = true
                }

                // create ICOImage
                img
            }
            PNG_MAGIC_LE -> {
                val info2 = input1.readLE32()
                if (info2 != PNG_MAGIC2_LE) {
                    return IOException("Unrecognized icon format for image #$i")
                }
                val pngBytes = packPNGBytes(input1, entry.sizeInBytes)
                if (pngBytes !is ByteArray) return pngBytes // exception case
                val stream = ByteArrayInputStream(pngBytes)
                ImageIO.read(stream).toImage()
            }
            else -> return IOException("Unrecognized icon format for image #$i")
        }
    }

    /**
     * Reads and decodes ICO data from the given source. The returned list of
     * images is in the order in which they appear in the source ICO data.
     *
     * returns List<Image> or exception
     */
    @JvmStatic
    fun readAllLayers(input0: InputStream): Any {

        val input1 = CountingInputStream(input0)

        // Reserved 2 bytes = 0
        input1.readLE16()

        // Type 2 bytes = 1
        input1.readLE16()

        // Count, 2 byte: number of icons in this file
        val sCount = input1.readLE16()

        // Entries Count * 16 list of icons
        // images list of bitmap structures in BMP/PNG format
        val entries = createArrayList(sCount) { IconEntry(input1) }
        entries.sortBy { it.fileOffset }

        val ret = ArrayList<Image>(sCount)
        for (i in 0 until sCount) {
            try {
                val layer = readLayer(input1, entries[i], i)
                if (layer !is Image) return layer // exception case
                ret.add(layer)
            } catch (ex: IOException) {
                if (ret.isNotEmpty()) {
                    ex.printStackTrace()
                    return ret
                }
                return IOException("Failed to read image #$i/$sCount", ex)
            }
        }
        return ret
    }

    @JvmStatic
    private fun findBestLayer(input1: CountingInputStream, targetSize: Int): Any {

        // Reserved 2 byte =0
        input1.readLE16()

        // Type 2 byte =1
        input1.readLE16()

        // Count, 2 byte: number of icons in this file
        val sCount = input1.readLE16()
        var bestDist = 0
        var bestLayer: IconEntry? = null
        for (i in 0 until sCount) {
            val layer = IconEntry(input1)
            val size = (layer.width + layer.height) ushr 1
            val dist = abs(size - targetSize)
            if (bestLayer == null || layer.bitCount > bestLayer.bitCount ||
                (layer.bitCount == bestLayer.bitCount && dist < bestDist)
            ) {
                bestDist = dist
                bestLayer = layer
                layer.index = i
            }
        }
        return bestLayer
            ?: IOException("No layer was found")
    }

    /**
     * Reads and decodes ICO data from the given source.
     * Finds the best image with size closest to parameter "size"
     *
     * returns image or exception
     */
    @JvmStatic
    fun read(input0: InputStream, targetSize: Int = MAX_SIZE): Any {
        val input1 = CountingInputStream(input0)
        val bestLayer = findBestLayer(input1, targetSize)
        if (bestLayer !is IconEntry) return bestLayer
        return readLayer(input1, bestLayer, bestLayer.index)
    }

    @JvmStatic
    private fun ByteArray.set2(i: Int, v: Int) {
        this[i] = (v shr 24).toByte()
        this[i + 1] = (v shr 16).toByte()
        this[i + 2] = (v shr 8).toByte()
        this[i + 3] = v.toByte()
    }

    /**
     * return ByteArray or exception
     * */
    @JvmStatic
    private fun packPNGBytes(input1: CountingInputStream, sizeInBytes: Int): Any {
        // we could encapsulate this smarter, directly as an InputStream, without intermediate buffer...
        val bytes = ByteArray(sizeInBytes)
        bytes.set2(0, PNG_MAGIC)
        bytes.set2(4, PNG_MAGIC2)
        return input1.readNBytes2(bytes, 8, sizeInBytes - 8)
    }

    /**
     * return size or exception
     * */
    @JvmStatic
    fun findSize(input0: InputStream): Any {
        val input1 = CountingInputStream(input0)
        val bestLayer = findBestLayer(input1, MAX_SIZE)
        if (bestLayer !is IconEntry) return bestLayer
        return IntPair(bestLayer.width, bestLayer.height)
    }

    private const val MAX_SIZE = 1024 * 1024
}