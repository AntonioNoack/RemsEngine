package me.anno.image.bmp

import me.anno.image.Image

/**
 * the quickest way to save images
 * */
object BMPWriter {

    private const val HEADER_SIZE = 0x7a

    fun calculateSize(img: Image): Long {
        return HEADER_SIZE + img.width * img.height * 4L
    }

    fun createBMP(img: Image): ByteArray {
        val dst = createBMPHeader(img)
        // a lot of zeros
        var j = HEADER_SIZE
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val color = img.getRGB(x, y)
                dst[j++] = color.toByte()
                dst[j++] = (color shr 8).toByte()
                dst[j++] = (color shr 16).toByte()
                dst[j++] = (color shr 24).toByte()
            }
        }
        return dst
    }

    private fun createBMPHeader(img: Image): ByteArray {
        val dst = ByteArray(calculateSize(img).toInt())
        // BM
        dst[0] = 0x42
        dst[1] = 0x4d
        fun write16(v: Int, p: Int) {
            dst[p] = v.and(255).toByte()
            dst[p + 1] = v.shr(8).and(255).toByte()
        }

        fun write32(v: Int, p: Int) {
            dst[p] = v.and(255).toByte()
            dst[p + 1] = v.shr(8).and(255).toByte()
            dst[p + 2] = v.shr(16).and(255).toByte()
            dst[p + 3] = v.shr(24).and(255).toByte()
        }
        write32(dst.size, 2)
        // 4 bytes unused
        write32(HEADER_SIZE, 0xA)
        write32(0x6C, 0xE) // bytes in the DIB header
        write32(+img.width, 0x12)
        write32(-img.height, 0x16) // flip upside down
        write16(1, 0x1A) // 1 plane
        write16(32, 0x1C) // 32 bits
        write32(3, 0x1E) // no compression used
        write32(dst.size - HEADER_SIZE, 0x22) // size of the data
        val pixelsPerMeterForPrinting = 2835 // 72 DPI
        write32(pixelsPerMeterForPrinting, 0x26)
        write32(pixelsPerMeterForPrinting, 0x2A)
        // 0 colors in palette, and 0 = all colors are important
        write32(0xff0000, 0x36) // red mask
        write32(0x00ff00, 0x3a) // green mask
        write32(0x0000ff, 0x3e) // blue mask
        write32(0xff.shl(24), 0x42) // alpha mask
        write32(0x57696e20, 0x46) // color space, "Win "
        return dst
    }

}