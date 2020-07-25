package me.anno.objects.cache

import me.anno.gpu.buffer.StaticFloatBuffer

class StaticFloatBufferData(val buffer: StaticFloatBuffer): CacheData {
    override fun destroy() {
        buffer.destroy()
    }
}