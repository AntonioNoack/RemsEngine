package me.anno.jvm.images

import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.jvm.images.BIImage.createBufferedImage
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.structures.Callback
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

object ImageImpl {

    fun register() {
        Image.writeImageImpl = ImageImpl::writeImage
        for (signature in listOf("png", "jpg", "gif", "bmp", "webp")) { // todo is there more supported signatures?
            ImageCache.registerStreamReader(signature) { it, callback ->
                tryImageIO(it, callback)
            }
        }
    }

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

    private fun tryImageIO(it: InputStream, callback: Callback<Image>) {
        val bytes = it.use { it.readBytes() }
        tryImageIO(bytes) { img, _ ->
            if (img != null) callback.ok(img)
            else tryImaging(bytes, callback)
        }
    }

    private fun tryImageIO(bytes: ByteArray, callback: Callback<Image>) {
        try {
            val img = ImageIO.read(ByteArrayInputStream(bytes))
            if (img != null) {
                callback.ok(img.toImage())
            } else callback.err(null)
        } catch (e: Exception) {
            callback.err(e)
        }
    }

    private fun tryImaging(bytes: ByteArray, callback: Callback<Image>) {
        try {
            val img = Imaging.getBufferedImage(ByteArrayInputStream(bytes))
            callback.call(img.toImage(), null)
        } catch (e: Exception) {
            callback.err(e)
        }
    }
}