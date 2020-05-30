package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.objects.cache.VideoData.Companion.framesPerContainer
import me.anno.video.FFMPEGStream
import me.anno.video.Frame
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.math.abs

object Cache {

    private val cache = HashMap<Any, CacheEntry>()

    fun getIcon(name: String): Texture2D {
        val cache = getEntry("Icon", name, 0){
            TextureCache(Texture2D(GFX.loadBImage(name)))
        } as? TextureCache
        return cache?.texture ?: GFX.whiteTexture
    }

    fun getEntry(file: File, allowDirectories: Boolean, index: Int, generator: () -> CacheData): CacheData? {
        if(!file.exists() || (!allowDirectories && file.isDirectory)) return null
        return getEntry(file to index, generator)
    }

    fun getEntry(major: String, minor: String, sub: Int, generator: () -> CacheData): CacheData? {
        return getEntry(Triple(major, minor, sub), generator)
    }

    fun getEntry(key: Any, generator: () -> CacheData): CacheData? {
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
            cache[key] = CacheEntry(data, GFX.lastTime)
            return data
        }
    }

    fun getVideoMeta(file: File){
        val videoBuffer = getVideoFrames(file, 0)

    }

    // todo specify fps for our needs...
    fun getVideoFrame(file: File, index: Int, maxIndex: Int, isLooping: Boolean = false): Frame? {
        if(index < 0) return null
        val bufferIndex = index/framesPerContainer
        val videoData = getVideoFrames(file, bufferIndex) ?: return null
        if(videoData.time0 != GFX.lastTime){
            if(GFX.editorTimeDilation > 0.01f){
                if((bufferIndex+1)*framesPerContainer <= maxIndex){
                    getVideoFrames(file, bufferIndex+1)
                } else if(isLooping){
                    getVideoFrames(file, 0)
                }
            } else if(GFX.editorTimeDilation < -0.01f){
                if(bufferIndex > 0){
                    getVideoFrames(file, bufferIndex-1)
                } else {
                    val maybeIndex = FFMPEGStream.frameCountByFile[file]
                    if(maybeIndex != null){// 1/16 probability, that this won't work ...
                        getVideoFrames(file, (maybeIndex-1)/framesPerContainer)
                    }
                }
            }
        }
        return videoData.frames.getOrNull(index % framesPerContainer)
    }

    fun getVideoFrames(file: File, index: Int) = getEntry(file, false, index){
        VideoData(file, index)
    } as? VideoData

    fun getImage(file: File) = (getEntry(file, false, 0){
        ImageData(file)
    } as? ImageData)?.texture

    fun update(){
        val timeout = 1_500_000_000L
        val time = GFX.lastTime
        synchronized(cache){
            val toRemove = cache.filter { (_, it) -> abs(it.lastUsed - time) > timeout }
            toRemove.forEach {
                cache.remove(it.key)
                it.value.destroy()
            }
        }
    }

}