package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.ImageData
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase1
import me.anno.gpu.TextureLib
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import javax.imageio.ImageIO
import kotlin.math.sqrt

object ImageCache: CacheSection("Images"){

    private val LOGGER = LogManager.getLogger(ImageCache::class)

    fun getImage(file: File, timeout: Long, asyncGenerator: Boolean): Texture2D? {
        val texture = if (file.isDirectory || !file.exists()) null
        else (getEntry(file as Any, timeout, asyncGenerator) {
            ImageData(file)
        } as? ImageData)?.texture
        return if(texture?.isCreated == true) texture else null
    }

    fun getInternalTexture(name: String, asyncGenerator: Boolean, timeout: Long = 60_000): Texture2D? {
        val texture = getEntry("Texture", name, 0, timeout, asyncGenerator) {
            try {
                val img = GFXBase1.loadAssetsImage(name)
                val tex = Texture2D("internal-texture", img.width, img.height, 1)
                tex.create(img, false)
                tex
            } catch (e: FileNotFoundException) {
                LOGGER.warn("Internal texture $name not found!")
                TextureLib.nullTexture
            } catch (e: Exception) {
                LOGGER.warn("Internal texture $name is invalid!")
                e.printStackTrace()
                TextureLib.nullTexture
            }
        } as? Texture2D
        return if(texture?.isCreated == true) texture else null
    }

    fun getLUT(file: File, asyncGenerator: Boolean, timeout: Long = 5000): Texture3D? {
        val texture = getEntry("LUT", file.toString(), 0, timeout, asyncGenerator) {
            val img = ImageIO.read(file)
            val sqrt = sqrt(img.width + 0.5f).toInt()
            val tex = Texture3D(sqrt, img.height, sqrt)
            tex.create(img, false)
            tex
        } as? Texture3D
        return if(texture?.isCreated == true) texture else null
    }

}