package speiger.primitivecollections

import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.callbacks.IntObjectCallback

/**
 * Wrapper around LongToObjectHashMap.
 * The overhead isn't that big, and it saves us from having lots of duplicated code.
 * */
class IntToObjectHashMap<V>(minCapacity: Int = 16, loadFactor: Float = DEFAULT_LOAD_FACTOR) {

    val content = LongToObjectHashMap<V>(minCapacity, loadFactor)

    val size get() = content.size
    fun isEmpty() = size == 0

    operator fun set(key: Int, value: V?) {
        put(key, value)
    }

    inline fun getOrPut(key: Int, generateIfNull: () -> V): V {
        val value = this[key]
        if (value != null) return value
        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun put(key: Int, value: V?): V? {
        return content.put(key.toLong(), value)
    }

    fun remove(key: Int): V? {
        return content.remove(key.toLong())
    }

    fun remove(key: Int, value: V?): Boolean {
        return content.remove(key.toLong(), value)
    }

    operator fun get(key: Int): V? {
        return content[key.toLong()]
    }

    fun containsKey(key: Int): Boolean {
        return content.containsKey(key.toLong())
    }

    fun replace(key: Int, oldValue: V?, newValue: V?): Boolean {
        return content.replace(key.toLong(), oldValue, newValue)
    }

    fun replace(key: Int, value: V?): V? {
        return content.replace(key.toLong(), value)
    }

    fun clear() {
        content.clear()
    }

    fun trim(size: Int): Boolean {
        return content.trim(size)
    }

    @Suppress("unused")
    fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    fun forEach(callback: IntObjectCallback<V>) {
        content.forEach { k, v ->
            callback.callback(k.toInt(), v)
        }
    }

    // definitely not ideal...
    val values: Iterable<V>
        get() {
            val values = ArrayList<V>(size)
            forEach { _, v -> values.add(v) }
            return values
        }
}