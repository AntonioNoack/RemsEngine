package me.anno.image

import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.structures.Callback
import org.apache.commons.imaging.Imaging
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.imageio.ImageIO

object ImageImpl {

    fun register() {
        ImageCache.registerStreamReader("png,jpg,gif,bmp,webp") { it, callback ->
            tryImageIO(it, callback)
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