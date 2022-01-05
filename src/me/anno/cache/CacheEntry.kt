package me.anno.cache

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX.gameTime
import me.anno.utils.Sleep
import kotlin.math.max

class CacheEntry private constructor(
    var timeoutTime: Long,
    var generatorThread: Thread
) {

    constructor(timeout: Long) : this(gameTime + timeout, Thread.currentThread())

    val needsGenerator get() = generatorThread == Thread.currentThread() && (!hasGenerator || hasBeenDestroyed)

    fun reset(timeout: Long) {
        this.timeoutTime = gameTime + timeout
        this.generatorThread = Thread.currentThread()
        hasBeenDestroyed = false
        hasValue = false
    }

    fun update(timeout: Long) {
        timeoutTime = max(timeoutTime, gameTime + max(0L, timeout))
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

    /**
     * @param limitNanos sleeping time in nano seconds, default: 0.5s
     * @return whether you need to keep waiting
     * */
    fun waitForValue2(limitNanos: Long = 500_000_000): Boolean {
        return Sleep.waitUntil2(true, limitNanos) { hasValue || hasBeenDestroyed }
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