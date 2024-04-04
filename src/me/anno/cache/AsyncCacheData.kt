package me.anno.cache

import me.anno.engine.Events.addEvent
import me.anno.utils.Sleep

open class AsyncCacheData<V> : ICacheData {

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

    fun waitForGFX(callback: (V?) -> Unit) {
        if (hasValue) callback(value)
        else addEvent { waitForGFX(callback) }
    }

    override fun destroy() {
        (value as? ICacheData)?.destroy()
        value = null
        hasBeenDestroyed = true
    }

    override fun toString(): String {
        val value = value
        return if (value == null) {
            "AsyncCacheData<null>(${hashCode()},$hasValue)"
        } else {
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // not unnecessary, because Intellij complains without it
            "AsyncCacheData<${"$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hashCode()},$hasValue)"
        }
    }
}