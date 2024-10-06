package me.anno.jvm.images

import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.async.Callback
import java.io.InputStream
import javax.imageio.ImageIO

object ImageIOImpl {

    fun register() {
        ImageCache.registerStreamReader("png,jpg,gif,bmp,webp") { it, callback ->
            tryImageIO(it, callback)
        }
    }

    private fun tryImageIO(it: InputStream, callback: Callback<Image>) {
        try {
            val img = ImageIO.read(it)
            if (img != null) {
                callback.ok(img.toImage())
            } else callback.err(null)
        } catch (e: Exception) {
            callback.err(e)
        }
    }
}