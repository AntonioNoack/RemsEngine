package me.anno.cache

open class AsyncCacheData<V>(val destroy: Boolean = true) : ICacheData {

    var hasValue = false
    var value: V? = null
        set(value) {
            field = value
            hasValue = true
        }

    override fun destroy() {
        (value as? ICacheData)?.destroy()
        value = null
    }

    override fun toString(): String {
        val value = value
        return if (value == null) {
            "AsyncCacheData<null>(${hashCode()},$hasValue)"
        } else {
            "AsyncCacheData<${"$value, ${value!!::class.simpleName}, ${value.hashCode()}"}>(${hashCode()},$hasValue)"
        }
    }
}