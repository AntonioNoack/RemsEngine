package me.anno.jvm.images

import me.anno.gpu.GFX
import me.anno.gpu.texture.Redundancy.checkRedundancyX4
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.image.raw.GPUImage
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.utils.Color.convertARGB2ABGR
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
        return when (this) {
            is GPUImage -> texture
                .createImage(false, hasAlphaChannel)
                .createBufferedImage()
            else -> {
                val width = width
                val height = height
                val result = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
                val dataBuffer = result.raster.dataBuffer as DataBufferInt
                val dst = dataBuffer.data
                when (this) {
                    is IntImage -> {
                        for (y in 0 until height) {
                            val srcI = getIndex(0, y)
                            data.copyInto(dst, y * width, srcI, srcI + width)
                        }
                    }
                    else -> {
                        var i = 0
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                dst[i++] = getRGB(x, y)
                            }
                        }
                    }
                }
                result
            }
        }
    }

    fun Texture2D.createFromBufferedImage(
        image: BufferedImage,
        sync: Boolean,
        checkRedundancy: Boolean
    ): (() -> Unit)? {

        width = image.width
        height = image.height
        wasCreated = false

        // use the type to correctly create the image
        val buffer = image.data.dataBuffer
        when (image.type) {
            BufferedImage.TYPE_INT_ARGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && GFX.isGFXThread()) {
                    createBGRA(data, checkRedundancy)
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancyX4(data) else data
                    convertARGB2ABGR(data2)
                    return {
                        createRGBA(data2, checkRedundancy)
                    }
                }
            }
            BufferedImage.TYPE_INT_RGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && GFX.isGFXThread()) {
                    createBGR(data, checkRedundancy)
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancyX4(data) else data
                    convertARGB2ABGR(data2)
                    return {
                        createRGB(data2, false)
                    }
                }
            }
            else -> throw NotImplementedError("BufferedImage.type[${image.type}]")
        }
    }

    fun BufferedImage.toImage(): Image {
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
            return ByteImage(width, height, ByteImage.Format.R, bytes)
        } else {
            val pixels = getRGB(0, 0, width, height, null, 0, width)
            val hasAlpha = colorModel.hasAlpha() && pixels.any { it.ushr(24) != 255 }
            return IntImage(width, height, pixels, hasAlpha)
        }
    }
}