package me.anno.gpu.texture

import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.cache.IgnoredException
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.utils.OS
import me.anno.utils.structures.Callback
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
            getFileEntry(file, false, timeout, asyncGenerator) { it, _ ->
                generateImageData(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
        return when {
            entry == null -> false
            entry.hasValue && entry.value == null -> true
            entry.value?.wasCreated == true -> true
            else -> false
        }
    }

    operator fun get(file: FileReference, asyncGenerator: Boolean): ITexture2D? {
        return get(file, 1000L, asyncGenerator)
    }

    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): ITexture2D? {
        if (file == InvalidRef) return null
        if (file !is InnerFile) {
            if (file.isDirectory || !file.exists) return null
        } else if (file.isDirectory || !file.exists) {
            LOGGER.warn("Image missing: $file")
            return null
        }
        val imageData = getFileEntry(file, false, timeout, asyncGenerator) { fileI, _ ->
            generateImageData(fileI)
        }
        return if (imageData != null) {
            if (!asyncGenerator && !OS.isWeb) {
                // the texture was forced to be loaded -> wait for it
                imageData.waitForGFX()
            }
            val texture = imageData.value
            if (texture != null && texture.isCreated()) texture else null
        } else {
            if (!asyncGenerator) {
                LOGGER.warn("Couldn't load $file, probably a folder")
            }
            null
        }
    }

    private fun generateImageData(file: FileReference) = TextureReader(file)

    fun getLateinitTexture(
        key: Any, timeout: Long, async: Boolean,
        generator: (callback: Callback<ITexture2D>) -> Unit
    ): LateinitTexture? {
        return getEntry(key, timeout, async) {
            val textureContainer = LateinitTexture()
            generator { v, err ->
                textureContainer.value = v
                err?.printStackTrace()
            }
            textureContainer
        }
    }

    fun getLateinitTextureLimited(
        key: Any, timeout: Long, async: Boolean, limit: Int,
        generator: (callback: Callback<ITexture2D>) -> Unit
    ): LateinitTexture? {
        val entry = getEntryLimited(key, timeout, async, limit) {
            val tex = LateinitTexture()
            generator { result, exc ->
                tex.value = result
                if (exc != null && exc !is IgnoredException) {
                    exc.printStackTrace()
                }
            }
            tex
        }
        if (!async) entry?.waitForGFX()
        return entry
    }

    @Suppress("unused") // used in Rem's Studio
    fun getLUT(file: FileReference, asyncGenerator: Boolean, timeout: Long = 5000): Texture3D? {
        val texture = getEntry("LUT" to file, timeout, asyncGenerator, TextureCache::generateLUT) as? Texture3D
        return if (texture?.wasCreated == true) texture else null
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