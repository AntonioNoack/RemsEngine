package me.anno.cache

import me.anno.engine.Events.getCalleeName
import me.anno.utils.Logging.hash32
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import me.anno.video.VideoSlice

/**
 * Represents a value to be filled in whenever.
 * You can "append" listeners to get notified when it is ready.
 *
 * To get asynchronous access to the value, just use this.value.
 * For synchronous access (listeners), use this.waitFor().
 * */
open class AsyncCacheData<V : Any>(
    content: IAsyncCacheContent<V>
) : ICacheData, Callback<V> {

    constructor() : this(AsyncCacheContent<V>())
    constructor(value: V?) : this() {
        call(value, null)
    }

    val hasValue: Boolean get() = content.hasValue
    val hasExpired: Boolean get() = content.hasExpired
    val hasBeenDestroyed: Boolean get() = content.hasBeenDestroyed
    val timeoutCacheTime: Long get() = content.timeoutCacheTime
    var value: V?
        get() = content.value
        set(value) {
            content.value = value
        }

    var content: IAsyncCacheContent<V> = content
        set(value) {
            val oldCallbacks = field.waitForCallbacks

            // must be assigned before callbacks are processed,
            // because our listeners might ask this for value
            field = value

            synchronized(oldCallbacks) {
                value.addCallbacks(oldCallbacks)
            }
        }

    fun update(timeoutMillis: Long) {
        content.update(timeoutMillis)
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(): V? {
        Sleep.waitUntil("AsyncCacheData.waitFor", true) { hasValue }
        return value
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(debugName: String): V? {
        Sleep.waitUntil(debugName, true) { hasValue }
        return value
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(debugName: String, async: Boolean): V? {
        if (!async) waitFor(debugName)
        return value
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(async: Boolean): V? {
        if (!async) waitFor(getCalleeName())
        return value
    }

    fun waitFor(callback: (V?) -> Unit) {
        content.addCallback(callback)
    }

    fun waitFor(extraCondition: (V?) -> Boolean, callback: (V?) -> Unit) {
        Sleep.waitUntil(true, { hasValue && extraCondition(value) }) {
            callback(value)
        }
    }

    fun waitFor(callback: Callback<V>) {
        waitFor { value ->
            if (value != null) callback.ok(value)
            else callback.err(null)
            if (callback is AsyncCacheData<*> && hasBeenDestroyed) {
                callback.destroy()
            }
        }
    }

    override fun call(value: V?, exception: Exception?) {
        content.value = value
        exception?.printStackTrace()
    }

    override fun destroy() {
        content.destroy()
    }

    override fun toString(): String {
        val value = value
        return if (value == null) {
            "AsyncCacheData<null>(#${hash32(this)},${flags()})"
        } else {
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // necessary, because Intellij complains without it
            "AsyncCacheData<${"$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hash32(this)},${flags()})"
        }
    }

    private fun flags(): String {
        return (if (hasValue) if (value != null) "V" else "v" else "") +
                (if (hasBeenDestroyed) "x" else "") +
                (content.waitForCallbacks.size.toString())
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