package me.anno.image.bmp

object BMPWriter {

    // converts raw data into a bmp buffer
    // is meant to be worked on locally only, theoretically doesn't need to be saved
    // https://en.wikipedia.org/wiki/BMP_file_format, example 2
    fun createBMP(width: Int, height: Int, argb: ByteArray): ByteArray {
        val pixelDataStart = 0x7a
        val dst = ByteArray(pixelDataStart + width * height * 4)
        // BM
        dst[0] = 0x42
        dst[1] = 0x4d
        fun write16(v: Int, p: Int) {
            dst[p + 0] = v.and(255).toByte()
            dst[p + 1] = v.shr(8).and(255).toByte()
        }

        fun write32(v: Int, p: Int) {
            dst[p + 0] = v.and(255).toByte()
            dst[p + 1] = v.shr(8).and(255).toByte()
            dst[p + 2] = v.shr(16).and(255).toByte()
            dst[p + 3] = v.shr(24).and(255).toByte()
        }
        write32(dst.size, 2)
        // 4 bytes unused
        write32(pixelDataStart, 0xA)
        write32(0x6C, 0xE) // bytes in the DIB header
        write32(width, 0x12)
        write32(height, 0x16)
        write16(1, 0x1A) // 1 plane
        write16(32, 0x1C) // 32 bits
        write32(3, 0x1E) // no compression used
        write32(argb.size, 0x22) // size of the data
        val pixelsPerMeterForPrinting = 2835 // 72 DPI
        write32(pixelsPerMeterForPrinting, 0x26)
        write32(pixelsPerMeterForPrinting, 0x2A)
        // 0 colors in palette, and 0 = all colors are important
        write32(0xff0000, 0x36) // red mask
        write32(0x00ff00, 0x3a) // green mask
        write32(0x0000ff, 0x3e) // blue mask
        write32(0xff.shl(24), 0x42) // alpha mask
        write32(0x57696e20, 0x46) // color space, "Win "
        // a lot of zeros
        for (i in argb.indices) {
            dst[pixelDataStart + i] = argb[i]
        }
        return dst
    }

}