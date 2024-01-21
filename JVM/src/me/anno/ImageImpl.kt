package me.anno

import me.anno.image.Image
import me.anno.image.raw.createBufferedImage
import org.apache.logging.log4j.LogManager
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

object ImageImpl {
    private val LOGGER = LogManager.getLogger(ImageImpl::class)
    fun writeImage(self: Image, dst: OutputStream, format: String, quality: Float) {
        val image = self.createBufferedImage()
        if (format.equals("jpg", true) || format.equals("jpeg", true)) {
            val writers = ImageIO.getImageWritersByFormatName("jpg")
            if (writers.hasNext()) {
                val writer = writers.next()
                val params = writer.defaultWriteParam
                params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                params.compressionQuality = quality
                writer.output = MemoryCacheImageOutputStream(dst)
                val outputImage = IIOImage(image, null, null)
                writer.write(null, outputImage, params)
                writer.dispose()
                return
            }
        }

        if (!ImageIO.write(image, format, dst)) {
            LOGGER.warn("Couldn't find writer for $format")
        }
    }
}