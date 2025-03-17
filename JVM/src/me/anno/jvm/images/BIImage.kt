package me.anno.jvm.images

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.OutputStream
import javax.imageio.ImageIO

object BIImage {

    private val LOGGER = LogManager.getLogger("BufferedImage")

    fun BufferedImage.write(dst: FileReference) {
        val format = dst.lcExtension
        dst.outputStream().use { out: OutputStream ->
            if (!ImageIO.write(this, format, out)) {
                LOGGER.warn("Couldn't find writer for $format")
            }
        }
    }

    fun Image.createBufferedImage(): BufferedImage {
        val asIntImage = asIntImage()
        val width = asIntImage.width
        val height = asIntImage.height
        val result = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
        val dataBuffer = result.raster.dataBuffer as DataBufferInt
        val dst = dataBuffer.data
        for (y in 0 until height) {
            val srcI = asIntImage.getIndex(0, y)
            asIntImage.data.copyInto(dst, y * width, srcI, srcI + width)
        }
        return result
    }

    fun Texture2D.createFromBufferedImage(image: BufferedImage, callback: Callback<ITexture2D>) {
        width = image.width
        height = image.height
        wasCreated = false
        image.toImage().createTexture(this, checkRedundancy = false, callback)
    }

    fun BufferedImage.toImage(withAlphaOverride: Boolean? = null): Image {
        // if image is grayscale, produce grayscale image
        // we could optimize a lot more formats, but that's probably not needed
        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            var i = 0
            val buffer = raster.dataBuffer
            val bytes = ByteArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    bytes[i] = buffer.getElem(i).toByte()
                    i++
                }
            }
            return ByteImage(width, height, ByteImageFormat.R, bytes)
        } else {
            val pixels = when (type) {
                BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_RGB -> (data.dataBuffer as DataBufferInt).data
                else -> getRGB(0, 0, width, height, null, 0, width)
            }
            val hasAlpha = withAlphaOverride
                ?: (colorModel.hasAlpha() && pixels.any { it.ushr(24) != 255 })
            return IntImage(width, height, pixels, hasAlpha)
        }
    }
}