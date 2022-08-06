package me.anno.cache

class LRUCache<K, V>(val size: Int) {

    private val keys = arrayOfNulls<Any?>(size)
    private val values = arrayOfNulls<Any?>(size)
    private var idx = 0

    fun clear() {
        keys.fill(null)
        values.fill(null)
    }

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

}