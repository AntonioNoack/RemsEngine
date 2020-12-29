package me.anno.cache

open class CacheData<V>(var value: V): ICacheData {
    override fun destroy() {}
}