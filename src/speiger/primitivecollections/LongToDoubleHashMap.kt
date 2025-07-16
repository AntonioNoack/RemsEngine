package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.callbacks.LongDoubleCallback

/**
 * Wrapper around LongToLongHashMap
 * */
class LongToDoubleHashMap(
    missingValue: Double,
    minCapacity: Int = 16,
    loadFactor: Float = 0.75f
) : PrimitiveCollection {

    @InternalAPI
    val content = LongToLongHashMap(missingValue.toRawBits(), minCapacity, loadFactor)

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

    fun remove(key: Long, value: Double): Boolean {
        return if (this[key] == value) {
            content.remove(key)
            true
        } else false
    }

    fun containsKey(key: Long): Boolean {
        return content.containsKey(key)
    }

    operator fun get(key: Long): Double {
        return Double.fromBits(content.remove(key))
    }

    fun replace(key: Long, oldValue: Double, newValue: Double): Boolean {
        return content.replace(key, oldValue.toRawBits(), newValue.toRawBits())
    }

    fun replace(key: Long, value: Double): Double {
        return Double.fromBits(content.replace(key, value.toRawBits()))
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

    fun forEach(callback: LongDoubleCallback) {
        content.forEach { k, v ->
            callback.callback(k, Double.fromBits(v))
        }
    }

    fun keysToHashSet() = content.keysToHashSet()
}