package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongCallback
import speiger.primitivecollections.callbacks.LongDoubleCallback
import speiger.primitivecollections.callbacks.LongDoublePredicate

/**
 * Wrapper around LongToLongHashMap
 * */
class LongToDoubleHashMap(
    @property:InternalAPI
    val content: LongToLongHashMap
) : PrimitiveCollection {

    constructor(
        missingValue: Double,
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : this(LongToLongHashMap(missingValue.toRawBits(), minCapacity, loadFactor))

    override val size get() = content.size
    override val maxFill get() = content.maxFill

    @Suppress("unused")
    val missingValue: Double
        get() = Double.fromBits(content.missingValue)

    operator fun set(key: Long, value: Double) {
        put(key, value)
    }

    inline fun getOrPut(key: Long, generateIfNull: () -> Double): Double {
        return Double.fromBits(content.getOrPut(key) { generateIfNull().toRawBits() })
    }

    fun put(key: Long, value: Double): Double {
        return Double.fromBits(content.put(key, value.toRawBits()))
    }

    fun remove(key: Long): Double {
        return Double.fromBits(content.remove(key))
    }

    fun containsKey(key: Long): Boolean {
        return content.containsKey(key)
    }

    operator fun get(key: Long): Double {
        return Double.fromBits(content[key])
    }

    override fun clear() {
        content.clear()
    }

    override fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    fun forEach(callback: LongDoubleCallback) {
        content.forEach { k, v ->
            callback.call(k, Double.fromBits(v))
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
    fun forEachKey(callback: LongCallback) = content.forEachKey(callback)
    fun removeIf(predicate: LongDoublePredicate): Int =
        content.removeIf { key, value -> predicate.test(key, Double.fromBits(value)) }

    override fun clone(): LongToDoubleHashMap = LongToDoubleHashMap(content.clone())
}