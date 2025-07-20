package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.ObjectIntCallback

/**
 * Wrapper around LongToLongHashMap
 * */
class ObjectToIntHashMap<K>(
    missingValue: Int,
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : PrimitiveCollection {

    @InternalAPI
    val content = ObjectToLongHashMap<K>(missingValue.toLong(), minCapacity, loadFactor)

    override val size get() = content.size
    override val maxFill get() = content.maxFill

    @Suppress("unused")
    val missingValue: Int
        get() = content.missingValue.toInt()

    operator fun set(key: K, value: Int) {
        put(key, value)
    }

    inline fun getOrPut(key: K, generateIfNull: () -> Int): Int {
        return content.getOrPut(key) { generateIfNull().toLong() }.toInt()
    }

    fun getOrPut(key: K, valueIfNull: Int): Int {
        return content.getOrPut(key, valueIfNull.toLong()).toInt()
    }

    fun put(key: K, value: Int): Int {
        return content.put(key, value.toLong()).toInt()
    }

    fun remove(key: K): Int {
        return content.remove(key).toInt()
    }

    fun remove(key: K, value: Int): Boolean {
        return content.remove(key, value.toLong())
    }

    fun containsKey(key: K): Boolean {
        return content.containsKey(key)
    }

    operator fun get(key: K): Int {
        return content[key].toInt()
    }

    fun replace(key: K, oldValue: Int, newValue: Int): Boolean {
        return content.replace(key, oldValue.toLong(), newValue.toLong())
    }

    fun replace(key: K, value: Int): Int {
        return content.replace(key, value.toLong()).toInt()
    }

    override fun clear() {
        content.clear()
    }

    fun trim(size: Int): Boolean {
        return content.trim(size)
    }

    @Suppress("unused")
    fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    fun forEach(callback: ObjectIntCallback<K>) {
        content.forEach { k, v ->
            callback.callback(k, v.toInt())
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
    fun forEachKey(callback: (K) -> Unit) = content.forEachKey(callback)
}