package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.callbacks.LongObjectCallback

/**
 * Long2ObjectOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs. Parts have been moved to LongToHashMap.
 *
 * This improves our smooth normal calculation from 54ms for 110k triangles down to 32ms (1.68x speedup).
 * */
class LongToObjectHashMap<V>(minCapacity: Int = 16, loadFactor: Float = 0.75f) :
    LongToHashMap<Array<V?>>(minCapacity, loadFactor) {

    override fun createArray(size: Int): Array<V?> {
        @Suppress("UNCHECKED_CAST")
        return arrayOfNulls<Any>(size) as Array<V?>
    }

    override fun fillNulls(values: Array<V?>) {
        values.fill(null)
    }

    override fun copyOver(
        dstValues: Array<V?>, dstIndex: Int,
        srcValues: Array<V?>, srcIndex: Int
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun setNull(dstValues: Array<V?>, dstIndex: Int) {
        dstValues[dstIndex] = null
    }

    operator fun set(key: Long, value: V) {
        put(key, value)
    }

    inline fun getOrPut(key: Long, generateIfNull: () -> V): V {
        val slot = findIndex(key)
        @Suppress("UNCHECKED_CAST")
        if (slot >= 0) return values[slot] as V

        val newValue = generateIfNull()
        // don't remember slot, because map might have changed in-between
        put(key, newValue)
        return newValue
    }

    fun put(key: Long, value: V): V? {
        val slot = findIndex(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return null
        } else {
            val oldValue = values[slot]
            values[slot] = value
            return oldValue
        }
    }

    fun remove(key: Long): V? {
        val slot = findIndex(key)
        return if (slot < 0) null else removeIndex(slot)
    }

    fun remove(key: Long, value: V?): Boolean {
        if (key == 0L) {
            if (containsNull && value == values[nullIndex]) {
                removeNullIndex()
                return true
            } else {
                return false
            }
        } else {
            var pos = HashUtil.mix(key.hashCode()) and mask
            var current = keys[pos]
            if (current == 0L) {
                return false
            } else if (current == key && value == values[pos]) {
                removeIndex(pos)
                return true
            } else {
                do {
                    pos = (pos + 1) and mask
                    current = keys[pos]
                    if (current == 0L) {
                        return false
                    }
                } while (current != key || value != values[pos])

                removeIndex(pos)
                return true
            }
        }
    }

    operator fun get(key: Long): V? {
        val slot = findIndex(key)
        return if (slot < 0) null else values[slot]
    }

    fun replace(key: Long, oldValue: V, newValue: V): Boolean {
        val index = findIndex(key)
        if (index >= 0 && values[index] == oldValue) {
            values[index] = newValue
            return true
        } else {
            return false
        }
    }

    fun replace(key: Long, value: V): V? {
        val index = findIndex(key)
        if (index < 0) {
            return null
        } else {
            val oldValue = values[index]
            values[index] = value
            return oldValue
        }
    }

    private fun removeIndex(pos: Int): V? {
        if (pos == nullIndex) {
            return if (containsNull) removeNullIndex() else null
        } else {
            val value = values[pos]
            keys[pos] = 0L
            values[pos] = null
            --size
            shiftKeys(pos)
            shrinkMaybe()
            return value
        }
    }

    private fun removeNullIndex(): V? {
        val value = values[nullIndex]
        containsNull = false
        keys[nullIndex] = 0L
        values[nullIndex] = null
        --size
        shrinkMaybe()
        return value
    }

    @InternalAPI
    fun insert(slot: Int, key: Long, value: V) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun removeIf(predicate: (Long, V) -> Boolean): Int {
        val oldSize = size
        keysToHashSet().forEach { key ->
            val slot = findIndex(key)
            @Suppress("UNCHECKED_CAST")
            if (slot >= 0 && predicate(key, values[slot] as V)) {
                removeIndex(slot)
            }
        }
        return oldSize - size
    }

    fun forEach(callback: LongObjectCallback<V>) {
        @Suppress("UNCHECKED_CAST")
        if (containsNull) callback.callback(0L, values[nullIndex] as V)

        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != 0L) callback.callback(key, values[i] as V)
        }
    }
}