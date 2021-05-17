package me.anno.cache

import me.anno.cache.data.ICacheData
import me.anno.utils.Sleep

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

    fun waitForValue(){
        Sleep.waitUntil(true){ hasValue }
    }

    fun destroy() {
        if(hasBeenDestroyed) throw IllegalStateException()
        hasBeenDestroyed = true
        data?.destroy()
        data = null
    }

}