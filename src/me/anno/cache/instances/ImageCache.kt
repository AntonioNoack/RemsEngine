package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.cache.data.ImageData
import me.anno.gpu.GFXBase1
import me.anno.gpu.TextureLib
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.studio.StudioBase.Companion.warn
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import javax.imageio.ImageIO
import kotlin.math.sqrt

object ImageCache : CacheSection("Images") {

    private val LOGGER = LogManager.getLogger(ImageCache::class)

    fun getImage(file: FileReference, timeout: Long, asyncGenerator: Boolean): Texture2D? {
        if (file == InvalidRef) return null
        val meta = LastModifiedCache[file]
        if (meta.isDirectory || !meta.exists) return null
        val texture = (getEntry(file, timeout, asyncGenerator, ::generateImageData) as? ImageData)?.texture
        return if (texture?.isCreated == true) texture else null
    }

    fun getImage(file: File, timeout: Long, asyncGenerator: Boolean): Texture2D? {
        warn("Use FileReference, please; because it is faster when hashing")
        return getImage(getReference(file), timeout, asyncGenerator)
    }

    private fun generateImageData(file: FileReference) = ImageData(file)

    fun getInternalTexture(name: String, asyncGenerator: Boolean, timeout: Long = 60_000): Texture2D? {
        val texture = getEntry(name, timeout, asyncGenerator, ::generateInternalTexture) as? Texture2D
        return if (texture?.isCreated == true) texture else null
    }

    private fun generateInternalTexture(name: String): ICacheData {
        return try {
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
    }

    fun getLUT(file: FileReference, asyncGenerator: Boolean, timeout: Long = 5000): Texture3D? {
        val texture = getEntry("LUT" to file, timeout, asyncGenerator, ::generateLUT) as? Texture3D
        return if (texture?.isCreated == true) texture else null
    }

    private fun generateLUT(pair: Pair<String, FileReference>): ICacheData {
        val file = pair.second
        val img = ImageIO.read(file.inputStream())
        val sqrt = sqrt(img.width + 0.5f).toInt()
        val tex = Texture3D(sqrt, img.height, sqrt)
        tex.create(img, false)
        return tex
    }

}