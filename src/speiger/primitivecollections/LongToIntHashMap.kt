package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.callbacks.LongIntCallback

/**
 * Wrapper around LongToLongHashMap
 * */
class LongToIntHashMap(
    missingValue: Int,
    minCapacity: Int = 16,
    loadFactor: Float = 0.75f
): PrimitiveCollection {

    @InternalAPI
    val content = LongToLongHashMap(missingValue.toLong(), minCapacity, loadFactor)

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

    fun remove(key: Long, value: Int): Boolean {
        return content.remove(key, value.toLong())
    }

    fun containsKey(key: Long): Boolean {
        return content.containsKey(key)
    }

    operator fun get(key: Long): Int {
        return content.remove(key).toInt()
    }

    fun replace(key: Long, oldValue: Int, newValue: Int): Boolean {
        return content.replace(key, oldValue.toLong(), newValue.toLong())
    }

    fun replace(key: Long, value: Int): Int {
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

    fun forEach(callback: LongIntCallback) {
        content.forEach { k, v ->
            callback.callback(k, v.toInt())
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
}