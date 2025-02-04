package me.anno.cache

import me.anno.utils.Sleep
import me.anno.utils.async.Callback

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

    @Deprecated(message = "Not supported on web")
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
}