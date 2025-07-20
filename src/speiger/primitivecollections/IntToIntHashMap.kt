package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongIntCallback

/**
 * Wrapper around LongToLongHashMap
 * */
@Suppress("unused")
class IntToIntHashMap(
    missingValue: Int,
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : PrimitiveCollection {

    @InternalAPI
    val content = LongToLongHashMap(missingValue.toLong(), minCapacity, loadFactor)

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

    fun remove(key: Int, value: Int): Boolean {
        return content.remove(key.toLong(), value.toLong())
    }

    fun containsKey(key: Int): Boolean {
        return content.containsKey(key.toLong())
    }

    operator fun get(key: Int): Int {
        return content[key.toLong()].toInt()
    }

    fun replace(key: Int, oldValue: Int, newValue: Int): Boolean {
        return content.replace(key.toLong(), oldValue.toLong(), newValue.toLong())
    }

    fun replace(key: Int, value: Int): Int {
        return content.replace(key.toLong(), value.toLong()).toInt()
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

    fun forEach(callback: LongIntCallback) {
        content.forEach { k, v ->
            callback.callback(k, v.toInt())
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
}