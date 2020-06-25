package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.HDRImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class ImageData(file: File): CacheData {

    val texture = Texture2D(1024,1024,1)

    init {
        if(file.name.endsWith(".hdr", true)){
            thread {
                val img = HDRImage(file, true)
                val w = img.width
                val h = img.height
                val pixels = img.pixelBuffer
                GFX.addGPUTask {
                    texture.setSize(w, h)
                    texture.create(pixels)
                    35
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