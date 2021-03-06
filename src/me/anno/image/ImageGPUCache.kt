package me.anno.image

import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.cache.data.ImageData
import me.anno.cache.data.LateinitTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib
import me.anno.image.raw.GPUImage
import me.anno.io.ResourceHelper
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerFile
import me.anno.utils.Sleep.waitForGFXThread
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import javax.imageio.ImageIO
import kotlin.math.sqrt

object ImageGPUCache : CacheSection("Images") {

    private val LOGGER = LogManager.getLogger(ImageGPUCache::class)

    fun hasImageOrCrashed(file: FileReference, timeout: Long, asyncGenerator: Boolean): Boolean {
        if (file is ImageReadable && file.readImage() is GPUImage) return true
        if (file == InvalidRef) return true
        if (file.isDirectory || !file.exists) return true
        val entry = getEntry(file, timeout, asyncGenerator, ImageGPUCache::generateImageData)
        return when {
            entry == null -> false
            entry !is ImageData -> true
            entry.hasFailed -> true
            entry.texture.isCreated -> true
            else -> false
        }
    }

    fun getImage(file: FileReference, asyncGenerator: Boolean): Texture2D? {
        return getImage(file, 1000L, asyncGenerator)
    }

    fun getImage(file: FileReference, timeout: Long, asyncGenerator: Boolean): Texture2D? {
        if (file == InvalidRef) return null
        if (file is ImageReadable) {
            val image = file.readImage()
            if (image is GPUImage) {
                val texture =
                    image.texture as? Texture2D ?: throw RuntimeException("TODO: Implement handling of ITexture2D")
                return if (!texture.isDestroyed && texture.isCreated) texture else null
            }
        }
        if (file !is InnerFile) {
            if (file.isDirectory || !file.exists) return null
        } else if (file.isDirectory || !file.exists) {
            LOGGER.warn("Image missing: $file")
            return null
        }
        val imageData = getEntry(
            file, timeout, asyncGenerator,
            ImageGPUCache::generateImageData
        ) as? ImageData ?: return null
        val texture = imageData.texture
        if (!imageData.hasFailed && !texture.isCreated && !asyncGenerator) {
            // the texture was forced to be loaded -> wait for it
            waitForGFXThread(true) { texture.isCreated || texture.isDestroyed || imageData.hasFailed }
        }
        return if (texture.isCreated) texture else null
    }

    /*fun getImage(file: File, timeout: Long, asyncGenerator: Boolean): Texture2D? {
        warn("Use FileReference, please; because it is faster when hashing")
        return getImage(getReference(file), timeout, asyncGenerator)
    }*/

    private fun generateImageData(file: FileReference) = ImageData(file)

    fun getInternalTexture(name: String, asyncGenerator: Boolean, timeout: Long = 60_000): Texture2D? {
        val texture = getEntry(name, timeout, asyncGenerator) { name1 ->
            try {
                val img = ImageIO.read(ResourceHelper.loadResource(name1).buffered())
                val texture = Texture2D("internal-'$name1'", img.width, img.height, 1)
                texture.create(img, sync = false, checkRedundancy = true)
                texture
            } catch (e: FileNotFoundException) {
                LOGGER.warn("Internal texture $name1 not found!")
                TextureLib.nullTexture
            } catch (e: Exception) {
                LOGGER.warn("Internal texture $name1 is invalid!")
                e.printStackTrace()
                TextureLib.nullTexture
            }
        } as? Texture2D
        return if (texture?.isCreated == true) texture else null
    }

    fun getLateinitTexture(
        key: Any,
        timeout: Long,
        async: Boolean,
        generator: (callback: (ITexture2D?) -> Unit) -> Unit
    ): LateinitTexture? {
        return getEntry(key, timeout, async) {
            val textureContainer = LateinitTexture()
            generator { textureContainer.texture = it }
            textureContainer
        } as? LateinitTexture
    }

    fun getLateinitTextureLimited(
        key: Any,
        timeout: Long,
        async: Boolean,
        limit: Int,
        generator: (callback: (ITexture2D?) -> Unit) -> Unit
    ): LateinitTexture? {
        return getEntryLimited(key, timeout, async, limit) {
            val textureContainer = LateinitTexture()
            generator { textureContainer.texture = it }
            textureContainer
        } as? LateinitTexture
    }

    fun getLUT(file: FileReference, asyncGenerator: Boolean, timeout: Long = 5000): Texture3D? {
        val texture = getEntry("LUT" to file, timeout, asyncGenerator, ImageGPUCache::generateLUT) as? Texture3D
        return if (texture?.isCreated == true) texture else null
    }

    private fun generateLUT(pair: Pair<String, FileReference>): ICacheData {
        val file = pair.second
        val img = ImageCPUCache.getImage(file, false)!!
        val sqrt = sqrt(img.width + 0.5f).toInt()
        val tex = Texture3D("lut-${file.name}", sqrt, img.height, sqrt)
        tex.create(img, false)
        return tex
    }

}