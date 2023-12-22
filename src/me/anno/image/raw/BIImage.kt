package me.anno.image.raw

import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.utils.LOGGER
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_BYTE_GRAY
import java.io.OutputStream
import javax.imageio.ImageIO

fun BufferedImage.toImage(): Image {
    // if image is grayscale, produce grayscale image
    // we could optimize a lot more formats, but that's probably not needed
    if (type == TYPE_BYTE_GRAY) {
        var i = 0
        val buffer = raster.dataBuffer
        val bytes = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bytes[i] = buffer.getElem(i).toByte()
                i++
            }
        }
        return ByteImage(width, height, ByteImage.Format.R, bytes)
    } else {
        val pixels = getRGB(0, 0, width, height, null, 0, width)
        val hasAlpha = colorModel.hasAlpha() && pixels.any { it.ushr(24) != 255 }
        return IntImage(width, height, pixels, hasAlpha)
    }
}

fun BufferedImage.write(dst: FileReference) {
    val format = dst.lcExtension
    dst.outputStream().use { out: OutputStream ->
        if (!ImageIO.write(this, format, out)) {
            LOGGER.warn("Couldn't find writer for $format")
        }
    }
}