package me.anno.cache

import me.anno.cache.CacheSection.Companion.callSafely
import me.anno.cache.CacheTime.MAX_CACHE_DT_MILLIS
import me.anno.cache.CacheTime.cacheTimeMillis
import me.anno.utils.Threads.runOnNonGFXThread
import kotlin.math.max

class PromiseBody<V : Any>() : IPromiseBody<V> {

    override val hasExpired: Boolean get() = hasBeenDestroyed || cacheTimeMillis > timeoutCacheTime
    override var timeoutCacheTime: Long = cacheTimeMillis + MAX_CACHE_DT_MILLIS * 3

    override var hasValue = false
        private set

    override var hasBeenDestroyed = false
        private set

    override var value: V? = null
        set(value) {
            if (hasBeenDestroyed) {
                (value as? ICacheData)?.destroy()
            } else {
                field = value
            }
            hasValue = true
            processCallbacks()
        }

    override val waitForCallbacks = ArrayList<(V?) -> Unit>()

    private fun processCallbacks() {
        val value = value
        while (true) {
            val callback = synchronized(waitForCallbacks) {
                waitForCallbacks.removeLastOrNull()
            } ?: break
            runOnNonGFXThread("Callback") {
                callSafely(value, callback)
            }
        }
    }

    override fun addCallback(callback: (V?) -> Unit) {
        synchronized(waitForCallbacks) { waitForCallbacks.add(callback) }
        if (hasValue || hasBeenDestroyed) processCallbacks()
    }

    override fun addCallbacks(callbacks: List<(V?) -> Unit>) {
        if (callbacks.isEmpty()) return // fast path without locking
        synchronized(waitForCallbacks) { waitForCallbacks.addAll(callbacks) }
        if (hasValue || hasBeenDestroyed) processCallbacks()
    }

    override fun update(timeoutMillis: Long) {
        synchronized(this) {
            val delta = max(timeoutMillis, MAX_CACHE_DT_MILLIS * 2L)
            timeoutCacheTime = max(timeoutCacheTime, cacheTimeMillis + delta)
        }
    }

    override fun destroy() {
        (value as? ICacheData)?.destroy()
        value = null
        hasBeenDestroyed = true
        processCallbacks()
    }
}