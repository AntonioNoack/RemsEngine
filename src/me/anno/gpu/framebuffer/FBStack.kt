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

    data class FBKey(val w: Int, val h: Int, val withMultisampling: Boolean)

    fun getValue(w: Int, h: Int, withMultisampling: Boolean): FBStackData {
        return Cache.getEntry(FBKey(w, h, withMultisampling), 1000, false){
            FBStackData()
        } as FBStackData
    }

    operator fun get(name: String, w: Int, h: Int, withMultisampling: Boolean): Framebuffer {
        val value = getValue(w, h, withMultisampling)
        synchronized(value){
            value.apply {
                return if(nextIndex >= data.size){
                    val framebuffer = Framebuffer(
                        name, w, h,
                        if(withMultisampling) 8 else 1, 1, true,
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

    /*operator fun get(name: String, w: Int, h: Int, withMultisampling: Boolean): Framebuffer {
        val value = getValue(w, h, withMultisampling)
        synchronized(value){
            value.apply {
                return if(nextIndex >= data.size){
                    val framebuffer = Framebuffer(
                        name, w, h,
                        if(withMultisampling) 8 else 1, 1, true,
                        Framebuffer.DepthBufferType.TEXTURE
                    )
                    data.add(framebuffer)
                    nextIndex = data.size
                    data.last()
                } else {
                    val framebuffer = data[nextIndex++]
                    framebuffer.bind(w, h)
                    framebuffer.name = name
                    framebuffer
                }
            }
        }
    }*/

    fun clear(w: Int, h: Int, withMultisampling: Boolean){
        getValue(w, h, withMultisampling).nextIndex = 0
    }

}