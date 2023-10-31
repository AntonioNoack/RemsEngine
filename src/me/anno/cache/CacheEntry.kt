package me.anno.cache

import me.anno.Time.nanoTime
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Sleep
import kotlin.math.max

class CacheEntry private constructor(
    var timeoutNanoTime: Long,
    var generatorThread: Thread
) {

    constructor(timeoutMillis: Long) : this(nanoTime + timeoutMillis * MILLIS_TO_NANOS, Thread.currentThread())

    val needsGenerator get() = (!hasGenerator || hasBeenDestroyed) && (generatorThread == Thread.currentThread())

    fun reset(timeoutMillis: Long) {
        this.timeoutNanoTime = nanoTime + timeoutMillis * MILLIS_TO_NANOS
        this.generatorThread = Thread.currentThread()
        deletingThreadName = null
        hasValue = false
    }

    fun update(timeout: Long) {
        val secondTime = nanoTime + max(0L, timeout) * MILLIS_TO_NANOS
        timeoutNanoTime = max(timeoutNanoTime, secondTime)
    }

    var data: ICacheData? = null
        set(value) {
            field = value
            hasValue = true
        }

    var hasValue = false
    val hasBeenDestroyed get() = deletingThreadName != null
    var hasGenerator = false

    fun waitForValue(key: Any?, limitNanos: Long = 60_000_000_000) {
        Sleep.waitUntil(true, limitNanos, key) {
            (hasValue && (data as? AsyncCacheData<*>)?.hasValue != false)
                    || hasBeenDestroyed
        }
    }

    /**
     * @param limitNanos sleeping time in nanoseconds, default: 0.5s
     * @return whether you need to keep waiting
     * */
    fun waitForValue2(limitNanos: Long = 500_000_000): Boolean {
        return Sleep.waitUntil2(true, limitNanos) {
            (hasValue && (data as? AsyncCacheData<*>)?.hasValue != false)
                    || hasBeenDestroyed
        }
    }

    var deletingThreadName: String? = null

    fun destroy() {
        if (deletingThreadName == null) {
            deletingThreadName = Thread.currentThread().name
            data?.destroy()
            data = null
        } else {
            RuntimeException("Cannot destroy things twice! ${this::class.qualifiedName}, by $deletingThreadName from ${Thread.currentThread().name}")
                .printStackTrace()
        }
    }

}