package me.anno.objects.cache

import me.anno.gpu.GFX
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import kotlin.math.abs

object Cache {

    val cache = HashMap<Pair<File, Int>, CacheEntry>()

    fun getEntry(file: File, allowDirectories: Boolean, index: Int, generator: () -> CacheData): CacheData? {
        if(!file.exists() || (!allowDirectories && file.isDirectory)) return null
        synchronized(cache){
            val key = file to index
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
            cache[key] = CacheEntry(file, index, data, GFX.lastTime)
            return data
        }
    }

    fun getVideoMeta(file: File, index: Int){
        // todo get the meta
    }

    // todo specify fps for our needs...
    fun getVideoFrame(file: File, index: Int) = getVideoFrames(file, index/VideoData.framesPerContainer)?.getOrNull(index % VideoData.framesPerContainer)
    fun getVideoFrames(file: File, index: Int) = (getEntry(file, false, index){
        VideoData(file, index)
    } as? VideoData)?.textures

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
            }
        }
    }

}