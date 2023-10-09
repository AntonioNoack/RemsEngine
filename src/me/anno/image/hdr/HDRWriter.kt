package me.anno.image.hdr

import me.anno.maths.Maths
import java.io.DataOutputStream
import java.io.OutputStream
import kotlin.math.*

object HDRWriter {

    fun writeHDR(w: Int, h: Int, pixels: FloatArray, out0: OutputStream) {
        val out = DataOutputStream(out0)
        out.writeBytes(HDRReader.HDR_MAGIC)
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
                    val exp0 = Maths.clamp(ceil(log2(max * 256f / 255f)), -128f, 127f) // +128
                    val invPow = 2f.pow(-exp0 + 8)
                    val r = (r0 * invPow).roundToInt()
                    val g = (g0 * invPow).roundToInt()
                    val b = (b0 * invPow).roundToInt()
                    rowBytes[i++] = Maths.clamp(r, 0, 255).toByte()
                    rowBytes[i++] = Maths.clamp(g, 0, 255).toByte()
                    rowBytes[i++] = Maths.clamp(b, 0, 255).toByte()
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
                            if (rowBytes[indexI + 4] == valueI && rowBytes[indexI + 8] == valueI) {
                                break // found repeating strip
                            } else {
                                length++
                                indexI += 4
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
}