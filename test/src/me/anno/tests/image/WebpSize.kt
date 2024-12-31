package me.anno.tests.image

import me.anno.io.MediaMetadata
import me.anno.io.Streams.readNBytes2
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Arrays.startsWith
import java.io.IOException
import java.io.InputStream

/**
 * Extract the image size from WebP files;
 * implemented in-engine, so we can skip some FFMPEG calls.
 *
 * -> we could use this, but it's not really needed; just ImageIO for this
 * */
object WebpSize {

    fun register() {
        MediaMetadata.registerSignatureHandler(90, "webp") { _, signature, dst, ri ->
            if (signature == "webp") { // ImageIO might support that, too...
                dst.setImageByStream(WebpSize::getWebpImageSize, ri)
            } else false
        }
    }

    private fun readU8(buffer: ByteArray, offset: Int): Int {
        return buffer[offset].toInt() and 0xff
    }

    private fun readU16(buffer: ByteArray, offset: Int): Int {
        return readU8(buffer, offset) or readU8(buffer, offset + 1).shl(8)
    }

    private fun readU24(buffer: ByteArray, offset: Int): Int {
        return readU16(buffer, offset) or readU8(buffer, offset + 2).shl(16)
    }

    // code originally from ChatGPT, adjusted for our needs
    /**
     * returns IntPair | Exception
     * */
    fun getWebpImageSize(stream: InputStream): Any {
        val minSize = 30
        val buffer = stream.readNBytes2(minSize, false)
        if (buffer.size < minSize) {
            return IOException("File too small to be a valid WebP")
        }

        if (!buffer.startsWith("RIFF", 0)) return IOException("Missing RIFF")
        if (!buffer.startsWith("WEBP", 8)) return IOException("Missing WEBP")

        // Determine chunk type
        return when {
            buffer.startsWith("VP8 ", 12) -> { // Lossy WebP
                val mask = 0x3fff
                val width = readU16(buffer, 26) and mask
                val height = readU16(buffer, 28) and mask
                IntPair(width, height)
            }
            buffer.startsWith("VP8L", 12) -> { // Lossless WebP
                val b1 = readU8(buffer, 21)
                val b2 = readU8(buffer, 22)
                val b3 = readU8(buffer, 23)
                val b4 = readU8(buffer, 24)
                val width = 1 + ((b2 and 0x3F) shl 8 or b1)
                val height = 1 + ((b4 and 0xF) shl 10 or (b3 shl 2) or (b2 shr 6))
                IntPair(width, height)
            }
            buffer.startsWith("VP8X", 12) -> { // Extended WebP
                val width = 1 + readU24(buffer, 24)
                val height = 1 + readU24(buffer, 27)
                val flags = readU8(buffer, 20)
                val isAnimated = (flags and 0x02) != 0
                if (isAnimated) {
                    // get an animated WebP file and check that this correctly uses ImageIO/FFMPEG
                    //  -> yes, that works :)
                    IOException("Animated WebP should be handled by FFMPEG")
                } else IntPair(width, height)
            }
            else -> IOException("Unsupported or invalid WebP")
        }
    }
}