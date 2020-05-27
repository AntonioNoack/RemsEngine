package me.anno.objects.cache

import me.anno.gpu.texture.Texture2D
import java.io.File
import javax.imageio.ImageIO

class ImageData(file: File): CacheData {

    val texture = Texture2D(ImageIO.read(file))
    override fun destroy() {
        texture.destroy()
    }

    companion object {
        val cache = HashMap<File, ImageData>()
        fun getImage(file: File): ImageData {
            synchronized(cache){
                val cached = cache[file]
                if(cached != null) return cached
                val image = ImageData(file)
                cache[file] = image
                return image
            }
        }
    }

}