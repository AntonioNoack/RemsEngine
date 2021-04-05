package me.anno.gpu.framebuffer

import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import org.apache.logging.log4j.LogManager

object FBStack : CacheSection("FBStack") {

    private val LOGGER = LogManager.getLogger(FBStack::class)

    class FBStackData(val key: FBKey) : ICacheData {
        var nextIndex = 0
        val data = ArrayList<Framebuffer>()
        override fun destroy() {
            data.forEach { it.destroy() }
            LOGGER.info("Destroyed ${data.size} framebuffers of $key")
        }
    }

    data class FBKey(val w: Int, val h: Int, val samples: Int, val usesFP: Boolean)

    fun getValue(w: Int, h: Int, samples: Int, usesFP: Boolean): FBStackData {
        val key = FBKey(w, h, samples, usesFP)
        return getEntry(key, 2100, false) {
            FBStackData(key)
        } as FBStackData
    }

    operator fun get(name: String, w: Int, h: Int, samples: Int, usesFP: Boolean): Framebuffer {
        val value = getValue(w, h, samples, usesFP)
        synchronized(value) {
            value.apply {
                return if (nextIndex >= data.size) {
                    val framebuffer = Framebuffer(
                        name, w, h,
                        samples, 1, usesFP,
                        Framebuffer.DepthBufferType.TEXTURE
                    )
                    data.add(framebuffer)
                    nextIndex = data.size
                    data.last()
                } else {
                    val framebuffer = data[nextIndex++]
                    framebuffer.name = name
                    framebuffer
                }
            }
        }
    }

    fun clear(w: Int, h: Int) {
        for (j in 0 until 8) clear(w, h, 1 shl j)
    }

    fun clear(w: Int, h: Int, samples: Int) {
        getValue(w, h, samples, true).nextIndex = 0
        getValue(w, h, samples, false).nextIndex = 0
    }

    fun reset() {
        resetFBStack()
    }

    fun resetFBStack() {
        synchronized(cache) {
            cache.values.forEach {
                (it.data as? FBStackData)?.apply {
                    nextIndex = 0
                }
            }
        }
    }

}