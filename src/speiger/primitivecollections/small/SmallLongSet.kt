package speiger.primitivecollections.small

import me.anno.utils.InternalAPI
import speiger.primitivecollections.callbacks.LongCallback
import speiger.primitivecollections.callbacks.LongPredicate

/**
 * A Set<Long>, that is just a linear array, and by that optimized for small sizes (up to 10-30).
 * */
class SmallLongSet(
    var size: Int = 0,
    var keys: LongArray = LongArray(16)
) {

    @InternalAPI
    fun findSlot(key: Long): Int {
        val keys = keys
        for (i in 0 until size) {
            if (key == keys[i]) return i
        }
        return size
    }

    operator fun contains(key: Long): Boolean {
        val slot = findSlot(key)
        return slot < size
    }

    fun add(key: Long): Boolean {
        return internalAdd(key, findSlot(key))
    }

    @InternalAPI
    fun internalAdd(key: Long, slot: Int): Boolean {
        if (slot < size) return false
        if (slot >= keys.size) {
            val newSize = keys.size * 2
            keys = keys.copyOf(newSize)
        }
        keys[slot] = key
        size = slot + 1
        return true
    }

    fun containsKey(key: Long): Boolean = findSlot(key) < size

    fun remove(key: Long): Boolean {
        val slot = findSlot(key)
        if (slot >= size) return false

        val lastIndex = size - 1
        keys[slot] = keys[lastIndex]
        size = lastIndex
        return true
    }

    fun removeIf(predicate: LongPredicate): Int {
        var writeIndex = 0
        val keys = keys
        val oldSize = size
        repeat(oldSize) { readIndex ->
            val key = keys[readIndex]
            if (!predicate.test(key)) {
                keys[writeIndex] = key
                writeIndex++
            }
        }
        size = writeIndex
        return oldSize - writeIndex
    }

    fun forEach(callback: LongCallback) {
        val keys = keys
        repeat(size) { readIndex ->
            val key = keys[readIndex]
            callback.call(key)
        }
    }

    fun clone() = SmallLongSet(size, keys.copyOf())
    fun clear() {
        size = 0
    }
}