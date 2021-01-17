package me.anno.cache

import me.anno.cache.data.ICacheData

open class CacheData<V>(var value: V): ICacheData {
    override fun destroy() {}
}