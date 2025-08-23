package me.anno.cache

import me.anno.cache.CacheTime.cacheTime
import me.anno.engine.Events.getCalleeName
import me.anno.utils.Sleep
import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.utils.async.Callback
import kotlin.math.max

/**
 * Represents a value to be filled in whenever.
 * You can "append" listeners to get notified when it is ready.
 *
 * To get asynchronous access to the value, just use this.value.
 * For synchronous access (listeners), use this.waitFor().
 * */
open class AsyncCacheData<V : Any>() : ICacheData, Callback<V> {

    constructor(value: V?) : this() {
        this.value = value
    }

    val hasExpired: Boolean get() = cacheTime > timeoutCacheTime
    var timeoutCacheTime: Long = 0L
        private set

    var hasValue = false
        private set

    var hasBeenDestroyed = false
        private set

    var value: V? = null
        set(value) {
            if (hasBeenDestroyed) {
                (value as? ICacheData)?.destroy()
            } else {
                field = value
            }
            hasValue = true
            processCallbacks()
        }

    private val waitForCallbacks = ArrayList<(V?) -> Unit>()

    private fun processCallbacks() {
        val value = value
        while (true) {
            val last = synchronized(waitForCallbacks) {
                waitForCallbacks.removeLastOrNull()
            } ?: break
            runOnNonGFXThread("Callback") { last(value) }
        }
    }

    private fun addCallback(callback: (V?) -> Unit) {
        synchronized(waitForCallbacks) { waitForCallbacks.add(callback) }
        if (hasValue || hasBeenDestroyed) processCallbacks()
    }

    fun reset(timeoutMillis: Long) {
        timeoutCacheTime = cacheTime + timeoutMillis
    }

    fun update(timeoutMillis: Long) {
        timeoutCacheTime = max(timeoutCacheTime, cacheTime + timeoutMillis)
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(debugName: String = getCalleeName()): V? {
        Sleep.waitUntil(debugName,true) { hasValue }
        return value
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(async: Boolean): V? {
        if (!async) waitFor(getCalleeName())
        return value
    }

    fun waitFor(callback: (V?) -> Unit) {
        addCallback(callback)
    }

    fun waitFor(extraCondition: (V?) -> Boolean, callback: (V?) -> Unit) {
        Sleep.waitUntil(true, { hasValue && extraCondition(value) }) {
            callback(value)
        }
    }

    fun <W> waitUntilDefined(valueMapper: (V) -> W?, callback: (W) -> Unit) {
        waitFor { value ->
            val newValue = if (value != null) valueMapper(value) else null
            if (newValue != null) callback(newValue)
        }
    }

    fun waitFor(callback: Callback<V>) {
        waitFor { value ->
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
        processCallbacks()
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

    fun <W : Any> mapNext(mapping: (V) -> W?): AsyncCacheData<W> {
        val result = AsyncCacheData<W>()
        mapResult(result, mapping)
        return result
    }

    fun <W : Any> mapNextNullable(mapping: (V?) -> W?): AsyncCacheData<W> {
        val result = AsyncCacheData<W>()
        waitFor { ownValue -> result.value = mapping(ownValue) }
        return result
    }

    fun <W : Any> mapResult(result: AsyncCacheData<W>, mapping: (V) -> W?) {
        waitFor { ownValue -> result.value = if (ownValue != null) mapping(ownValue) else null }
    }

    fun <W : Any> onSuccess(result: AsyncCacheData<*>, mapping: (V) -> W) {
        waitFor { ownValue ->
            if (ownValue != null) mapping(ownValue)
            else result.value = null
        }
    }

    fun <W : Any> mapNext2(mapping: (V) -> AsyncCacheData<W>): AsyncCacheData<W> {
        val result = AsyncCacheData<W>()
        waitFor { value ->
            if (value != null) {
                mapping(value).waitFor(result)
            } else result.value = null
        }
        return result
    }

    companion object {

        const val ASYNC_WARNING = "Avoid blocking, it's also not supported in browsers"

        private val nothingCacheData = AsyncCacheData<Any>(null)

        fun <V : Any> empty(): AsyncCacheData<V> {
            @Suppress("UNCHECKED_CAST")
            return nothingCacheData as AsyncCacheData<V>
        }

        @Deprecated(message = ASYNC_WARNING)
        inline fun <V : Any> loadSync(loadAsync: (Callback<V>) -> Unit): V? {
            val wrapper = AsyncCacheData<V>()
            loadAsync(wrapper)
            wrapper.waitFor()
            return wrapper.value
        }
    }
}