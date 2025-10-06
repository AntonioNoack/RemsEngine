package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.ObjectIntCallback
import speiger.primitivecollections.callbacks.ObjectIntPredicate

/**
 * Wrapper around LongToLongHashMap
 * */
class ObjectToIntHashMap<K>(
    @InternalAPI
    val content: ObjectToLongHashMap<K>
) : PrimitiveCollection {

    constructor(
        missingValue: Int,
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : this(ObjectToLongHashMap<K>(missingValue.toLong(), minCapacity, loadFactor))

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

    fun containsKey(key: K): Boolean {
        return content.containsKey(key)
    }

    operator fun get(key: K): Int {
        return content[key].toInt()
    }

    override fun clear() {
        content.clear()
    }

    override fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    fun forEach(callback: ObjectIntCallback<K>) {
        content.forEach { k, v ->
            callback.call(k, v.toInt())
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
    fun forEachKey(callback: (K) -> Unit) = content.forEachKey(callback)
    fun removeIf(predicate: ObjectIntPredicate<K>): Int =
        content.removeIf { key, value -> predicate.test(key, value.toInt()) }

    override fun clone(): ObjectToIntHashMap<K> = ObjectToIntHashMap<K>(content.clone())
}