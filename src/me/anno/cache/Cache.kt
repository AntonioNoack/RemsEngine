package me.anno.cache

import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.TextureLib
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.video.FFMPEGMetadata
import me.anno.video.VFrame
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object Cache {

    private val LOGGER = LogManager.getLogger(Cache::class)

    private val cache = HashMap<Any, CacheEntry>()
    private val lockedKeys = HashSet<Any>(2048)

    fun clear() {
        synchronized(cache) {
            cache.values.forEach { it.destroy() }
            cache.clear()
            lockedKeys.clear() // mmh...
        }
    }

    fun remove(filter: (Map.Entry<Any, CacheEntry>) -> Boolean) {
        synchronized(cache) {
            val toRemove = cache.filter(filter)
            cache.remove(toRemove)
            toRemove.values.forEach { it.destroy() }
        }
    }

    fun getLUT(file: File, asyncGenerator: Boolean, timeout: Long = 5000): Texture3D? {
        return getEntry("LUT", file.toString(), 0, timeout, asyncGenerator) {
            val img = ImageIO.read(file)
            val sqrt = sqrt(img.width + 0.5f).toInt()
            val tex = Texture3D(sqrt, img.height, sqrt)
            tex.create(img, false)
            tex
        } as? Texture3D
    }

    fun getInternalTexture(name: String, asyncGenerator: Boolean, timeout: Long = 60_000): Texture2D? {
        return getEntry("Texture", name, 0, timeout, asyncGenerator) {
            try {
                val img = GFX.loadAssetsImage(name)
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
    }

    fun getEntry(
        file: File,
        allowDirectories: Boolean,
        key: Any,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: () -> CacheData
    ): CacheData? {
        if (!file.exists() || (!allowDirectories && file.isDirectory)) return null
        return getEntry(file to key, timeout, asyncGenerator, generator)
    }

    fun getEntry(
        major: String,
        minor: String,
        sub: Int,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: () -> CacheData
    ): CacheData? {
        return getEntry(Triple(major, minor, sub), timeout, asyncGenerator, generator)
    }

    fun getEntry(key: Any, timeout: Long, asyncGenerator: Boolean, generator: () -> CacheData): CacheData? {

        // old, sync cache
        /*if(false){// key is FBStack.FBKey -> all textures are missing... why ever...
            synchronized(cache){
                val cached = cache[key]
                if(cached != null){
                    cached.lastUsed = GFX.gameTime
                    return cached.data
                }
                var data: CacheData? = null
                try {
                    data = generator()
                } catch (e: FileNotFoundException){
                    LOGGER.warn("FileNotFoundException: ${e.message}")
                } catch (e: Exception){
                    e.printStackTrace()
                }
                synchronized(cache){
                    cache[key] = CacheEntry(data, timeout, GFX.gameTime)
                }
                return data
            }
        }*/

        // new, async cache
        // only the key needs to be locked, not the whole cache

        if (asyncGenerator) {
            synchronized(lockedKeys) {
                if (key !in lockedKeys) {
                    lockedKeys += key
                } else {
                    return null
                } // somebody else is using the cache ;p
            }
        } else {
            var hasKey = false
            while (!hasKey) {
                synchronized(lockedKeys) {
                    if (key !in lockedKeys) {
                        lockedKeys += key
                        hasKey = true
                    }
                }
                if (hasKey) break
                Thread.sleep(1)
            }
        }


        val cached: CacheEntry?
        synchronized(cache) { cached = cache[key] }
        if (cached != null) {
            cached.lastUsed = GFX.gameTime
            synchronized(lockedKeys) { lockedKeys.remove(key) }
            return cached.data
        }

        return if (asyncGenerator) {
            thread {
                var data: CacheData? = null
                try {
                    data = generator()
                } catch (e: FileNotFoundException) {
                    LOGGER.warn("FileNotFoundException: ${e.message}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                synchronized(cache) { cache[key] = CacheEntry(data, timeout, gameTime) }
                synchronized(lockedKeys) { lockedKeys.remove(key) }
            }
            null
        } else {
            var data: CacheData? = null
            try {
                data = generator()
            } catch (e: FileNotFoundException) {
                LOGGER.warn("FileNotFoundException: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            synchronized(cache) { cache[key] = CacheEntry(data, timeout, gameTime) }
            synchronized(lockedKeys) { lockedKeys.remove(key) }
            data
        }

    }

    fun getVideoFrame(
        file: File,
        scale: Int,
        index: Int,
        bufferLength0: Int,
        fps: Double,
        timeout: Long,
        async: Boolean
    ): VFrame? {
        if (file.isDirectory || !file.exists()) return null
        if (index < 0) return null
        if (scale < 1) throw RuntimeException()
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, async) ?: return null
        return videoData.frames.getOrNull(index % bufferLength)
    }

    data class VideoFramesKey(
        val file: File,
        val scale: Int,
        val bufferIndex: Int,
        val frameLength: Int,
        val fps: Double
    )

    fun getVideoFrames(
        file: File,
        scale: Int,
        bufferIndex: Int,
        bufferLength: Int,
        fps: Double,
        timeout: Long,
        async: Boolean
    ) =
        getEntry(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps), timeout, async) {
            val meta = FFMPEGMetadata.getMeta(file, false)!!
            VideoData(file, meta.videoWidth / scale, meta.videoHeight / scale, bufferIndex, bufferLength, fps)
        } as? VideoData

    fun getImage(file: File, timeout: Long, asyncGenerator: Boolean) =
        if (file.isDirectory || !file.exists()) null
        else (getEntry(file as Any, timeout, asyncGenerator) {
            ImageData(file)
        } as? ImageData)?.texture

    fun update() {
        val minTimeout = 300L
        val time = GFX.gameTime
        synchronized(cache) {
            val toRemove =
                cache.filter { (_, entry) -> abs(entry.lastUsed - time) > max(entry.timeout, minTimeout) * 1_000_000 }
            toRemove.forEach {
                cache.remove(it.key)
                it.value.destroy()
            }
        }
    }

    fun resetFBStack() {
        synchronized(cache) {
            cache.values.forEach {
                (it.data as? FBStack.FBStackData)?.apply {
                    nextIndex = 0
                }
            }
        }
    }

}