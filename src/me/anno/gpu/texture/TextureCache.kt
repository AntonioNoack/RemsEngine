package me.anno.gpu.texture

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.IgnoredException
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.raw.GPUImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.temporary.InnerTmpImageFile
import me.anno.utils.OS
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import org.apache.logging.log4j.LogManager
import org.joml.Vector2i
import kotlin.math.sqrt

/**
 * Use this to load textures (asynchronously if possible).
 * Textures are equivalent to Images on the CPU, just on the GPU.
 * */
object TextureCache : CacheSection("Texture") {

    private val LOGGER = LogManager.getLogger(TextureCache::class)

    var timeoutMillis = 10_000L

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
        return get(file, timeoutMillis, asyncGenerator)
    }

    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): ITexture2D? {
        if (file == InvalidRef) return null
        if (file !is InnerFile) {
            if (file.isDirectory || !file.exists) return null
        } else if (file.isDirectory || !file.exists) {
            LOGGER.warn("Image missing: $file")
            return null
        } else if (file is InnerTmpImageFile && file.image is GPUImage) {
            return file.image.texture // shortcut
        }
        val imageData = getFileEntry(file, false, timeout, asyncGenerator) { fileI, _ ->
            generateImageData(fileI)
        }
        return if (imageData != null) {
            if (!asyncGenerator && !OS.isWeb) {
                // the texture was forced to be loaded -> wait for it
                imageData.waitFor()
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

    fun <V> getLateinitTextureLimited(
        key: V, timeout: Long, async: Boolean, limit: Int,
        generator: (key: V, callback: Callback<ITexture2D>) -> Unit
    ): LateinitTexture? {
        val entry = getEntryLimited(key, timeout, async, limit) {
            val tex = LateinitTexture()
            generator(key) { result, exc ->
                tex.value = result
                if (exc != null && exc !is IgnoredException) {
                    exc.printStackTrace()
                }
            }
            tex
        }
        if (!async) entry?.waitFor()
        return entry
    }

    private data class FileTriple<V>(val file: FileReference, val lastModified: Long, val type: V) {
        constructor(file: FileReference, type: V) : this(file, file.lastModified, type)
    }

    fun getLUT(file: FileReference, timeoutMillis: Long = TextureCache.timeoutMillis): Texture3D? {
        val key = FileTriple(getReference(file.absolutePath), "LUT")
        val texture = getEntry(key, timeoutMillis, true, TextureCache::generateLUT)
        return texture?.value?.createdOrNull() as? Texture3D
    }

    private fun generateLUT(key: FileTriple<*>): AsyncCacheData<Texture3D> {
        val file = key.file
        val result = AsyncCacheData<Texture3D>()
        ImageCache.getAsync(file, timeoutMillis, true, result.map { img ->
            createLUT(file, img)
        })
        return result
    }

    private fun createLUT(file: FileReference, img: Image): Texture3D {
        val size = sqrt(img.width + 0.5f).toInt()
        return Texture3D("lut-${file.name}", size, img.height, size).create(img)
    }

    fun getTextureArray(
        file: FileReference, numTiles: Vector2i,
        timeoutMillis: Long = TextureCache.timeoutMillis
    ): Texture2DArray? {
        val key = FileTriple(file, numTiles)
        val texture = getEntry(key, timeoutMillis, true, TextureCache::generateTextureArray)
        return texture?.value?.createdOrNull() as? Texture2DArray
    }

    private fun generateTextureArray(key: FileTriple<Vector2i>): AsyncCacheData<Texture2DArray> {
        val file = key.file
        val result = AsyncCacheData<Texture2DArray>()
        ImageCache.getAsync(file, timeoutMillis, true, result.map { img ->
            createTextureArray(file, img, key.type)
        })
        return result
    }

    private fun createTextureArray(file: FileReference, img: Image, numTiles: Vector2i): Texture2DArray {
        return Texture2DArray(
            "spite-${file.name}-$numTiles",
            img.width / numTiles.x, img.height / numTiles.y,
            numTiles.x * numTiles.y
        ).create(img.split(numTiles.x, numTiles.y), false)
    }
}