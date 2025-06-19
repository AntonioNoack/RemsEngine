package me.anno.cache

import me.anno.Time.nanoTime
import me.anno.gpu.GFX
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread
import kotlin.math.max

open class AsyncCacheData<V : Any> : ICacheData, Callback<V> {

    var timeoutNanoTime: Long = 0
    var generatorThread: Thread = Thread.currentThread()

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

    fun reset(timeoutMillis: Long) {
        timeoutNanoTime = nanoTime + timeoutMillis * MILLIS_TO_NANOS
        generatorThread = Thread.currentThread()
    }

    fun update(timeoutMillis: Long) {
        val secondTime = nanoTime + max(0L, timeoutMillis) * MILLIS_TO_NANOS
        timeoutNanoTime = max(timeoutNanoTime, secondTime)
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(): V? {
        warnRecursive()
        Sleep.waitUntil(true) { hasValue }
        return value
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(async: Boolean): V? {
        if (!async) waitFor()
        return value
    }

    private fun warnRecursive() {
        if (generatorThread == Thread.currentThread()) {
            LOGGER.warn("Recursive dependency?")
        }
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

        private val LOGGER = LogManager.getLogger(AsyncCacheData::class)
        const val ASYNC_WARNING = "Avoid blocking, it's also not supported in browsers"

        @Deprecated(message = ASYNC_WARNING)
        inline fun <V : Any> loadSync(loadAsync: (Callback<V>) -> Unit): V? {
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