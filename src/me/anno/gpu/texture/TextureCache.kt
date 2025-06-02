package me.anno.gpu.texture

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
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
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.deferredToValue
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

    /**
     * Asynchronously load texture & return whether loading them is complete
     * */
    @Deprecated(USE_COROUTINES_INSTEAD)
    fun loadTextureGetIsFinished(file: FileReference, timeout: Long): Boolean {
        if (file is ImageReadable && file.hasInstantGPUImage()) return true
        if (file == InvalidRef) return true
        if (file.isDirectory || !file.exists) return true
        val entry = getX(file, timeout)
        return !entry.isActive
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    operator fun get(file: FileReference, asyncGenerator: Boolean): ITexture2D? {
        return get(file, timeoutMillis, asyncGenerator)
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): ITexture2D? {
        val imageData = deferredToValue(getX(file, timeout), asyncGenerator)
        return if (imageData != null) {
            imageData.createdOrNull()
        } else {
            if (!asyncGenerator) {
                LOGGER.warn("Couldn't load $file, probably a folder")
            }
            null
        }
    }

    fun getX(file: FileReference, timeout: Long): Deferred<Result<ITexture2D>> {
        if (file == InvalidRef) return failedDeferred
        if (file !is InnerFile) {
            if (file.isDirectory || !file.exists) return failedDeferred
        } else if (file.isDirectory || !file.exists) {
            LOGGER.warn("Image missing: $file")
            return failedDeferred
        } else if (file is InnerTmpImageFile && file.image is GPUImage) {
            return CompletableDeferred(Result.success(file.image.texture)) // shortcut
        }
        return getFileEntryX(file, false, timeout) { fileI, _ ->
            generateImageData(fileI)
        }
    }

    private val failedDeferred = CompletableDeferred(Result.failure<ITexture2D>(IgnoredException()))

    private suspend fun generateImageData(file: FileReference) = TextureReader.read(file)

    @Deprecated(USE_COROUTINES_INSTEAD)
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

    @Deprecated(USE_COROUTINES_INSTEAD)
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

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun getLUT(file: FileReference, timeoutMillis: Long = TextureCache.timeoutMillis): Texture3D? {
        val key = FileTriple(getReference(file.absolutePath), "LUT")
        val texture = getEntry(key, timeoutMillis, true, TextureCache::generateLUT)
        return texture?.value?.createdOrNull() as? Texture3D
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    private fun generateLUT(key: FileTriple<*>): AsyncCacheData<Texture3D> {
        val file = key.file
        val result = AsyncCacheData<Texture3D>()
        ImageCache.getAsync(file, timeoutMillis, result.map { img ->
            createLUT(file, img)
        })
        return result
    }

    private fun createLUT(file: FileReference, img: Image): Texture3D {
        val size = sqrt(img.width + 0.5f).toInt()
        return Texture3D("lut-${file.name}", size, img.height, size).create(img)
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun getTextureArray(
        file: FileReference, numTiles: Vector2i,
        timeoutMillis: Long = TextureCache.timeoutMillis
    ): Texture2DArray? {
        val key = FileTriple(file, numTiles)
        val texture = getEntry(key, timeoutMillis, true, TextureCache::generateTextureArray)
        return texture?.value?.createdOrNull() as? Texture2DArray
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    private fun generateTextureArray(key: FileTriple<Vector2i>): AsyncCacheData<Texture2DArray> {
        val file = key.file
        val result = AsyncCacheData<Texture2DArray>()
        ImageCache.getAsync(file, timeoutMillis, result.map { img ->
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