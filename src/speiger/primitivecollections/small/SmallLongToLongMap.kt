package speiger.primitivecollections.small

import me.anno.utils.InternalAPI
import speiger.primitivecollections.callbacks.LongCallback
import speiger.primitivecollections.callbacks.LongLongCallback
import speiger.primitivecollections.callbacks.LongLongPredicate

// todo clean up constructors, and implement related methods, too
/**
 * A Map<Long,Long>, that is just two linear arrays, and by that optimized for small sizes (up to 10-30).
 *
 * todo should we change our logic in LongToLongHashMap for small sizes, or should we just keep this a separate structure? :)
 *  depends on how much complexity it introduces...
 * */
class SmallLongToLongMap(
    val missingValue: Long,
    initialCapacity: Int = 16,
    base: SmallLongToLongMap? = null
) {

    var size: Int = base?.size ?: 0
    var keys: LongArray = base?.keys?.copyOf() ?: LongArray(initialCapacity)
    var values: LongArray = base?.values?.copyOf() ?: LongArray(initialCapacity)

    @InternalAPI
    fun findSlot(key: Long): Int {
        val keys = keys
        for (i in 0 until size) {
            if (key == keys[i]) return i
        }
        return size
    }

    operator fun get(key: Long): Long {
        val slot = findSlot(key)
        return if (slot < size) values[slot] else missingValue
    }

    operator fun set(key: Long, value: Long): Long {
        return internalSet(key, value, findSlot(key))
    }

    @InternalAPI
    fun internalSet(key: Long, value: Long, slot: Int): Long {
        if (slot >= keys.size) {
            val newSize = keys.size * 2
            keys = keys.copyOf(newSize)
            values = values.copyOf(newSize)
        }
        val prevValue = values[slot]
        keys[slot] = key
        values[slot] = value
        if (slot < size) {
            return missingValue
        } else {
            size = slot + 1
            return prevValue
        }
    }

    fun containsKey(key: Long): Boolean = findSlot(key) < size

    fun remove(key: Long): Long {
        val slot = findSlot(key)
        if (slot >= size) return missingValue

        val lastIndex = size - 1
        val lastValue = values[slot]
        keys[slot] = keys[lastIndex]
        values[slot] = values[lastIndex]
        size = lastIndex
        return lastValue
    }

    fun removeIf(predicate: LongLongPredicate): Int {
        var writeIndex = 0
        val keys = keys
        val values = values
        val oldSize = size
        for (readIndex in 0 until oldSize) {
            val key = keys[readIndex]
            val value = values[readIndex]
            if (!predicate.test(key, value)) {
                keys[writeIndex] = key
                values[writeIndex] = value
                writeIndex++
            }
        }
        size = writeIndex
        return oldSize - writeIndex
    }

    fun forEach(callback: LongLongCallback) {
        val keys = keys
        val values = values
        for (readIndex in 0 until size) {
            val key = keys[readIndex]
            val value = values[readIndex]
            callback.call(key, value)
        }
    }

    fun forEachKey(callback: LongCallback) {
        val keys = keys
        for (readIndex in 0 until size) {
            val key = keys[readIndex]
            callback.call(key)
        }
    }

    inline fun getOrPut(key: Long, generateIfMissing: () -> Long): Long {
        val slot = findSlot(key)
        return if (slot >= size) {
            val value = generateIfMissing()
            internalSet(key, value, findSlot(key)) // slot can be invalidated during generateIfMissing()!
            value
        } else {
            values[slot]
        }
    }

    fun clone() = SmallLongToLongMap(missingValue, 16, this)
    fun clear() {
        size = 0
    }

    fun keysToHashSet(): SmallLongSet {
        return SmallLongSet(size, keys.copyOf())
    }
}