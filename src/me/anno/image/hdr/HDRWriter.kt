package me.anno.image.hdr

import me.anno.io.Streams.writeBE16
import me.anno.io.Streams.writeString
import me.anno.maths.Maths
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

object HDRWriter {

    fun writeHDR(w: Int, h: Int, stride: Int, pixels: FloatArray, out: OutputStream) {
        writeHDRHeader(w, h, out)
        val rowBytes = ByteArray(4 * (w + 2)) // +2 for faster RLE (no bounds checks needed), 4x for RGBE
        for (y in 0 until h) {
            // bytes for RLE
            out.write(2)
            out.write(2)
            // "checksum"
            out.writeBE16(w)
            convertFloatsToBytes(w, y * stride * 3, pixels, rowBytes)
            rleEncodeBytes(w, rowBytes, out)
        }
    }

    private fun writeHDRHeader(w: Int, h: Int, out: OutputStream) {
        out.writeString(HDRReader.HDR_MAGIC)
        // metadata, which seems to be required
        out.writeString("\nFORMAT=32-bit_rle_rgbe\n\n")
        out.writeString("-Y ")
        out.writeString(h.toString())
        out.writeString(" +X ")
        out.writeString(w.toString())
        out.write('\n'.code)
    }

    private fun convertFloatsToBytes(w: Int, srcI0: Int, pixels: FloatArray, rowBytes: ByteArray) {
        for (x in 0 until w) {
            val srcI = srcI0 + x * 3
            val dstI = x shl 2
            val r0 = pixels[srcI]
            val g0 = pixels[srcI + 1]
            val b0 = pixels[srcI + 2]
            val max = max(max(r0, g0), b0)
            if (max > 0) {
                // Math.pow(2, exp - 128 - 8)
                // probably could be optimized massively by extracting the exponent from the binary representation
                val exp0 = Maths.clamp(ceil(log2(max * 256f / 255f)), -128f, 127f) // +128
                val invPow = 2f.pow(-exp0 + 8)
                val r = (r0 * invPow).roundToIntOr()
                val g = (g0 * invPow).roundToIntOr()
                val b = (b0 * invPow).roundToIntOr()
                rowBytes[dstI] = Maths.clamp(r, 0, 255).toByte()
                rowBytes[dstI + 1] = Maths.clamp(g, 0, 255).toByte()
                rowBytes[dstI + 2] = Maths.clamp(b, 0, 255).toByte()
                rowBytes[dstI + 3] = (exp0 + 128f).toIntOr().toByte()
            }
            // else just zeros; exponent could be the same as the old value,
            // but zero is rare probably anyway
        }
    }

    private fun rleEncodeBytes(w: Int, rowBytes: ByteArray, out: OutputStream) {
        for (channel in 0 until 4) {
            rleEncodeBytes(w, rowBytes, out, channel)
        }
    }

    private fun rleEncodeBytes(w: Int, rowBytes: ByteArray, out: OutputStream, channel: Int) {
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
                out.write(length + 128)
                out.write(firstValue.toInt())
            } else {
                // find length until there is a repeating value
                var indexI = i0 + 4
                while (length < 128 && xi + length < w) {
                    val valueI = rowBytes[indexI]
                    if (rowBytes[indexI + 4] == valueI && rowBytes[indexI + 8] == valueI) {
                        break // found repeating strip
                    } else {
                        length++
                        indexI += 4
                    }
                }
                out.write(length)
                val endIndex = i0 + 4 * length
                while (i0 < endIndex) {
                    out.write(rowBytes[i0].toInt())
                    i0 += 4
                }
            }
            xi += length
        }
    }
}