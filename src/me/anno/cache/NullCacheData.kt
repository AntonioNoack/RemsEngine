package me.anno.cache

object NullCacheData {
    private val nothingCacheData = AsyncCacheData<Any>().apply {
        value = null
    }

    fun <V : Any> get(): AsyncCacheData<V> {
        @Suppress("UNCHECKED_CAST")
        return nothingCacheData as AsyncCacheData<V>
    }
}