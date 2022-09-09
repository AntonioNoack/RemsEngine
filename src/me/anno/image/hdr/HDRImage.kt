package me.anno.image.hdr

import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import java.io.*
import kotlin.math.*

/**
 * src/author: https://github.com/aicp7/HDR_file_readin;
 * modified for our needs
 * This class is used to convert a HDR format image
 * into a three-dimensional float array representing the RGB channels of the original image.
 * */
class HDRImage : Image {

    lateinit var pixels: FloatArray

    constructor(width: Int, height: Int, numChannels: Int) : super(0, 0, numChannels, numChannels > 3) {
        this.width = width
        this.height = height
        pixels = FloatArray(width * height * numChannels)
    }

    constructor(input: InputStream) : super(0, 0, 3, false) {
        optimizeStream(input).use { input1 -> read(input1) }
    }

    constructor(file: FileReference) : super(0, 0, 3, false) {
        optimizeStream(file.inputStream()).use { input -> read(input) }
    }

    fun optimizeStream(input: InputStream): InputStream {
        return if (input is BufferedInputStream ||
            input is ByteArrayInputStream
        ) input else BufferedInputStream(input)
    }

    fun hasAlphaChannel(): Boolean {
        return false
    }

    override fun getRGB(index: Int): Int {
        val i0 = index * numChannels
        val delta = typicalBrightness
        var r = pixels[i0] * delta
        var g = pixels[i0 + 1] * delta
        var b = pixels[i0 + 2] * delta
        // reinhard tonemapping
        r = r / (r + 1f) * 255f
        g = g / (g + 1f) * 255f
        b = b / (b + 1f) * 255f
        return rgb(r.toInt(), g.toInt(), b.toInt())
    }

    override fun createIntImage(): IntImage {
        // accelerated version without that many function calls
        // and member calls
        val width = width
        val height = height
        val size = width * height
        val data = IntArray(size)
        val pixels = pixels
        val delta = typicalBrightness
        val pixelStride = numChannels - 2
        var i = 0
        var i0 = 0
        while (i < size) {
            var r = pixels[i0++] * delta
            var g = pixels[i0++] * delta
            var b = pixels[i0] * delta
            r = r / (r + 1f) * 255f
            g = g / (g + 1f) * 255f
            b = b / (b + 1f) * 255f
            data[i] = rgb(r.toInt(), g.toInt(), b.toInt())
            i0 += pixelStride
            i++
        }
        return IntImage(width, height, data, hasAlphaChannel)
    }

    fun createMonoTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {

        when (numChannels) {
            1 -> createTexture(texture, sync, checkRedundancy)
            2 -> {
                val data = pixels
                val data3 = FloatArray(width * height)
                var j = 0
                for (i in data3.indices) {
                    val r = data[j++]
                    val g = data[j++]
                    data3[i] = (r + g) * 0.5f
                }
                val data2 = if (checkRedundancy) texture.checkRedundancyMonochrome(data3) else data3
                if (sync && GFX.isGFXThread()) {
                    texture.createMonochrome(data2, false)
                } else {
                    GFX.addGPUTask("HDRImage", width, height) {
                        texture.createMonochrome(data2, false)
                    }
                }
            }
            else -> {
                val data = pixels
                val data3 = FloatArray(width * height)
                var j = 0
                val offset = numChannels - 3
                for (i in data3.indices) {
                    val r = data[j++]
                    val g = data[j++]
                    val b = data[j++]
                    data3[i] = ShaderLib.brightness(r, g, b)
                    j += offset
                }
                val data2 = if (checkRedundancy) texture.checkRedundancyMonochrome(data3) else data3
                if (sync && GFX.isGFXThread()) {
                    texture.createMonochrome(data2, false)
                } else {
                    GFX.addGPUTask("HDRImage", width, height) {
                        texture.createMonochrome(data2, false)
                    }
                }
            }
        }
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        val data = pixels
        val data2 = if (checkRedundancy) {
            when (numChannels) {
                1 -> texture.checkRedundancyMonochrome(data)
                2 -> texture.checkRedundancyRG(data)
                3 -> texture.checkRedundancyRGB(data)
                4 -> texture.checkRedundancyRGBA(data)
                else -> throw NotImplementedError()
            }
        } else data
        if (sync && GFX.isGFXThread()) {
            createTex(texture, data2)
        } else {
            GFX.addGPUTask("HDRImage", width, height) {
                createTex(texture, data2)
            }
        }
    }

    private fun createTex(texture: Texture2D, data2: FloatArray) {
        when (numChannels) {
            1 -> texture.createMonochrome(data2, false)
            2 -> texture.createRG(data2, false)
            3 -> texture.createRGB(data2, false)
            4 -> texture.createRGBA(data2, false)
            else -> throw NotImplementedError()
        }
    }

    // Construction method if the input is a InputStream.
    // Parse the HDR file by its format. HDR format encode can be seen in Radiance HDR(.pic,.hdr) file format
    private fun read(input: InputStream) {
        // Parse HDR file's header line
        // readLine(InputStream in) method will be introduced later.

        // The first line of the HDR file. If it is a HDR file, the first line should be "#?RADIANCE"
        // If not, we will throw a IllegalArgumentException.
        val isHDR = readLine(input)
        require(isHDR == HDR_MAGIC) { "Unrecognized format: $isHDR" }

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
        val tokens = inform.split(" ".toRegex(), 4).toTypedArray()
        if (tokens[0][1] == 'Y') {
            width = tokens[3].toInt()
            height = tokens[1].toInt()
        } else {
            width = tokens[1].toInt()
            height = tokens[3].toInt()
        }
        require(width > 0) { "HDR Width must be positive" }
        require(height > 0) { "HDR Height must be positive" }

        // In the above, the basic information has been collected. Now, we will deal with the pixel data.
        // According to the HDR format document, each pixel is stored as 4 bytes, one bytes mantissa for each r,g,b and a shared one byte exponent.
        // The pixel data may be stored uncompressed or using a straightforward run length encoding scheme.
        val din = DataInputStream(input)
        pixels = FloatArray(height * width * 3)

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
            // The third*2^8+the fourth should equals to the width. They combined the width information.
            // For every line, we need check this kind of informatioin. And the starting four nums of every line is the same
            val a = din.readUnsignedByte()
            val b = din.readUnsignedByte()
            require(!(a != 2 || b != 2)) { "Only HDRs with run length encoding are supported." }
            val checksum = din.readUnsignedShort()
            require(checksum == width) { "Width-Checksum is incorrect. Is this file a true HDR?" }

            // This inner loop is for the four channels. The way they compressed the data is in this way:
            // Firstly, they compressed a row.
            // Inside that row, they firstly compressed the red channel information. If there are duplicate data, they will use RLE to compress.
            // First data shows the numbers of duplicates(which should minus 128), and the following data is the duplicate one.
            // If there is no duplicate, they will store the information in order.
            // And the first data is the number of how many induplicate items, and the following data stream is their associated data.
            for (channel in 0..3) { // This loop controls the four channel. R,G,B and Exp.
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
                    val exponent = 2f.pow(exp - 128 - 8) // could be optimized by using integer arithmetic to calculate this float
                    pixels[index++] = (lineBuffer[i2].toInt() and 255) * exponent
                    pixels[index++] = (lineBuffer[i2 + 1].toInt() and 255) * exponent
                    pixels[index++] = (lineBuffer[i2 + 2].toInt() and 255) * exponent
                }
            }
        }
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

    override fun write(dst: FileReference) {
        if ("hdr" == dst.lcExtension) {
            throw RuntimeException("Exporting HDR as HDR isn't yet implemented")
        } else super.write(dst)
    }

    companion object {

        private fun rgb(r: Int, g: Int, b: Int): Int {
            return -0x1000000 or (r shl 16) or (g shl 8) or b
        }

        // with the reinhard tonemapping, the average brightness of pixels is expected to be
        // more than just 1, and more like 5
        var typicalBrightness = 5f

        fun writeHDR(w: Int, h: Int, pixels: FloatArray, out0: OutputStream?) {
            val out = DataOutputStream(out0)
            out.writeBytes(HDR_MAGIC)
            // metadata, which seems to be required
            out.writeBytes("\nFORMAT=32-bit_rle_rgbe\n\n")
            out.writeBytes("-Y ")
            out.writeBytes(h.toString())
            out.writeBytes(" +X ")
            out.writeBytes(w.toString())
            out.writeByte('\n'.code)
            val rowBytes = ByteArray(4 * (w + 2)) // +2 for seamless testing
            for (y in 0 until h) {
                // bytes for RLE
                out.writeByte(2)
                out.writeByte(2)
                // "checksum"
                out.writeShort(w)
                // collect bytes
                // convert floats into bytes
                var x = 0
                var i = 0
                var j = y * w * 3
                while (x < w) {
                    val r0 = pixels[j++]
                    val g0 = pixels[j++]
                    val b0 = pixels[j++]
                    val max = max(max(r0, g0), b0)
                    if (max > 0) {
                        // Math.pow(2, exp - 128 - 8)
                        // probably could be optimized massively by extracting the exponent from the binary representation
                        val exp0 = clamp(ceil(log2(max * 256f / 255f)), -128f, 127f) // +128
                        val invPow = 2f.pow(-exp0 + 8)
                        val r = (r0 * invPow).roundToInt()
                        val g = (g0 * invPow).roundToInt()
                        val b = (b0 * invPow).roundToInt()
                        rowBytes[i++] = clamp(r, 0, 255).toByte()
                        rowBytes[i++] = clamp(g, 0, 255).toByte()
                        rowBytes[i++] = clamp(b, 0, 255).toByte()
                        rowBytes[i++] = (exp0 + 128).toInt().toByte()
                    } else {
                        // just zeros; exponent could be the same as the old value,
                        // but zero is rare probably anyway
                        i += 4
                    }
                    x++
                }
                // compress byte stream with RLE
                for (channel in 0..3) {
                    // check how long the next run is, up to 128
                    // if the run is short (1 or 2), then find how long the run of different heterogeneous data is
                    var xi = 0
                    while (xi < w) {
                        var i0 = channel + (xi shl 2)
                        val firstValue = rowBytes[i0]
                        var length = 1
                        if (rowBytes[i0 + 4] == firstValue && rowBytes[i0 + 8] == firstValue) { // at least 3 bytes have the same value
                            // find length of the same value
                            var j0 = i0 + 4
                            while (length < 127 && xi + length < w && rowBytes[j0] == firstValue) {
                                length++
                                j0 += 4
                            }
                            out.writeByte(length + 128)
                            out.writeByte(firstValue.toInt())
                        } else {
                            // find length until there is a repeating value
                            var indexI = i0 + 4
                            while (length < 128 && xi + length < w) {
                                val valueI = rowBytes[indexI]
                                indexI += if (rowBytes[indexI + 4] == valueI && rowBytes[indexI + 8] == valueI) {
                                    break // found repeating strip
                                } else {
                                    length++
                                    4
                                }
                            }
                            out.writeByte(length)
                            val endIndex = i0 + 4 * length
                            while (i0 < endIndex) {
                                out.writeByte(rowBytes[i0].toInt())
                                i0 += 4
                            }
                        }
                        xi += length
                    }
                }
            }
            out.close()
        }

        private const val HDR_MAGIC = "#?RADIANCE"

    }
}