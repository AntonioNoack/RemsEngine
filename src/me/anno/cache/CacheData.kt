package me.anno.cache

import me.anno.cache.data.ICacheData

open class CacheData<V>(var value: V) : ICacheData {
    override fun destroy() {}
    override fun toString(): String {
        val value = value
        return if(value == null){
            "CacheData<null>(${hashCode()})"
        } else {
            "CacheData<${"$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hashCode()})"
        }
    }
}