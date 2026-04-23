package me.anno.jvm.images

import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.async.Callback
import java.io.InputStream
import javax.imageio.ImageIO

object ImageIOImpl {

    fun register() {
        ImageCache.registerStreamReader("png,jpg,gif,bmp,webp,tif") { _, stream, callback ->
            tryImageIO(stream, callback)
        }
    }

    private fun tryImageIO(stream: InputStream, callback: Callback<Image>) {
        try {
            val image = ImageIO.read(stream)
            if (image != null) {
                callback.ok(image.toImage())
            } else callback.err(null)
        } catch (e: Exception) {
            callback.err(e)
        }
    }
}