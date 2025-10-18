package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongCallback
import speiger.primitivecollections.callbacks.LongIntCallback
import speiger.primitivecollections.callbacks.LongIntPredicate

/**
 * Wrapper around LongToLongHashMap
 * */
class LongToIntHashMap(
    @property:InternalAPI
    val content: LongToLongHashMap
) : PrimitiveCollection {

    constructor(
        missingValue: Int,
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : this(LongToLongHashMap(missingValue.toLong(), minCapacity, loadFactor))

    override val size get() = content.size
    override val maxFill get() = content.maxFill

    @Suppress("unused")
    val missingValue: Int
        get() = content.missingValue.toInt()

    operator fun set(key: Long, value: Int) {
        put(key, value)
    }

    inline fun getOrPut(key: Long, generateIfNull: () -> Int): Int {
        return content.getOrPut(key) { generateIfNull().toLong() }.toInt()
    }

    fun put(key: Long, value: Int): Int {
        return content.put(key, value.toLong()).toInt()
    }

    fun remove(key: Long): Int {
        return content.remove(key).toInt()
    }

    fun containsKey(key: Long): Boolean {
        return content.containsKey(key)
    }

    operator fun get(key: Long): Int {
        return content[key].toInt()
    }

    override fun clear() {
        content.clear()
    }

    override fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    fun forEach(callback: LongIntCallback) {
        content.forEach { k, v ->
            callback.call(k, v.toInt())
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
    fun forEachKey(callback: LongCallback) = content.forEachKey(callback)
    fun removeIf(predicate: LongIntPredicate): Int =
        content.removeIf { key, value -> predicate.test(key, value.toInt()) }

    override fun clone(): LongToIntHashMap = LongToIntHashMap(content.clone())
}