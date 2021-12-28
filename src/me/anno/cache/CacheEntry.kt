package me.anno.cache

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.utils.Sleep

class CacheEntry private constructor(
    var timeout: Long,
    var lastUsed: Long,
    var generatorThread: Thread
) {

    constructor(timeout: Long) : this(timeout, GFX.gameTime, Thread.currentThread())

    val needsGenerator get() = generatorThread == Thread.currentThread() && (!hasGenerator || hasBeenDestroyed)

    fun reset(timeout: Long) {
        this.timeout = timeout
        this.lastUsed = GFX.gameTime
        this.generatorThread = Thread.currentThread()
        hasBeenDestroyed = false
        hasValue = false
    }

    var data: ICacheData? = null
        set(value) {
            field = value
            hasValue = true
        }

    var hasValue = false
    var hasBeenDestroyed = false
    var hasGenerator = false

    fun waitForValue(key: Any?, limitNanos: Long = 60_000_000_000) {
        Sleep.waitUntil(true, limitNanos, key) { hasValue || hasBeenDestroyed }
    }

    fun destroy() {
        if (!hasBeenDestroyed) {
            hasBeenDestroyed = true
            data?.destroy()
            data = null
        } else {
            RuntimeException("Cannot destroy things twice!")
                .printStackTrace()
        }
    }

}