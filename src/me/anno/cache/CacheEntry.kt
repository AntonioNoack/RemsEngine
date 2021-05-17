package me.anno.cache

import me.anno.cache.data.ICacheData

class CacheEntry(
    var timeout: Long,
    var lastUsed: Long
) {

    var data: ICacheData? = null
        set(value) {
            field = value
            hasValue = true
        }

    var hasValue = false
    var hasBeenDestroyed = false

    fun destroy() {
        if(hasBeenDestroyed) throw IllegalStateException()
        hasBeenDestroyed = true
        data?.destroy()
        data = null
    }

}