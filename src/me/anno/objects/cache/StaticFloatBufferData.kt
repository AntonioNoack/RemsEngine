package me.anno.objects.cache

import me.anno.gpu.buffer.StaticBuffer

class StaticFloatBufferData(val buffer: StaticBuffer): CacheData {
    override fun destroy() {
        buffer.destroy()
    }
}