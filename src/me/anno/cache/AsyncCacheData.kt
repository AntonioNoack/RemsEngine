package me.anno.cache

import me.anno.gpu.GFX
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import kotlin.concurrent.thread

open class AsyncCacheData<V> : ICacheData, Callback<V> {

    var hasValue = false
    var hasBeenDestroyed = false
    var value: V? = null
        set(value) {
            if (hasBeenDestroyed) {
                (value as? ICacheData)?.destroy()
            } else {
                field = value
            }
            hasValue = true
        }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(): V? {
        Sleep.waitUntil(true) { hasValue }
        return value
    }

    fun waitFor(callback: (V?) -> Unit) {
        Sleep.waitUntil(true, { hasValue }) {
            callback(value)
        }
    }

    fun waitFor(callback: Callback<V>) {
        Sleep.waitUntil(true, { hasValue }) {
            val value = value
            if (value != null) callback.ok(value)
            else callback.err(null)
        }
    }

    override fun call(value: V?, exception: Exception?) {
        this.value = value
        exception?.printStackTrace()
    }

    override fun destroy() {
        (value as? ICacheData)?.destroy()
        value = null
        hasBeenDestroyed = true
    }

    override fun toString(): String {
        val value = value
        return if (value == null) {
            "AsyncCacheData<null>(#${hashCode()},$hasValue)"
        } else {
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // necessary, because Intellij complains without it
            "AsyncCacheData<${"$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hashCode()},$hasValue)"
        }
    }

    companion object {

        const val ASYNC_WARNING = "Avoid blocking, it's also not supported in browsers"

        @Deprecated(message = ASYNC_WARNING)
        inline fun <V> loadSync(loadAsync: (Callback<V>) -> Unit): V? {
            val wrapper = AsyncCacheData<V>()
            loadAsync(wrapper)
            wrapper.waitFor()
            return wrapper.value
        }

        fun runOnNonGFXThread(threadName: String, runnable: () -> Unit) {
            if (GFX.isGFXThread()) {
                thread(name = threadName) {
                    runnable()
                }
            } else {
                runnable()
            }
        }
    }
}