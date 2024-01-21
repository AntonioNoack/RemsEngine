package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.io.OutputStream
import javax.imageio.ImageIO

// todo move this to JVMPlugin

private val LOGGER = LogManager.getLogger("BufferedImage")

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

fun BufferedImage.write(dst: FileReference) {
    val format = dst.lcExtension
    dst.outputStream().use { out: OutputStream ->
        if (!ImageIO.write(this, format, out)) {
            LOGGER.warn("Couldn't find writer for $format")
        }
    }
}

fun Texture2D.createFromBufferedImage(image: BufferedImage, sync: Boolean, checkRedundancy: Boolean): (() -> Unit)? {

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
                val data2 = if (checkRedundancy) checkRedundancy(data) else data
                Texture2D.switchRGB2BGR(data2)
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
                val data2 = if (checkRedundancy) checkRedundancy(data) else data
                Texture2D.switchRGB2BGR(data2)
                return {
                    createRGB(data2, false)
                }
            }
        }

        BufferedImage.TYPE_INT_BGR -> {
            buffer as DataBufferInt
            val data = buffer.data
            // data is already in the correct format; no swizzling needed
            if (sync && GFX.isGFXThread()) {
                createRGB(data, checkRedundancy)
                return null
            } else {
                val data2 = if (checkRedundancy) checkRedundancy(data) else data
                return {
                    createRGB(data2, false)
                }
            }
        }

        BufferedImage.TYPE_BYTE_GRAY -> {
            buffer as DataBufferByte
            val data = buffer.data
            // data is already in the correct format; no swizzling needed
            if (sync && GFX.isGFXThread()) {
                createMonochrome(data, checkRedundancy)
                return null
            } else {
                val data2 = if (checkRedundancy) checkRedundancy(data) else data
                return {
                    createMonochrome(data2, false)
                }
            }
        }

        else -> {
            val data = image.getRGB(0, 0, width, height, Texture2D.intArrayPool[width * height, false, false], 0, width)
            val hasAlpha = image.colorModel.hasAlpha()
            if (!hasAlpha) {
                // ensure opacity
                if (sync && GFX.isGFXThread()) {
                    createBGR(data, checkRedundancy)
                    Texture2D.intArrayPool.returnBuffer(data)
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    Texture2D.switchRGB2BGR(data2)
                    val buffer2 = Texture2D.bufferPool[data2.size * 4, false, false]
                    val buffer2i = buffer2.asIntBuffer()
                    buffer2i.put(data2)
                    buffer2i.flip()
                    return {
                        createRGB(buffer2i, checkRedundancy)
                        Texture2D.intArrayPool.returnBuffer(data)
                    }
                }
            } else {
                if (sync && GFX.isGFXThread()) {
                    createBGRA(data, checkRedundancy)
                    Texture2D.intArrayPool.returnBuffer(data)
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    Texture2D.switchRGB2BGR(data2)
                    val buffer2 = Texture2D.bufferPool[data2.size * 4, false, false]
                    buffer2.asIntBuffer().put(data2)
                    buffer2.position(0)
                    return {
                        createRGBA(buffer2, false)
                        Texture2D.intArrayPool.returnBuffer(data)
                    }
                }
            }
        }
    }
}

fun Image.createBufferedImage(): BufferedImage {
    when (this) {
        is GPUImage -> {
            return texture.createImage(false, hasAlphaChannel)
                .createBufferedImage()
        }
        is CachedImage -> {
            return base!!.createBufferedImage()
        }
        else -> {}
    }
    val width = width
    val height = height
    val result = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
    val dataBuffer = result.raster.dataBuffer as DataBufferInt
    val dst = dataBuffer.data
    when (this) {
        is IntImage -> {
            data.copyInto(dst)
        }

        else -> {
            var i = 0
            val size = width * height
            while (i < size) {
                dst[i] = getRGB(i)
                i++
            }
        }
    }
    return result
}

fun Image.createBufferedImage(dstWidth: Int, dstHeight: Int, allowUpscaling: Boolean): BufferedImage {
    return resized(dstWidth, dstHeight, allowUpscaling).createBufferedImage()
}
