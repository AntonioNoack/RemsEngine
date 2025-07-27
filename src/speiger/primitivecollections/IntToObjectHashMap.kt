package speiger.primitivecollections

import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.IntCallback
import speiger.primitivecollections.callbacks.IntObjectCallback
import speiger.primitivecollections.callbacks.IntObjectPredicate

/**
 * Wrapper around LongToObjectHashMap.
 * The overhead isn't that big, and it saves us from having lots of duplicated code.
 * */
class IntToObjectHashMap<V>(
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : PrimitiveCollection {

    val content = LongToObjectHashMap<V>(minCapacity, loadFactor)

    override val size get() = content.size
    override val maxFill: Int get() = content.maxFill

    operator fun set(key: Int, value: V) {
        put(key, value)
    }

    inline fun getOrPut(key: Int, generateIfNull: () -> V): V {
        val value = this[key]
        if (value != null) return value
        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun put(key: Int, value: V): V? {
        return content.put(key.toLong(), value)
    }

    fun remove(key: Int): V? {
        return content.remove(key.toLong())
    }

    operator fun get(key: Int): V? {
        return content[key.toLong()]
    }

    fun containsKey(key: Int): Boolean {
        return content.containsKey(key.toLong())
    }

    override fun clear() {
        content.clear()
    }

    override fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    fun forEach(callback: IntObjectCallback<V>) {
        content.forEach { k, v ->
            callback.callback(k.toInt(), v)
        }
    }

    fun removeIf(predicate: IntObjectPredicate<V>): Int =
        content.removeIf { key, value -> predicate.test(key.toInt(), value) }

    fun forEachKey(callback: IntCallback) {
        content.forEachKey { key -> callback.callback(key.toInt()) }
    }

    fun keysToHashSet() = IntHashSet(content.keysToHashSet())

    // definitely not ideal...
    val values: Iterable<V>
        get() {
            val values = ArrayList<V>(size)
            forEach { _, v -> values.add(v) }
            return values
        }
}