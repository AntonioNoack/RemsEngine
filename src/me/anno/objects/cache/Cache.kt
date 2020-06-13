package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.objects.cache.VideoData.Companion.framesPerContainer
import me.anno.studio.Studio.editorTimeDilation
import me.anno.video.FFMPEGStream
import me.anno.video.Frame
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object Cache {

    private val cache = HashMap<Any, CacheEntry>()

    fun getLUT(file: File, timeout: Long = 5000): Texture3D? {
        val cache = getEntry("LUT", file.toString(), 0, timeout){
            val cache = Texture3DCache(null)
            thread {
                val img = ImageIO.read(file)
                val sqrt = sqrt(img.width+0.5f).toInt()
                val tex = Texture3D(sqrt, img.height, sqrt)
                tex.create(img, false)
                cache.texture = tex
            }
            cache
        } as? Texture3DCache
        return cache?.texture
    }

    fun getLUT(name: String, timeout: Long = 5000): Texture3D? {
        val cache = getEntry("LUT", name, 0, timeout){
            val cache = Texture3DCache(null)
            thread {
                val img = GFX.loadBImage(name)
                val sqrt = sqrt(img.width+0.5f).toInt()
                val tex = Texture3D(sqrt, img.height, sqrt)
                tex.create(img, false)
                cache.texture = tex
            }
            cache
        } as? Texture3DCache
        return cache?.texture
    }

    fun getIcon(name: String, timeout: Long = 5000): Texture2D {
        val cache = getEntry("Icon", name, 0, timeout){
            val cache = TextureCache(null)
            thread {
                val img = GFX.loadBImage(name)
                val tex = Texture2D(img.width, img.height)
                tex.create(img, false)
                cache.texture = tex
            }
            cache
        } as? TextureCache
        return cache?.texture ?: GFX.whiteTexture
    }

    fun getEntry(file: File, allowDirectories: Boolean, key: Any, timeout: Long, generator: () -> CacheData): CacheData? {
        if(!file.exists() || (!allowDirectories && file.isDirectory)) return null
        return getEntry(file to key, timeout, generator)
    }

    fun getEntry(major: String, minor: String, sub: Int, timeout: Long, generator: () -> CacheData): CacheData? {
        return getEntry(Triple(major, minor, sub), timeout, generator)
    }

    fun getEntry(key: Any, timeout: Long, generator: () -> CacheData): CacheData? {
        synchronized(cache){
            val cached = cache[key]
            if(cached != null){
                cached.lastUsed = GFX.lastTime
                return cached.data
            }
            var data: CacheData? = null
            try {
                data = generator()
            } catch (e: FileNotFoundException){
                println(e.message)
            } catch (e: Exception){
                e.printStackTrace()
            }
            cache[key] = CacheEntry(data, timeout, GFX.lastTime)
            return data
        }
    }

    // todo specify fps for our needs...
    // todo specify size for our needs
    fun getVideoFrame(file: File, index: Int, maxIndex: Int, fps: Float, timeout: Long, isLooping: Boolean = false): Frame? {
        if(index < 0) return null
        val bufferIndex = index/framesPerContainer
        val videoData = getVideoFrames(file, bufferIndex, fps, timeout) ?: return null
        if(videoData.time0 != GFX.lastTime){
            if(editorTimeDilation > 0.01f){
                if((bufferIndex+1)*framesPerContainer <= maxIndex){
                    getVideoFrames(file, bufferIndex+1, fps, timeout)
                } else if(isLooping){
                    getVideoFrames(file, 0, fps, timeout)
                }
            } else if(editorTimeDilation < -0.01f){
                if(bufferIndex > 0){
                    getVideoFrames(file, bufferIndex-1, fps, timeout)
                } else {
                    val maybeIndex = FFMPEGStream.frameCountByFile[file]
                    if(maybeIndex != null){// 1/16 probability, that this won't work ...
                        getVideoFrames(file, (maybeIndex-1)/framesPerContainer, fps, timeout)
                    }
                }
            }
        }
        return videoData.frames.getOrNull(index % framesPerContainer)
    }

    fun getVideoFrames(file: File, index: Int, fps: Float, timeout: Long) = getEntry(file, false, index to fps, timeout){
        VideoData(file, index, fps)
    } as? VideoData

    fun getImage(file: File, timeout: Long) = (getEntry(file, false, timeout, 0){
        ImageData(file)
    } as? ImageData)?.texture

    fun update(){
        val minTimeout = 300L
        val time = GFX.lastTime
        synchronized(cache){
            val toRemove = cache.filter { (_, entry) -> abs(entry.lastUsed - time) > max(entry.timeout, minTimeout) * 1_000_000 }
            toRemove.forEach {
                cache.remove(it.key)
                it.value.destroy()
            }
        }
    }

}