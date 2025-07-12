package me.anno.gpu.texture

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.raw.GPUImage
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.temporary.InnerTmpImageFile
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.mapCallback
import org.apache.logging.log4j.LogManager
import org.joml.Vector2i
import kotlin.math.sqrt

/**
 * Use this to load textures (asynchronously if possible).
 * Textures are equivalent to Images on the CPU, just on the GPU.
 * */
object TextureCache {

    private val textures = CacheSection<FileKey, ITexture2D>("Texture")
    private val textureLUTs = CacheSection<FileKey, Texture3D>("Texture3D")
    private val textureArrays1 = CacheSection<ArrayKey, Texture2DArray>("TextureArray")
    private val textureArrays2 = CacheSection<FileTriple<Vector2i>, Texture2DArray>("TextureArray")

    private val LOGGER = LogManager.getLogger(TextureCache::class)

    fun clear() {
        textures.clear()
        textureLUTs.clear()
        textureArrays1.clear()
        textureArrays2.clear()
    }

    var timeoutMillis = 10_000L

    fun hasImageOrCrashed(file: FileReference, timeout: Long): Boolean {
        if (file is ImageReadable && file.hasInstantGPUImage()) return true
        if (file == InvalidRef) return true
        if (file.isDirectory || !file.exists) return true
        val entry = try {
            textures.getFileEntry(file, false, timeout, ::generateTexture)
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
        return when {
            entry.hasValue && entry.value == null -> true
            entry.value?.isCreated() == true -> true
            else -> false
        }
    }

    operator fun get(file: FileReference): AsyncCacheData<ITexture2D> {
        return get(file, timeoutMillis)
    }

    operator fun get(file: FileReference, timeout: Long): AsyncCacheData<ITexture2D> {
        if (file == InvalidRef) return AsyncCacheData.empty()
        if (file !is InnerFile) {
            if (file.isDirectory || !file.exists) return AsyncCacheData.empty()
        } else if (file.isDirectory || !file.exists) {
            LOGGER.warn("Image missing: $file")
            return AsyncCacheData.empty()
        } else if (file is InnerTmpImageFile && file.image is GPUImage) {
            return AsyncCacheData(file.image.texture) // shortcut
        }
        return textures.getFileEntry(file, false, timeout, ::generateTexture)
    }

    private fun generateTexture(file: FileKey, result: AsyncCacheData<ITexture2D>) =
        TextureReader(file.file, result)

    private data class FileTriple<V>(val file: FileReference, val lastModified: Long, val type: V) {
        constructor(file: FileReference, type: V) : this(file, file.lastModified, type)
    }

    fun getLUT(file: FileReference, timeoutMillis: Long = TextureCache.timeoutMillis): Texture3D? {
        val texture = textureLUTs.getFileEntry(file, false, timeoutMillis, TextureCache::generateLUT)
        return texture.value?.createdOrNull() as? Texture3D
    }

    private fun generateLUT(key: FileKey, result: AsyncCacheData<Texture3D>) {
        ImageCache[key.file, timeoutMillis].mapResult(result) { img ->
            createLUT(key.file, img)
        }
    }

    private fun createLUT(file: FileReference, img: Image): Texture3D {
        val size = sqrt(img.width + 0.5f).toInt()
        return Texture3D("lut-${file.name}", size, img.height, size).create(img)
    }

    fun getTextureArray(
        file: FileReference, numTiles: Vector2i,
        timeoutMillis: Long = TextureCache.timeoutMillis
    ): Texture2DArray? {
        return getTextureArrayEntry(file, numTiles, timeoutMillis)
            .waitFor()?.createdOrNull() as? Texture2DArray
    }

    fun getTextureArrayEntry(
        file: FileReference, numTiles: Vector2i,
        timeoutMillis: Long = TextureCache.timeoutMillis
    ): AsyncCacheData<Texture2DArray> {
        val key = FileTriple(file, numTiles)
        return textureArrays2.getEntry(key, timeoutMillis, TextureCache::generateTextureArray)
    }

    private data class ArrayKey(val files: List<FileReference>, val width: Int, val height: Int) {
        override fun toString(): String {
            return "Array[${files.map { it.nameWithoutExtension }},$width x $height]"
        }
    }

    fun getTextureArray(
        file: List<FileReference>, width: Int, height: Int,
        timeoutMillis: Long = TextureCache.timeoutMillis
    ): Texture2DArray? {
        val key = ArrayKey(file, width, height)
        val texture = textureArrays1.getEntry(key, timeoutMillis, TextureCache::generateTextureArray)
        return texture.value?.createdOrNull() as? Texture2DArray
    }

    private fun generateTextureArray(key: FileTriple<Vector2i>, result: AsyncCacheData<Texture2DArray>) {
        val file = key.file
        ImageCache[file, timeoutMillis].mapResult(result) { img ->
            createTextureArray(file, img, key.type)
        }
    }

    private fun generateTextureArray(key: ArrayKey, result: AsyncCacheData<Texture2DArray>) {
        key.files.mapCallback({ _, file, cb ->
            ImageCache[file, timeoutMillis].waitFor(cb)
        }, result.map { images ->
            val images2 = images.map { image -> image.resampled(key.width, key.height) }
            val texture = Texture2DArray("spite-$key", key.width, key.height, images2.size)
            texture.create(images2, false)
        })
    }

    private fun createTextureArray(file: FileReference, img: Image, numTiles: Vector2i): Texture2DArray {
        return Texture2DArray(
            "spite-${file.name}-$numTiles",
            img.width / numTiles.x, img.height / numTiles.y,
            numTiles.x * numTiles.y
        ).create(img.split(numTiles.x, numTiles.y), false)
    }
}