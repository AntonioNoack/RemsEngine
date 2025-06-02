package me.anno.jvm.images

import me.anno.image.ImageCache
import me.anno.jvm.images.BIImage.toImage
import java.io.IOException
import javax.imageio.ImageIO

object ImageIOImpl {
    fun register() {
        ImageCache.registerStreamReader("png,jpg,gif,bmp,webp") { _, stream ->
            try {
                val img = ImageIO.read(stream)
                if (img != null) Result.success(img.toImage())
                else Result.failure(IOException("ImageIO returned null"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}