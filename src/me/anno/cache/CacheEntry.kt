package me.anno.cache

import me.anno.Time.nanoTime
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
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

    @Deprecated("Please use the variant with callback")
    fun waitForValueOrThrow(key: Any?, limitNanos: Long = 60 * SECONDS_TO_NANOS) {
        Sleep.waitUntilOrThrow(true, limitNanos, key) {
            update(500) // ensure that it stays loaded; 500 is a little high,
            // but we need the image to stay loaded for GFX.addGPUTask() afterward in some places
            (hasValue && (data as? AsyncCacheData<*>)?.hasValue != false)
                    || hasBeenDestroyed
        }
    }

    /**
     * @param limitNanos sleeping time in nanoseconds, default: 0.5s
     * @return whether you need to keep waiting
     * */
    @Deprecated("Please use the variant with callback")
    fun waitForValueReturnWhetherIncomplete(limitNanos: Long = 500_000_000): Boolean {
        return Sleep.waitUntilReturnWhetherIncomplete(true, limitNanos) {
            update(16) // ensure it's loaded
            (hasValue && (data as? AsyncCacheData<*>)?.hasValue != false)
                    || hasBeenDestroyed
        }
    }

    fun <R> callback(exception: Exception?, resultCallback: Callback<R>) {
        @Suppress("UNCHECKED_CAST")
        resultCallback.call(
            if (hasBeenDestroyed) null
            else data as? R, exception
        )
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