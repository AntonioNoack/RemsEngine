package me.anno.cache

open class NonDestroyingCacheData<V>(var value: V) : ICacheData {
    override fun destroy() {
    }

    override fun toString(): String {
        val value = value
        return if (value == null) {
            "NonDestroyingCacheData<null>(${hashCode()})"
        } else {
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // Kotlin is too stupid to figure it out
            "NonDestroyingCacheData<${"$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hashCode()})"
        }
    }
}