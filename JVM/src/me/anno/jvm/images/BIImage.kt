package me.anno.jvm.images

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.utils.Color.undoPremultiply
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.DirectColorModel
import java.awt.image.Raster
import java.io.OutputStream
import javax.imageio.ImageIO

object BIImage {

    private val LOGGER = LogManager.getLogger("BufferedImage")

    private val MASKS_ARGB = intArrayOf(0xff0000, 0x00ff00, 0x0000ff, 0xff000000.toInt())
    private val MASKS_RGB = intArrayOf(0xff0000, 0x00ff00, 0x0000ff)

    fun BufferedImage.write(dst: FileReference) {
        val format = dst.lcExtension
        dst.outputStream().use { out: OutputStream ->
            if (!ImageIO.write(this, format, out)) {
                LOGGER.warn("Couldn't find writer for $format")
            }
        }
    }

    fun Image.createBufferedImage(withAlpha: Boolean): BufferedImage {
        val src = asIntImage()
        val width = src.width
        val height = src.height

        val pixelData =
            if (src.offset == 0 && width == src.stride) {
                // reuse data :3
                src.data
            } else {
                val tmp = IntImage(width, height, withAlpha)
                src.copyInto(tmp, 0, 0)
                tmp.data
            }

        val dataBuffer = DataBufferInt(pixelData, pixelData.size)
        val masks = if (withAlpha) MASKS_ARGB else MASKS_RGB
        val raster = Raster.createPackedRaster(dataBuffer, width, height, width, masks, null)
        val colorModel = if (withAlpha) DirectColorModel(32, masks[0], masks[1], masks[2], masks[3])
        else DirectColorModel(24, masks[0], masks[1], masks[2])
        return BufferedImage(colorModel, raster, false, null)
    }

    fun BufferedImage.undoPremultiply(): BufferedImage {
        val buffer = raster.dataBuffer as DataBufferInt
        val data = buffer.data
        for (i in data.indices) {
            data[i] = data[i].undoPremultiply()
        }
        return this
    }

    fun Texture2D.createFromBufferedImage(image: BufferedImage, callback: Callback<ITexture2D>) {
        width = image.width
        height = image.height
        wasCreated = false
        image.toImage().createTexture(this, checkRedundancy = false, callback)
    }

    fun BufferedImage.toImage(hasAlphaOverride: Boolean? = null): Image {
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
            val hasAlpha = hasAlphaOverride
                ?: (colorModel.hasAlpha() && pixels.any { it.ushr(24) != 255 })
            return IntImage(width, height, pixels, hasAlpha)
        }
    }
}