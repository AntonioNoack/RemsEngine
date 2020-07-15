package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.HDRImage
import org.apache.commons.imaging.Imaging
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class ImageData(file: File): CacheData {

    val texture = Texture2D(1024,1024,1)

    init {
        val fileExtension = file.extension
        when(fileExtension.toLowerCase()){
            "hdr" -> {
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
            }
            "png", "jpg", "jpeg" -> {
                texture.create {
                    ImageIO.read(file) ?: throw IOException("Format of $file is not supported.")
                }
            }
            // todo support webp here...
            else -> {
                texture.create {
                    Imaging.getBufferedImage(file) ?: throw IOException("Format of $file is not supported.")
                }
            }
        }
        /*if(file.name.endsWith(".hdr", true)){

        } else if(file.name.endsWith(".ico") || file.name.endsWith("")){
            texture.create {
                ImageIO.read(file) ?: throw IOException("Format of $file is not supported.")
            }
        }*/
    }

    override fun destroy() {
        texture.destroy()
    }

}