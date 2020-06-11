package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.HDRImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class ImageData(file: File): CacheData {

    val texture = Texture2D(1024,1024)

    init {
        if(file.name.endsWith(".hdr", true)){
            thread {
                val img = HDRImage(file)
                val w = img.width
                val h = img.height
                val pixels = img.pixelArray
                GFX.addTask {
                    texture.w = w
                    texture.h = h
                    texture.create(pixels)
                    10
                }
            }
        } else {
            texture.create {
                ImageIO.read(file) ?: throw IOException("Format of $file is not supported.")
            }
        }
    }

    override fun destroy() {
        texture.destroy()
    }

}