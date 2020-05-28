package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.video.Frame
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import kotlin.math.abs

object Cache {

    private val cache = HashMap<Any, CacheEntry>()

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
    fun getVideoFrame(file: File, index: Int): Frame? {
        if(index < 0) return null
        val bufferIndex = index/VideoData.framesPerContainer
        val videoData = getVideoFrames(file, bufferIndex) ?: return null
        if(videoData.time0 != GFX.lastTime){
            if(GFX.editorTimeDilation > 0.01f){
                // todo check if that buffer still would be valid
                getVideoFrames(file, bufferIndex+1)
            } else if(GFX.editorTimeDilation < -0.01f){
                if(bufferIndex > 0){
                    getVideoFrames(file, bufferIndex-1)
                }
            }
        }
        return videoData.frames.getOrNull(index % VideoData.framesPerContainer)
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