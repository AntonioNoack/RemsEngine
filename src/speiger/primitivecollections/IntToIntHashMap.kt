package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.IntCallback
import speiger.primitivecollections.callbacks.IntIntCallback
import speiger.primitivecollections.callbacks.IntIntPredicate

/**
 * Wrapper around LongToLongHashMap
 * */
class IntToIntHashMap(
    @property:InternalAPI
    val content: LongToLongHashMap
) : PrimitiveCollection {

    constructor(missingValue: Int, minCapacity: Int = DEFAULT_MIN_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR) :
            this(LongToLongHashMap(missingValue.toLong(), minCapacity, loadFactor))

    override val size get() = content.size
    override val maxFill get() = content.maxFill

    @Suppress("unused")
    val missingValue: Int
        get() = content.missingValue.toInt()

    operator fun set(key: Int, value: Int) {
        put(key, value)
    }

    inline fun getOrPut(key: Int, generateIfNull: () -> Int): Int {
        return content.getOrPut(key.toLong()) { generateIfNull().toLong() }.toInt()
    }

    fun getOrPut(key: Int, valueIfNull: Int): Int {
        return content.getOrPut(key.toLong(), valueIfNull.toLong()).toInt()
    }

    fun put(key: Int, value: Int): Int {
        return content.put(key.toLong(), value.toLong()).toInt()
    }

    fun remove(key: Int): Int {
        return content.remove(key.toLong()).toInt()
    }

    fun containsKey(key: Int): Boolean {
        return content.containsKey(key.toLong())
    }

    operator fun get(key: Int): Int {
        return content[key.toLong()].toInt()
    }

    override fun clear() {
        content.clear()
    }

    override fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    fun forEach(callback: IntIntCallback) {
        content.forEach { k, v ->
            callback.call(k.toInt(), v.toInt())
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
    fun forEachKey(callback: IntCallback) {
        content.forEachKey { key -> callback.call(key.toInt()) }
    }

    fun removeIf(predicate: IntIntPredicate): Int =
        content.removeIf { key, value -> predicate.test(key.toInt(), value.toInt()) }

    override fun clone(): IntToIntHashMap = IntToIntHashMap(content.clone())
}