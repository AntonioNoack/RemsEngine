package me.anno.cache

import me.anno.cache.CacheSection.Companion.callSafely
import me.anno.engine.Events.getCalleeName
import me.anno.utils.Logging.hash32
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents a value to be filled in whenever.
 * You can "append" listeners to get notified when it is ready.
 *
 * To get asynchronous access to the value, just use this.value.
 * For synchronous access (listeners), use this.waitFor().
 * */
open class Promise<V : Any>(
    content: IPromiseBody<V>
) : ICacheData, Callback<V> {

    constructor() : this(PromiseBody<V>())
    constructor(value: V?) : this() {
        call(value, null)
    }

    val hasValue: Boolean get() = content.hasValue
    val hasExpired: Boolean get() = content.hasExpired && locks.get() <= 0
    val hasBeenDestroyed: Boolean get() = content.hasBeenDestroyed
    val timeoutCacheTime: Long get() = content.timeoutCacheTime

    var value: V?
        get() = content.value
        set(value) {
            content.value = value
        }

    var content: IPromiseBody<V> = content
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
        lock()
        Sleep.waitUntil("AsyncCacheData.waitFor", true) { hasValue }
        unlock()
        return value
    }

    @Deprecated(message = ASYNC_WARNING)
    fun waitFor(debugName: String): V? {
        lock()
        Sleep.waitUntil(debugName, true) { hasValue }
        unlock()
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

    /**
     * Waits for Promise to have a value (could be null!),
     * and then calls the callback.
     *
     * When the engine is shut down,
     * you lose your guarantee for a callback.
     *
     * If you want to save on quit,
     * prompt the user shortly before quitting instead!
     * */
    fun waitFor(callback: (V?) -> Unit) {
        lock()
        content.addCallback { valueI ->
            callSafely(valueI, callback)
            unlock()
        }
    }

    fun waitFor(extraCondition: (V?) -> Boolean, callback: (V?) -> Unit) {
        lock()
        Sleep.waitUntil(true, { hasValue && extraCondition(value) }) {
            callSafely(value, callback)
            unlock()
        }
    }

    fun waitFor(callback: Callback<V>) {
        waitFor { value ->
            if (value != null) callback.ok(value)
            else callback.err(null)
            if (callback is Promise<*> && hasBeenDestroyed) {
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

    fun <W : Any> mapResult(result: Promise<W>, mapping: (V) -> W?) {
        waitFor { ownValue -> result.value = if (ownValue != null) mapping(ownValue) else null }
    }

    fun <W : Any> onSuccess(result: Promise<*>, mapping: (V) -> W) {
        waitFor { ownValue ->
            if (ownValue != null) mapping(ownValue)
            else result.value = null
        }
    }

    /**
     * Locks, runs, unlocks.
     * */
    inline fun use(run: () -> Unit) {
        lock()
        run()
        unlock()
    }

    /**
     * Use this method before expensive calculations
     * to make sure the value doesn't get destroyed in the meantime
     * todo use this e.g. for asset thumbnail generation
     * todo Unity assets often have sync-issues: generate all assets at once, or does this solve the issue?
     *   -> we didn't do that, because it used lots of memory...
     * */
    fun lock() {
        locks.incrementAndGet()
    }

    /**
     * Use this method after expensive calculations & a corresponding
     * lock-call() to make the value destructible, again
     * */
    fun unlock() {
        update(1)
        locks.decrementAndGet()
    }

    private val locks = AtomicInteger(0)

    companion object {

        const val ASYNC_WARNING = "Avoid blocking, it's also not supported in browsers"

        private val nothingCacheData = Promise<Any>(null)

        fun <V : Any> empty(): Promise<V> {
            @Suppress("UNCHECKED_CAST")
            return nothingCacheData as Promise<V>
        }

        @Deprecated(message = ASYNC_WARNING)
        inline fun <V : Any> loadSync(loadAsync: (Callback<V>) -> Unit): V? {
            val wrapper = Promise<V>()
            loadAsync(wrapper)
            wrapper.waitFor()
            return wrapper.value
        }
    }
}