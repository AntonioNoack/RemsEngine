package me.anno.cache

/**
 * thread-safe, last-recently-used cache with <size> entries,
 * if you have more than say 64 entries, you should consider something like a hashmap instead
 * */
class LRUCache<K, V>(val size: Int) {

    private val keys = arrayOfNulls<Any?>(size)
    private val values = arrayOfNulls<Any?>(size)
    private var idx = 0

    fun clear() {
        keys.fill(null)
        values.fill(null)
    }

    /**
     * returns value on success, Unit on failure (value may be null)
     * */
    operator fun get(key: K): Any? {
        synchronized(this) {
            for (i in 0 until size) {
                if (keys[i] === key) {
                    @Suppress("unchecked_cast")
                    return values[i] as V?
                }
            }
        }
        return Unit
    }

    operator fun set(key: K, value: V?) {
        synchronized(this) {
            val i = idx++
            if (idx >= size) idx = 0
            keys[i] = key
            values[i] = value
        }
    }

    fun register(): LRUCache<K,V> {
        CacheSection.registerCache(::clear, ::clear)
        return this
    }
}