package me.anno.jvm.images

import me.anno.image.Image
import me.anno.image.ImageStreamWriter
import me.anno.image.raw.IFloatImage
import me.anno.jvm.images.BIImage.createBufferedImage
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

object ImageWriterImpl {

    fun register() {
        Image.writeImageImpl = ImageStreamWriter(ImageWriterImpl::writeImage)
    }

    private val LOGGER = LogManager.getLogger(ImageWriterImpl::class)
    fun writeImage(image: Image, output: OutputStream, format: String, quality: Float) {

        if (format.equals("hdr", true) && image is IFloatImage && image.numChannels in 1..3) {
            Image.writeHDR(image, output)
            return
        }

        val tryJPG = isLossyFormat(format)
        val bImage = image.createBufferedImage(image.hasAlphaChannel && !tryJPG)
        if (tryJPG && tryWritingJPG(bImage, output, quality)) {
            return
        }

        if (!ImageIO.write(bImage, format, output)) {
            LOGGER.warn("Couldn't find writer for $format")
        }
    }

    private fun isLossyFormat(format: String): Boolean {
        return format.equals("jpg", true) || format.equals("jpeg", true)
    }

    private fun tryWritingJPG(bImage: BufferedImage, output: OutputStream, quality: Float): Boolean {
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (!writers.hasNext()) return false
        val writer = writers.next()
        val params = writer.defaultWriteParam
        params.compressionMode = ImageWriteParam.MODE_EXPLICIT
        params.compressionQuality = quality
        writer.output = MemoryCacheImageOutputStream(output)
        val outputImage = IIOImage(bImage, null, null)
        writer.write(null, outputImage, params)
        writer.dispose()
        return true
    }
}