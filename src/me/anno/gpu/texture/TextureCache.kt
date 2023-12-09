package me.anno.gpu.texture

import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.raw.GPUImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.utils.OS
import me.anno.utils.Sleep
import org.apache.logging.log4j.LogManager
import kotlin.math.sqrt

/**
 * Use this to load textures (asynchronously if possible).
 * Textures are equivalent to Images on the CPU, just on the GPU.
 * */
object TextureCache : CacheSection("Texture") {

    private val LOGGER = LogManager.getLogger(TextureCache::class)

    fun hasImageOrCrashed(file: FileReference, timeout: Long, asyncGenerator: Boolean): Boolean {
        if (file is ImageReadable && file.hasInstantGPUImage()) return true
        if (file == InvalidRef) return true
        if (file.isDirectory || !file.exists) return true
        val entry = try {
            getEntry(file, timeout, asyncGenerator, TextureCache::generateImageData)
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
        return when {
            entry == null -> false
            entry !is ImageToTexture -> true
            entry.hasFailed -> true
            entry.texture?.isCreated == true -> true
            else -> false
        }
    }

    operator fun get(file: FileReference, asyncGenerator: Boolean): Texture2D? {
        return get(file, 1000L, asyncGenerator)
    }

    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): Texture2D? {
        if (file == InvalidRef) return null
        if (file is ImageReadable) {
            val image = file.readGPUImage()
            if (image is GPUImage) {
                val texture = image.texture as? Texture2D
                    ?: throw RuntimeException("TODO: Implement handling of ITexture2D")
                if (texture is TextureLib.IndestructibleTexture2D) texture.ensureExists()
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
            TextureCache::generateImageData
        ) as? ImageToTexture ?: return null
        if (!imageData.hasFailed && imageData.texture?.isCreated != true && !asyncGenerator && !OS.isWeb) {
            // the texture was forced to be loaded -> wait for it
            Sleep.waitForGFXThread(true) {
                val texture = imageData.texture
                (texture != null && (texture.isCreated || texture.isDestroyed)) || imageData.hasFailed
            }
        }
        val texture = imageData.texture
        return if (texture != null && texture.isCreated) texture else null
    }

    private fun generateImageData(file: FileReference) = ImageToTexture(file)

    fun getLateinitTexture(
        key: Any, timeout: Long, async: Boolean,
        generator: (callback: (ITexture2D?) -> Unit) -> Unit
    ): LateinitTexture? {
        return getEntry(key, timeout, async) {
            val textureContainer = LateinitTexture()
            generator { textureContainer.texture = it }
            textureContainer
        } as? LateinitTexture
    }

    fun getLateinitTextureLimited(
        key: Any, timeout: Long, async: Boolean, limit: Int,
        generator: (callback: (ITexture2D?) -> Unit) -> Unit
    ): LateinitTexture? {
        return getEntryLimited(key, timeout, async, limit) {
            val textureContainer = LateinitTexture()
            generator { textureContainer.texture = it }
            textureContainer
        } as? LateinitTexture
    }

    fun getLUT(file: FileReference, asyncGenerator: Boolean, timeout: Long = 5000): Texture3D? {
        val texture = getEntry("LUT" to file, timeout, asyncGenerator, TextureCache::generateLUT) as? Texture3D
        return if (texture?.isCreated == true) texture else null
    }

    private fun generateLUT(pair: Pair<String, FileReference>): ICacheData {
        val file = pair.second
        val img = ImageCache[file, false]!!
        val sqrt = sqrt(img.width + 0.5f).toInt()
        val tex = Texture3D("lut-${file.name}", sqrt, img.height, sqrt)
        tex.create(img, false)
        return tex
    }
}