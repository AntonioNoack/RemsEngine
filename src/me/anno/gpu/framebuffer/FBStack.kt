package me.anno.gpu.framebuffer

import me.anno.objects.cache.Cache
import me.anno.objects.cache.CacheData

object FBStack {

    class FBStackData: CacheData {
        var nextIndex = 0
        val data = ArrayList<Framebuffer>()
        override fun destroy() {
            data.forEach { it.destroy() }
        }
    }

    data class FBKey(val w: Int, val h: Int, val samples: Int, val usesFP: Boolean)

    fun getValue(w: Int, h: Int, samples: Int, usesFP: Boolean): FBStackData {
        return Cache.getEntry(FBKey(w, h, samples, usesFP), 1000, false){
            FBStackData()
        } as FBStackData
    }

    operator fun get(name: String, w: Int, h: Int, samples: Int, usesFP: Boolean): Framebuffer {
        val value = getValue(w, h, samples, usesFP)
        synchronized(value){
            value.apply {
                return if(nextIndex >= data.size){
                    val framebuffer = Framebuffer(
                        name, w, h,
                        samples, 1, usesFP,
                        Framebuffer.DepthBufferType.TEXTURE
                    )
                    // if(!bind) framebuffer.unbind()
                    data.add(framebuffer)
                    nextIndex = data.size
                    data.last()
                } else {
                    val framebuffer = data[nextIndex++]
                    // if(bind) framebuffer.bind()
                    framebuffer.name = name
                    framebuffer
                }
            }
        }
    }

    fun clear(w: Int, h: Int, samples: Int){
        getValue(w, h, samples, true).nextIndex = 0
        getValue(w, h, samples, false).nextIndex = 0
    }

    fun reset(){
        Cache.resetFBStack()
    }

}