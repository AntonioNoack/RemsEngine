package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.ObjectLongCallback

/**
 * Adjusted from LongToLongHashMap
 * */
class ObjectToLongHashMap<K>(
    val missingValue: Long,
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : ObjectToHashMap<K, LongArray>(minCapacity, loadFactor) {

    override fun createArray(size: Int): LongArray = LongArray(size)
    override fun fillNulls(values: LongArray) {
        values.fill(0L)
    }

    override fun copyOver(
        dstValues: LongArray, dstIndex: Int,
        srcValues: LongArray, srcIndex: Int
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun setNull(dstValues: LongArray, dstIndex: Int) {
        dstValues[dstIndex] = missingValue
    }

    operator fun set(key: K, value: Long) {
        put(key, value)
    }

    inline fun getOrPut(key: K, generateIfNull: () -> Long): Long {
        val slot = findIndex(key)
        if (slot >= 0) return values[slot]

        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun getOrPut(key: K, valueIfNull: Long): Long {
        val slot = findIndex(key)
        if (slot >= 0) return values[slot]

        insert(-slot - 1, key, valueIfNull)
        return valueIfNull
    }

    fun put(key: K, value: Long): Long {
        val slot = findIndex(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return missingValue
        } else {
            val oldValue = values[slot]
            values[slot] = value
            return oldValue
        }
    }

    fun remove(key: K): Long {
        val slot = findIndex(key)
        return if (slot < 0) missingValue else removeIndex(slot)
    }

    fun remove(key: K, value: Long): Boolean {
        if (key == null) {
            if (containsNull && value == values[nullIndex]) {
                removeNullIndex()
                return true
            } else {
                return false
            }
        } else {
            val hashCode = key.hashCode()
            var pos = HashUtil.mix(hashCode) and mask
            var current = keys[pos]
            if (current == null) {
                return false
            } else if (current == key && value == values[pos]) {
                removeIndex(pos)
                return true
            } else {
                do {
                    pos = (pos + 1) and mask
                    current = keys[pos]
                    if (current == null) {
                        return false
                    }
                } while (current != key || value != values[pos])

                removeIndex(pos)
                return true
            }
        }
    }

    operator fun get(key: K): Long {
        val slot = findIndex(key)
        return if (slot < 0) missingValue else values[slot]
    }

    fun replace(key: K, oldValue: Long, newValue: Long): Boolean {
        val index = findIndex(key)
        if (index >= 0 && values[index] == oldValue) {
            values[index] = newValue
            return true
        } else return false
    }

    fun replace(key: K, value: Long): Long {
        val index = findIndex(key)
        if (index < 0) {
            return missingValue
        } else {
            val oldValue = values[index]
            values[index] = value
            return oldValue
        }
    }

    private fun removeIndex(pos: Int): Long {
        if (pos == nullIndex) {
            return if (containsNull) removeNullIndex() else missingValue
        } else {
            val value = values[pos]
            keys[pos] = null
            values[pos] = missingValue
            --size
            shiftKeys(pos)
            shrinkMaybe()
            return value
        }
    }

    private fun removeNullIndex(): Long {
        val value = values[nullIndex]
        containsNull = false
        keys[nullIndex] = null
        values[nullIndex] = missingValue
        --size
        shrinkMaybe()
        return value
    }

    @InternalAPI
    fun insert(slot: Int, key: K, value: Long) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun forEach(callback: ObjectLongCallback<K>) {
        @Suppress("UNCHECKED_CAST")
        if (containsNull) callback.callback(null as K, values[nullIndex])
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != null) callback.callback(key as K, values[i])
        }
    }
}