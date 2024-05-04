package me.anno.cache

import me.anno.utils.Sleep
import me.anno.utils.structures.Callback

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
    fun waitForGFX(): V? {
        Sleep.waitForGFXThread(true) { hasValue }
        return value
    }

    @Deprecated(message = "Not supported on web")
    fun waitFor(): V? {
        Sleep.waitUntil(true) { hasValue }
        return value
    }

    fun waitForGFX(callback: (V?) -> Unit) {
        Sleep.waitUntil(true, { hasValue }) {
            callback(value)
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
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // not unnecessary, because Intellij complains without it
            "AsyncCacheData<${"$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hashCode()},$hasValue)"
        }
    }
}