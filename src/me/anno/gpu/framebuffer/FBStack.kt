package me.anno.gpu.framebuffer

import me.anno.objects.cache.Cache
import me.anno.objects.cache.CacheData

object FBStack {

    class FBStackData(): CacheData {
        var nextIndex = 0
        val data = ArrayList<Framebuffer>()
        override fun destroy() {
            data.forEach { it.destroy() }
        }
    }

    data class FBKey(val w: Int, val h: Int)

    fun getValue(w: Int, h: Int): FBStackData {
        return Cache.getEntry(FBKey(w, h), 1000){
            FBStackData()
        } as FBStackData
    }

    operator fun get(w: Int, h: Int): Framebuffer {
        val value = getValue(w, h)
        synchronized(value){
            value.apply {
                return if(nextIndex >= data.size){
                    data.add(Framebuffer(w, h,
                        1, true,
                        Framebuffer.DepthBufferType.TEXTURE))
                    nextIndex = data.size
                    data.last()
                } else {
                    data[nextIndex++]
                }
            }
        }
    }

    fun clear(w: Int, h: Int){
        getValue(w,h).nextIndex = 0
    }

}