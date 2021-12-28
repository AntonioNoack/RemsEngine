package me.anno.cache

import me.anno.cache.data.ICacheData

open class CacheData<V>(var value: V) : ICacheData {
    override fun destroy() {}
    override fun toString(): String {
        val value = value
        return "CacheData<${if (value == null) "null" else "$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hashCode()})"
    }
}